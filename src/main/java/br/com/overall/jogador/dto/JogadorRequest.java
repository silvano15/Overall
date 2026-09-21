package br.com.overall.jogador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cadastro/edição de jogador: apenas nome e foto.
 *
 * <p>'foto': null = mantém a atual (na edição); "" = remove; senão, data URL de imagem.
 * A miniatura é reduzida no navegador antes do envio (~256px), por isso o limite baixo.
 */
public record JogadorRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres")
        String nome,

        @Size(max = 300_000, message = "A foto é muito grande")
        @Pattern(regexp = "^$|^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+$",
                 message = "A foto deve ser uma imagem PNG, JPEG ou WEBP")
        String foto,

        Boolean goleiro
) {}
