package br.com.techmister.partida.dto;

import br.com.techmister.partida.Partida;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PartidaResponse(
        Long id,
        String adversario,
        LocalDate dataJogo,
        Integer golsPro,
        Integer golsContra,
        String local,
        LocalDateTime criadoEm
) {

    public static PartidaResponse de(Partida p) {
        return new PartidaResponse(
                p.getId(),
                p.getAdversario(),
                p.getDataJogo(),
                p.getGolsPro(),
                p.getGolsContra(),
                p.getLocal(),
                p.getCriadoEm()
        );
    }
}
