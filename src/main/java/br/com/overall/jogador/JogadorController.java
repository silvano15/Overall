package br.com.overall.jogador;

import br.com.overall.jogador.dto.JogadorRequest;
import br.com.overall.jogador.dto.JogadorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/jogadores")
@Tag(name = "Jogadores", description = "Cadastro dos jogadores da pelada (nome e foto)")
public class JogadorController {

    private final JogadorService jogadorService;

    public JogadorController(JogadorService jogadorService) {
        this.jogadorService = jogadorService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um jogador (nome e foto)")
    public ResponseEntity<JogadorResponse> cadastrar(@Valid @RequestBody JogadorRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        JogadorResponse criado = jogadorService.cadastrar(request);
        URI location = uriBuilder.path("/jogadores/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera nome, foto ou posição de goleiro")
    public ResponseEntity<JogadorResponse> alterar(@PathVariable Long id, @Valid @RequestBody JogadorRequest request) {
        return ResponseEntity.ok(jogadorService.alterar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove o jogador (ou arquiva, se já tiver histórico)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        jogadorService.remover(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{id}/reativar")
    @Operation(summary = "Reativa um jogador arquivado")
    public ResponseEntity<JogadorResponse> reativar(@PathVariable Long id) {
        return ResponseEntity.ok(jogadorService.reativar(id));
    }

    @GetMapping
    @Operation(summary = "Lista todos os jogadores com overall e estatísticas")
    public ResponseEntity<List<JogadorResponse>> listar() {
        return ResponseEntity.ok(jogadorService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um jogador pelo id")
    public ResponseEntity<JogadorResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(jogadorService.buscar(id));
    }
}
