# Gera o comando SQL que cria o usuario "admin" com a senha que voce digitar.
# A senha fica so neste computador: o SQL contem apenas o hash BCrypt.
# Requer Java 17 e o Maven ja ter baixado as dependencias (rodar o projeto uma vez).

$sec = Read-Host "Digite a senha do admin (min. 6 caracteres)" -AsSecureString
$senha = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))
if ($senha.Length -lt 6) { Write-Host "Senha muito curta." -ForegroundColor Red; return }

$m2     = "$env:USERPROFILE\.m2\repository"
$crypto = (Get-ChildItem $m2 -Recurse -Filter 'spring-security-crypto-*.jar' | Select-Object -First 1).FullName
$jcl    = (Get-ChildItem $m2 -Recurse -Filter 'spring-jcl-*.jar' | Select-Object -First 1).FullName
if (-not $crypto -or -not $jcl) { Write-Host "Nao achei as dependencias no .m2. Rode o projeto uma vez antes." -ForegroundColor Red; return }

$java = (Get-Command java -ErrorAction SilentlyContinue).Source
if (-not $java) { $java = (Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Recurse -Filter java.exe | Select-Object -First 1).FullName }

$tmp = Join-Path $env:TEMP "HashSenha.java"
@'
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class HashSenha {
    public static void main(String[] a) { System.out.println(new BCryptPasswordEncoder().encode(a[0])); }
}
'@ | Set-Content -Encoding ascii $tmp

$hash = (& $java -cp "$crypto;$jcl" $tmp $senha | Select-Object -Last 1).Trim()
Remove-Item $tmp -Force

Write-Host "`nCole este comando no SQL Editor do TiDB (banco Overall) e execute:`n" -ForegroundColor Green
"INSERT INTO usuario (username, senha, nome_completo, perfil, ativo) VALUES ('admin', '$hash', 'Administrador da Pelada', 'ADMIN', TRUE) ON DUPLICATE KEY UPDATE senha = VALUES(senha), ativo = TRUE;"
