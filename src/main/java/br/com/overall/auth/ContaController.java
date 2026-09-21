package br.com.overall.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Conta do ADM. Fica fora de /auth porque exige estar autenticado. */
@RestController
@RequestMapping("/conta")
@Tag(name = "Conta", description = "Dados da conta do ADM logado")
public class ContaController {

    private final AuthService authService;

    public ContaController(AuthService authService) {
        this.authService = authService;
    }

    @PutMapping("/senha")
    @Operation(summary = "Altera a senha do ADM logado")
    public ResponseEntity<Void> alterarSenha(Authentication auth, @Valid @RequestBody AlterarSenhaRequest req) {
        authService.alterarSenha(auth.getName(), req);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
