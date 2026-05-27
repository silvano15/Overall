package br.com.techmister.timeideal;

import br.com.techmister.timeideal.dto.TimeIdealResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/time-ideal")
@Tag(name = "Time Ideal", description = "Escalação automática semanal (RF-022)")
public class TimeIdealController {

    private final TimeIdealService timeIdealService;

    @Autowired
    public TimeIdealController(TimeIdealService timeIdealService) { this.timeIdealService = timeIdealService; }

    @PostMapping("/gerar")
    @PreAuthorize("hasRole('TREINADOR')")
    @Operation(summary = "Gera o Time Ideal da semana (RF-022)")
    public ResponseEntity<TimeIdealResponse> gerar() {
        return ResponseEntity.ok(timeIdealService.gerarDaSemanaAtual());
    }
}
