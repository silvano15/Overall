package br.com.overall.ranking;

import br.com.overall.ranking.dto.ConexoesResponse;
import br.com.overall.ranking.dto.RankingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rankings")
@Tag(name = "Rankings", description = "Rankings mensal, anual e geral")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    @Operation(summary = "Vencedores, goleadores, assistentes, goleiros, notas e overalls de um período")
    public RankingResponse ranking(@RequestParam Long grupoId,
                                   @RequestParam(defaultValue = "MES") String periodo,
                                   @RequestParam(required = false) Integer ano,
                                   @RequestParam(required = false) Integer mes) {
        return rankingService.ranking(grupoId, RankingService.periodo(periodo), ano, mes);
    }

    @GetMapping("/conexoes")
    @Operation(summary = "Rede de assistências (assistente → autor) de um período")
    public ConexoesResponse conexoes(@RequestParam Long grupoId,
                                     @RequestParam(defaultValue = "MES") String periodo,
                                     @RequestParam(required = false) Integer ano,
                                     @RequestParam(required = false) Integer mes) {
        return rankingService.conexoes(grupoId, RankingService.periodo(periodo), ano, mes);
    }
}
