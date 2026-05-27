package br.com.techmister.jogador;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JogadorRepository extends JpaRepository<Jogador, Long> {

    /** Usado no cadastro para impedir documento duplicado (RF-002 CN02). */
    Optional<Jogador> findByDocumento(String documento);

    /** Usado na alteração para garantir que o documento não pertence a outro jogador (RF-003 CN02). */
    Optional<Jogador> findByDocumentoAndIdNot(String documento, Long id);
}
