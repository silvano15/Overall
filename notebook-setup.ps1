# =====================================================================
#  OVERALL - preparar o NOTEBOOK (copie e cole no PowerShell, bloco por bloco)
#  Nao precisa de administrador, exceto no BLOCO 5 (firewall).
#  Tudo e baixado em zip para C:\overall-setup (JDK 17, Maven, MySQL 8.0).
#  Requer internet so nesta preparacao. Depois, o uso e 100% local.
# =====================================================================


# ---------------------------------------------------------------------
# BLOCO 0 - CONFIGURACAO (edite as 2 primeiras linhas e cole)
# ---------------------------------------------------------------------
# Pasta do projeto que voce copiou do PC para o notebook (a que contem o pom.xml):
$Projeto    = "$env:USERPROFILE\Downloads\Overall-main"
# Senha do login "admin" do sistema (voce pode trocar depois na tela Conta):
$SenhaAdmin = 'TroqueEstaSenha#2026'

$Base  = 'C:\overall-setup'
$Jdk   = "$Base\jdk"
$Mvn   = "$Base\maven"
$Mysql = "$Base\mysql"
$Dados = "$Base\mysql-data"
if (-not (Test-Path "$Projeto\pom.xml")) { Write-Host "ERRO: nao achei $Projeto\pom.xml - ajuste a variavel `$Projeto" -ForegroundColor Red }


# ---------------------------------------------------------------------
# BLOCO 1 - BAIXAR E EXTRAIR JDK 17, MAVEN E MYSQL (so na primeira vez)
# ---------------------------------------------------------------------
$ProgressPreference = 'SilentlyContinue'
New-Item -ItemType Directory -Force $Base | Out-Null

# JDK 17 (Temurin)
Invoke-WebRequest 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile "$Base\jdk.zip"
Expand-Archive "$Base\jdk.zip" -DestinationPath "$Base\_jdk" -Force
Move-Item (Get-ChildItem "$Base\_jdk" -Directory | Select-Object -First 1).FullName $Jdk -Force

# Maven 3.9.9
Invoke-WebRequest 'https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip' -OutFile "$Base\maven.zip"
Expand-Archive "$Base\maven.zip" -DestinationPath "$Base\_mvn" -Force
Move-Item (Get-ChildItem "$Base\_mvn" -Directory | Select-Object -First 1).FullName $Mvn -Force

# MySQL 8.0.36 (zip, sem instalador) - ~230 MB
Invoke-WebRequest 'https://cdn.mysql.com/archives/mysql-8.0/mysql-8.0.36-winx64.zip' -OutFile "$Base\mysql.zip"
Expand-Archive "$Base\mysql.zip" -DestinationPath "$Base\_mysql" -Force
Move-Item (Get-ChildItem "$Base\_mysql" -Directory | Select-Object -First 1).FullName $Mysql -Force

Remove-Item "$Base\jdk.zip","$Base\maven.zip","$Base\mysql.zip","$Base\_jdk","$Base\_mvn","$Base\_mysql" -Recurse -Force -ErrorAction SilentlyContinue
& "$Jdk\bin\java.exe" -version
& "$Mvn\bin\mvn.cmd" -v


# ---------------------------------------------------------------------
# BLOCO 2 - CRIAR O BANCO MYSQL LOCAL (so na primeira vez)
#   Usuario root sem senha, escutando apenas em 127.0.0.1 (so este notebook).
#   Se der erro de "VCRUNTIME140.dll", instale o "Microsoft Visual C++ Redistributable x64".
# ---------------------------------------------------------------------
& "$Mysql\bin\mysqld.exe" --initialize-insecure --console --datadir="$Dados"


# ---------------------------------------------------------------------
# BLOCO 3 - LIGAR O MYSQL + BAIXAR DEPENDENCIAS + LIGAR O SISTEMA
#   Este bloco tambem e o que voce cola TODA VEZ que for usar o sistema
#   (reexecute o BLOCO 0 antes se abrir um PowerShell novo).
# ---------------------------------------------------------------------
# 3a) MySQL (se ainda nao estiver rodando)
if (-not (Get-Process mysqld -ErrorAction SilentlyContinue)) {
    Start-Process "$Mysql\bin\mysqld.exe" -ArgumentList "--datadir=$Dados","--bind-address=127.0.0.1","--console" -WindowStyle Minimized
}
while (-not (Test-NetConnection 127.0.0.1 -Port 3306 -InformationLevel Quiet -WarningAction SilentlyContinue)) { Start-Sleep 2 }
Write-Host "MySQL no ar." -ForegroundColor Green

