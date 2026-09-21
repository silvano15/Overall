package br.com.overall.pelada.dto;

import br.com.overall.pelada.PeladaStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/** Linha do histórico de peladas. */
public record PeladaListaItem(
        Long id,
        LocalDate dataJogo,
        LocalTime horaJogo,
        PeladaStatus status,
        long jogadores,
        long partidas
) {}
