package br.com.techmister.jogador;

import br.com.techmister.jogador.dto.JogadorRequest;
import br.com.techmister.jogador.dto.JogadorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/jogadores")
@Tag(name = "Jogadores", description = "CRUD do elenco (RFs 002 a 005)")
public class JogadorController {

    private final JogadorService jogadorService;

    @Autowired
    public JogadorController(JogadorService jogadorService) {
        this.jogadorService = jogadorService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo jogador (RF-002)")
    public ResponseEntity<JogadorResponse> cadastrar(@Valid @RequestBody JogadorRequest request, UriComponentsBuilder uriBuilder) {
        JogadorResponse criado = jogadorService.cadastrar(request);
        URI location = uriBuilder.path("/jogadores/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera os dados de um jogador (RF-003)")
    public ResponseEntity<JogadorResponse> alterar(@PathVariable Long id, @Valid @RequestBody JogadorRequest request) {
        return ResponseEntity.ok(jogadorService.alterar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um jogador (RF-004)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        jogadorService.remover(id); return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    @Operation(summary = "Lista todos os jogadores (RF-005)")
    public ResponseEntity<List<JogadorResponse>> listar() { return ResponseEntity.ok(jogadorService.listar()); }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um jogador pelo id")
    public ResponseEntity<JogadorResponse> buscar(@PathVariable Long id) { return ResponseEntity.ok(jogadorService.buscar(id)); }
}
