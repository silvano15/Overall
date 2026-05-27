package br.com.techmister.partida;

import br.com.techmister.partida.dto.PartidaRequest;
import br.com.techmister.partida.dto.PartidaResponse;
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
@RequestMapping("/partidas")
@Tag(name = "Partidas", description = "Calendário de jogos (RFs 014 a 017)")
public class PartidaController {

    private final PartidaService partidaService;

    @Autowired
    public PartidaController(PartidaService partidaService) { this.partidaService = partidaService; }

    @PostMapping
    @Operation(summary = "Cadastra partida (RF-014)")
    public ResponseEntity<PartidaResponse> cadastrar(@Valid @RequestBody PartidaRequest req, UriComponentsBuilder ub) {
        PartidaResponse criada = partidaService.cadastrar(req);
        URI loc = ub.path("/partidas/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(loc).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera partida (RF-015)")
    public ResponseEntity<PartidaResponse> alterar(@PathVariable Long id, @Valid @RequestBody PartidaRequest req) {
        return ResponseEntity.ok(partidaService.alterar(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove partida (RF-016)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        partidaService.remover(id); return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    @Operation(summary = "Lista partidas (RF-017)")
    public ResponseEntity<List<PartidaResponse>> listar() { return ResponseEntity.ok(partidaService.listar()); }

    @GetMapping("/{id}")
    public ResponseEntity<PartidaResponse> buscar(@PathVariable Long id) { return ResponseEntity.ok(partidaService.buscar(id)); }
}
