package br.com.overall.pelada;

import br.com.overall.pelada.dto.PeladaListaItem;
import br.com.overall.pelada.dto.PeladaRequest;
import br.com.overall.pelada.dto.PeladaResponse;
import br.com.overall.pelada.dto.ResumoPeladaResponse;
import br.com.overall.pelada.dto.SorteioRequest;
import br.com.overall.partida.PartidaService;
import br.com.overall.partida.dto.PartidaRequest;
import br.com.overall.partida.dto.PartidaResponse;
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
@RequestMapping("/peladas")
@Tag(name = "Peladas", description = "Lista da semana, sorteio dos times e fechamento da pelada")
public class PeladaController {

    private final PeladaService peladaService;
    private final PartidaService partidaService;

    public PeladaController(PeladaService peladaService, PartidaService partidaService) {
        this.peladaService = peladaService;
        this.partidaService = partidaService;
    }

    @PostMapping
    @Operation(summary = "Cria a pelada (data e horário) e abre a lista da semana")
    public ResponseEntity<PeladaResponse> criar(@Valid @RequestBody PeladaRequest req, UriComponentsBuilder uri) {
        PeladaResponse criada = peladaService.criar(req);
        URI location = uri.path("/peladas/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera data/horário (antes da primeira partida)")
    public PeladaResponse atualizar(@PathVariable Long id, @Valid @RequestBody PeladaRequest req) {
        return peladaService.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma pelada sem partidas")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        peladaService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/atual")
    @Operation(summary = "A pelada da vez (a não finalizada mais próxima); 204 se não houver")
    public ResponseEntity<PeladaResponse> atual() {
        return peladaService.atual().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    @Operation(summary = "Histórico de peladas")
    public List<PeladaListaItem> historico() {
        return peladaService.historico();
    }

    @GetMapping("/{id}")
    public PeladaResponse obter(@PathVariable Long id) {
        return peladaService.obter(id);
    }

    @PutMapping("/{id}/lista/{jogadorId}")
    @Operation(summary = "Adiciona um jogador à lista da pelada")
    public PeladaResponse adicionarJogador(@PathVariable Long id, @PathVariable Long jogadorId) {
        return peladaService.adicionarJogador(id, jogadorId);
    }

    @DeleteMapping("/{id}/lista/{jogadorId}")
    @Operation(summary = "Tira um jogador da lista da pelada")
    public PeladaResponse removerJogador(@PathVariable Long id, @PathVariable Long jogadorId) {
        return peladaService.removerJogador(id, jogadorId);
    }

    @PostMapping("/{id}/sorteio")
    @Operation(summary = "Sorteia os times (aleatório no início; balanceado por overall com histórico)")
    public PeladaResponse sortear(@PathVariable Long id, @Valid @RequestBody SorteioRequest req) {
        return peladaService.sortear(id, req);
    }

    @PostMapping("/{id}/partidas")
    @Operation(summary = "Cria uma partida entre dois times, com duração e limite de gols")
    public ResponseEntity<PartidaResponse> criarPartida(@PathVariable Long id, @Valid @RequestBody PartidaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partidaService.criar(id, req));
    }

    @PostMapping("/{id}/finalizar")
    @Operation(summary = "Finaliza a pelada: notas do dia e atualização do overall")
    public ResumoPeladaResponse finalizar(@PathVariable Long id) {
        return peladaService.finalizar(id);
    }

    @GetMapping("/{id}/resumo")
    @Operation(summary = "Ranking de times, artilheiros, assistentes e notas da pelada")
    public ResumoPeladaResponse resumo(@PathVariable Long id) {
        return peladaService.resumo(id);
    }
}
