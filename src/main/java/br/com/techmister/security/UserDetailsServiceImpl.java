package br.com.techmister.security;

import br.com.techmister.usuario.Usuario;
import br.com.techmister.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Arrays;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Autowired
    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário ou senha inválidos"));
        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new UsernameNotFoundException("Usuário inativo");
        }
        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getSenha())
                .authorities(Arrays.asList(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())))
                .build();
    }
}
