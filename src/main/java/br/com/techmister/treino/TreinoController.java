package br.com.techmister.treino;

import br.com.techmister.treino.dto.TreinoRequest;
import br.com.techmister.treino.dto.TreinoResponse;
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
@RequestMapping("/treinos")
@Tag(name = "Treinos", description = "Estatísticas de treinos (RFs 006 a 009)")
public class TreinoController {

    private final TreinoService treinoService;

    @Autowired
    public TreinoController(TreinoService treinoService) { this.treinoService = treinoService; }

    @PostMapping
    @Operation(summary = "Cadastra um treino (RF-006)")
    public ResponseEntity<TreinoResponse> cadastrar(@Valid @RequestBody TreinoRequest req, UriComponentsBuilder ub) {
        TreinoResponse criado = treinoService.cadastrar(req);
        URI loc = ub.path("/treinos/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(loc).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera um treino (RF-007)")
    public ResponseEntity<TreinoResponse> alterar(@PathVariable Long id, @Valid @RequestBody TreinoRequest req) {
        return ResponseEntity.ok(treinoService.alterar(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um treino (RF-008)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        treinoService.remover(id); return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    @Operation(summary = "Lista treinos da semana (RF-009)")
    public ResponseEntity<List<TreinoResponse>> listar() { return ResponseEntity.ok(treinoService.listarDaSemanaAtual()); }

    @GetMapping("/todos")
    @Operation(summary = "Lista todos os treinos")
    public ResponseEntity<List<TreinoResponse>> listarTodos() { return ResponseEntity.ok(treinoService.listarTodos()); }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um treino")
    public ResponseEntity<TreinoResponse> buscar(@PathVariable Long id) { return ResponseEntity.ok(treinoService.buscar(id)); }
}
