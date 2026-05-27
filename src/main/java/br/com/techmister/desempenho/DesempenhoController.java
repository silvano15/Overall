package br.com.techmister.desempenho;

import br.com.techmister.desempenho.dto.DesempenhoRequest;
import br.com.techmister.desempenho.dto.DesempenhoResponse;
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
@RequestMapping("/desempenhos")
@Tag(name = "Desempenhos", description = "Scout dos jogadores (RFs 018 a 021)")
public class DesempenhoController {

    private final DesempenhoService desempenhoService;

    @Autowired
    public DesempenhoController(DesempenhoService desempenhoService) { this.desempenhoService = desempenhoService; }

    @PostMapping
    @Operation(summary = "Cadastra desempenho (RF-018)")
    public ResponseEntity<DesempenhoResponse> cadastrar(@Valid @RequestBody DesempenhoRequest req, UriComponentsBuilder ub) {
        DesempenhoResponse criado = desempenhoService.cadastrar(req);
        URI loc = ub.path("/desempenhos/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(loc).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera (RF-019)")
    public ResponseEntity<DesempenhoResponse> alterar(@PathVariable Long id, @Valid @RequestBody DesempenhoRequest req) {
        return ResponseEntity.ok(desempenhoService.alterar(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove (RF-020)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        desempenhoService.remover(id); return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    @Operation(summary = "Lista (RF-021)")
    public ResponseEntity<List<DesempenhoResponse>> listar() { return ResponseEntity.ok(desempenhoService.listar()); }

    @GetMapping("/{id}")
    public ResponseEntity<DesempenhoResponse> buscar(@PathVariable Long id) { return ResponseEntity.ok(desempenhoService.buscar(id)); }
}
