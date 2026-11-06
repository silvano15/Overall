package br.com.overall.auth;

import br.com.overall.exception.RegraDeNegocioException;
import br.com.overall.security.JwtService;
import br.com.overall.usuario.Usuario;
import br.com.overall.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(AuthenticationManager authenticationManager,
                       UsuarioRepository usuarioRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse autenticar(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.senha()));
        Usuario usuario = usuarioRepository.findByUsername(req.username()).orElseThrow();
        UserDetails userDetails = User.builder()
                .username(usuario.getUsername())
                .password(usuario.getSenha())
                .authorities(Arrays.asList(() -> "ROLE_" + usuario.getPerfil().name()))
                .build();
        String token = jwtService.gerarToken(userDetails);
        return new LoginResponse(token, "Bearer", usuario.getUsername(), usuario.getPerfil().name());
    }

    @Transactional
    public void alterarSenha(String username, AlterarSenhaRequest req) {
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        if (!passwordEncoder.matches(req.senhaAtual(), usuario.getSenha()))
            throw new RegraDeNegocioException("A senha atual está incorreta.");
        usuario.setSenha(passwordEncoder.encode(req.novaSenha()));
        usuarioRepository.save(usuario);
    }
}
