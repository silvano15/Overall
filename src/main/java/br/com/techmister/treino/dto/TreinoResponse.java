package br.com.techmister.treino.dto;

import br.com.techmister.treino.Treino;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TreinoResponse(
        Long id,
        Long jogadorId,
        String jogadorNome,
        LocalDate dataTreino,
        Integer acertos,
        Integer erros,
        LocalDateTime criadoEm
) {

    public static TreinoResponse de(Treino t) {
        return new TreinoResponse(
                t.getId(),
                t.getJogador().getId(),
                t.getJogador().getNome(),
                t.getDataTreino(),
                t.getAcertos(),
                t.getErros(),
                t.getCriadoEm()
        );
    }
}
