package br.com.techmister.comportamento;

import br.com.techmister.comportamento.dto.ComportamentoRequest;
import br.com.techmister.comportamento.dto.ComportamentoResponse;
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
@RequestMapping("/comportamentos")
@Tag(name = "Comportamentos", description = "Avaliações comportamentais (RFs 010 a 013)")
public class ComportamentoController {

    private final ComportamentoService comportamentoService;

    @Autowired
    public ComportamentoController(ComportamentoService comportamentoService) { this.comportamentoService = comportamentoService; }

    @PostMapping
    @Operation(summary = "Cadastra um comportamento (RF-010)")
    public ResponseEntity<ComportamentoResponse> cadastrar(@Valid @RequestBody ComportamentoRequest req, UriComponentsBuilder ub) {
        ComportamentoResponse criado = comportamentoService.cadastrar(req);
        URI loc = ub.path("/comportamentos/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(loc).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera (RF-011)")
    public ResponseEntity<ComportamentoResponse> alterar(@PathVariable Long id, @Valid @RequestBody ComportamentoRequest req) {
        return ResponseEntity.ok(comportamentoService.alterar(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove (RF-012)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        comportamentoService.remover(id); return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    @Operation(summary = "Lista (RF-013)")
    public ResponseEntity<List<ComportamentoResponse>> listar() { return ResponseEntity.ok(comportamentoService.listar()); }

    @GetMapping("/{id}")
    public ResponseEntity<ComportamentoResponse> buscar(@PathVariable Long id) { return ResponseEntity.ok(comportamentoService.buscar(id)); }
}
