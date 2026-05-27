package br.com.techmister.auth;

import br.com.techmister.security.JwtService;
import br.com.techmister.usuario.Usuario;
import br.com.techmister.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.util.Arrays;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    @Autowired
    public AuthService(AuthenticationManager authenticationManager,
                       UsuarioRepository usuarioRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
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
}
