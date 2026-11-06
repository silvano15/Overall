package br.com.overall.partida;

import br.com.overall.partida.dto.GoleiroRequest;
import br.com.overall.partida.dto.GolRequest;
import br.com.overall.partida.dto.IniciarPenaltisRequest;
import br.com.overall.partida.dto.PartidaResponse;
import br.com.overall.partida.dto.PenaltiRequest;
import br.com.overall.partida.dto.SubstituicaoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/partidas")
@Tag(name = "Partidas", description = "Partida ao vivo: cronômetro, gols, substituições e pênaltis")
public class PartidaController {

    private final PartidaService partidaService;

    public PartidaController(PartidaService partidaService) {
        this.partidaService = partidaService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Estado atual da partida (encerra automaticamente se o tempo acabou)")
    public PartidaResponse obter(@PathVariable Long id) { return partidaService.obter(id); }

    @DeleteMapping("/{id}")
    @Operation(summary = "Descarta uma partida que ainda não começou")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        partidaService.cancelar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // --- cronômetro
    @PostMapping("/{id}/iniciar")
    public PartidaResponse iniciar(@PathVariable Long id) { return partidaService.iniciar(id); }

    @PostMapping("/{id}/pausar")
    public PartidaResponse pausar(@PathVariable Long id) { return partidaService.pausar(id); }

    @PostMapping("/{id}/retomar")
    public PartidaResponse retomar(@PathVariable Long id) { return partidaService.retomar(id); }

    @PostMapping("/{id}/reiniciar")
    @Operation(summary = "Zera cronômetro e placar (a partida volta a aguardar o início)")
    public PartidaResponse reiniciar(@PathVariable Long id) { return partidaService.reiniciar(id); }

    @PostMapping("/{id}/encerrar")
    @Operation(summary = "Encerra a partida agora, valendo o placar atual")
    public PartidaResponse encerrar(@PathVariable Long id) { return partidaService.encerrar(id); }

    // --- gols
    @PostMapping("/{id}/gols")
    @Operation(summary = "Registra um gol (autor e assistente opcional)")
    public PartidaResponse gol(@PathVariable Long id, @Valid @RequestBody GolRequest req) {
        return partidaService.registrarGol(id, req);
    }

    @DeleteMapping("/{id}/gols/{golId}")
    @Operation(summary = "Desfaz um gol lançado por engano")
    public PartidaResponse desfazerGol(@PathVariable Long id, @PathVariable Long golId) {
        return partidaService.desfazerGol(id, golId);
    }

    // --- escalação
    @PostMapping("/{id}/substituicoes")
    @Operation(summary = "Substitui um jogador (ou coloca um reforço em time incompleto)")
    public PartidaResponse substituir(@PathVariable Long id, @Valid @RequestBody SubstituicaoRequest req) {
        return partidaService.substituir(id, req);
    }

    @PutMapping("/{id}/goleiro")
    @Operation(summary = "Define quem está no gol em um dos times")
    public PartidaResponse goleiro(@PathVariable Long id, @Valid @RequestBody GoleiroRequest req) {
        return partidaService.definirGoleiro(id, req);
    }

    // --- empate e pênaltis
    @PostMapping("/{id}/empate/encerrar")
    @Operation(summary = "Encerra a partida empatada (os dois times saem)")
    public PartidaResponse encerrarEmpate(@PathVariable Long id) { return partidaService.encerrarEmpate(id); }

    @PostMapping("/{id}/penaltis/iniciar")
    @Operation(summary = "Abre a disputa de pênaltis: NORMAL (todos batem), PREDETERMINADO (quantidade) ou ALTERNADO")
    public PartidaResponse iniciarPenaltis(@PathVariable Long id, @Valid @RequestBody IniciarPenaltisRequest req) {
        return partidaService.iniciarPenaltis(id, req);
    }

    @PostMapping("/{id}/penaltis")
    @Operation(summary = "Registra uma cobrança (as cobranças são alternadas entre os times)")
    public PartidaResponse penalti(@PathVariable Long id, @Valid @RequestBody PenaltiRequest req) {
        return partidaService.registrarPenalti(id, req);
    }

    @DeleteMapping("/{id}/penaltis/ultimo")
    public PartidaResponse desfazerPenalti(@PathVariable Long id) { return partidaService.desfazerPenalti(id); }

    @PostMapping("/{id}/penaltis/encerrar")
    @Operation(summary = "Confirma o vencedor dos pênaltis e fecha a partida")
    public PartidaResponse encerrarPenaltis(@PathVariable Long id) { return partidaService.encerrarPenaltis(id); }
}