# 3b) Sistema em uma nova janela (fica aberta enquanto voce usa; feche para parar)
$cmd = "`$env:JAVA_HOME='$Jdk'; `$env:PATH='$Jdk\bin;$Mvn\bin;'+`$env:PATH; `$env:DB_SSL_MODE='DISABLED'; cd '$Projeto'; mvn spring-boot:run"
Start-Process powershell -ArgumentList '-NoExit','-Command',$cmd

# 3c) Espera o sistema subir (a 1a vez baixa dependencias: pode levar varios minutos)
while (-not (Test-NetConnection 127.0.0.1 -Port 8080 -InformationLevel Quiet -WarningAction SilentlyContinue)) { Start-Sleep 5 }
Write-Host "Sistema no ar." -ForegroundColor Green


# ---------------------------------------------------------------------
# BLOCO 4 - CRIAR O USUARIO ADMIN (so na primeira vez, com o sistema ja no ar)
#   O banco novo nasce sem admin. Este bloco gera o hash BCrypt da $SenhaAdmin
#   e cria o usuario "admin". Login no sistema: admin / <a senha do BLOCO 0>
# ---------------------------------------------------------------------
$m2     = "$env:USERPROFILE\.m2\repository"
$crypto = (Get-ChildItem $m2 -Recurse -Filter 'spring-security-crypto-*.jar' | Select-Object -First 1).FullName
$jcl    = (Get-ChildItem $m2 -Recurse -Filter 'spring-jcl-*.jar' | Select-Object -First 1).FullName

@'
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class Hash {
    public static void main(String[] a) { System.out.println(new BCryptPasswordEncoder().encode(a[0])); }
}
'@ | Set-Content -Encoding ascii "$Base\Hash.java"

$hash = (& "$Jdk\bin\java.exe" -cp "$crypto;$jcl" "$Base\Hash.java" $SenhaAdmin | Select-Object -Last 1).Trim()
$sql  = "INSERT INTO usuario (username, senha, nome_completo, perfil, ativo) VALUES ('admin','" + $hash + "','Administrador da Pelada','ADMIN',TRUE) ON DUPLICATE KEY UPDATE senha=VALUES(senha), ativo=TRUE;"
Set-Content -Encoding ascii "$Base\admin.sql" $sql
& "$Mysql\bin\mysql.exe" -u root overall -e "source $Base\admin.sql"
Remove-Item "$Base\admin.sql","$Base\Hash.java" -Force
Write-Host "Admin criado. Login: admin  /  senha definida em `$SenhaAdmin" -ForegroundColor Green


# ---------------------------------------------------------------------
# BLOCO 5 - LIBERAR O CELULAR NA REDE (PowerShell COMO ADMINISTRADOR, uma vez)
# ---------------------------------------------------------------------
New-NetFirewallRule -DisplayName "Overall App (8080)" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow -Profile Private,Domain


# ---------------------------------------------------------------------
# BLOCO 6 - DESCOBRIR O ENDERECO PARA ABRIR NO CELULAR
#   Celular e notebook no MESMO Wi-Fi. Abra no navegador do celular o
#   endereco mostrado abaixo. Se o Wi-Fi estiver como "Publica", mude para
#   "Privada" (Configuracoes > Rede > propriedades do Wi-Fi).
# ---------------------------------------------------------------------
Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object { $_.IPAddress -notmatch '^(127|169\.254)\.' -and $_.InterfaceAlias -notmatch 'VirtualBox|vEthernet|Loopback|Bluetooth' } |
    ForEach-Object { "http://$($_.IPAddress):8080/api/   ($($_.InterfaceAlias))" }


# ---------------------------------------------------------------------
# PARAR TUDO (quando terminar)
# ---------------------------------------------------------------------
# Feche a janela do sistema e depois:
# Stop-Process -Name mysqld -Force
