package br.com.overall.pelada.dto;

import jakarta.validation.constraints.NotBlank;

/** Confirmação para apagar uma pelada de vez: exige a senha do usuário logado. */
public record ExclusaoPeladaRequest(
        @NotBlank(message = "Informe a senha para confirmar")
        String senha
) {}
