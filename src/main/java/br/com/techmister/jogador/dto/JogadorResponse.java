package br.com.techmister.jogador.dto;

import br.com.techmister.jogador.Jogador;
import br.com.techmister.jogador.Posicao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representação do jogador devolvida pela API.
 *
 * <p>Por que não retornar a entidade direto?
 * <ul>
 *   <li>Desacopla a API do modelo de banco — se a tabela muda, o contrato HTTP não muda automaticamente.</li>
 *   <li>Evita expor campos internos (ex.: senhas em outras entidades).</li>
 *   <li>Evita problemas de serialização de relacionamentos JPA preguiçosos.</li>
 * </ul>
 */
public record JogadorResponse(
        Long id,
        String nome,
        String documento,
        Posicao posicao,
        Integer alturaCm,
        BigDecimal pesoKg,
        LocalDate dataNascimento,
        Boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {

    /** Conversão Entidade → DTO. */
    public static JogadorResponse de(Jogador j) {
        return new JogadorResponse(
                j.getId(),
                j.getNome(),
                j.getDocumento(),
                j.getPosicao(),
                j.getAlturaCm(),
                j.getPesoKg(),
                j.getDataNascimento(),
                j.getAtivo(),
                j.getCriadoEm(),
                j.getAtualizadoEm()
        );
    }
}
