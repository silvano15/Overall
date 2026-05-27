package br.com.techmister.comportamento;

import br.com.techmister.comportamento.dto.ComportamentoRequest;
import br.com.techmister.comportamento.dto.ComportamentoResponse;
import br.com.techmister.exception.RecursoNaoEncontradoException;
import br.com.techmister.exception.RegraDeNegocioException;
import br.com.techmister.jogador.Jogador;
import br.com.techmister.jogador.JogadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ComportamentoService {

    private final ComportamentoRepository comportamentoRepository;
    private final JogadorRepository jogadorRepository;

    @Autowired
    public ComportamentoService(ComportamentoRepository comportamentoRepository, JogadorRepository jogadorRepository) {
        this.comportamentoRepository = comportamentoRepository;
        this.jogadorRepository = jogadorRepository;
    }

    @Transactional
    public ComportamentoResponse cadastrar(ComportamentoRequest req) {
        Jogador jogador = jogadorRepository.findById(req.jogadorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Jogador com id " + req.jogadorId() + " não encontrado."));
        comportamentoRepository.findByJogadorIdAndDataAvaliacao(jogador.getId(), req.dataAvaliacao()).ifPresent(c -> {
            throw new RegraDeNegocioException("Esse jogador já possui avaliação comportamental hoje. Utilize a opção alterar.");
        });
        Comportamento novo = Comportamento.builder().jogador(jogador).dataAvaliacao(req.dataAvaliacao())
                .notaFoco(req.notaFoco()).notaDisciplina(req.notaDisciplina()).notaAtitude(req.notaAtitude()).build();
        return ComportamentoResponse.de(comportamentoRepository.save(novo));
    }

    @Transactional
    public ComportamentoResponse alterar(Long id, ComportamentoRequest req) {
        Comportamento comp = buscarOuFalhar(id);
        Jogador jogador = jogadorRepository.findById(req.jogadorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Jogador com id " + req.jogadorId() + " não encontrado."));
        comp.setJogador(jogador); comp.setDataAvaliacao(req.dataAvaliacao());
        comp.setNotaFoco(req.notaFoco()); comp.setNotaDisciplina(req.notaDisciplina()); comp.setNotaAtitude(req.notaAtitude());
        return ComportamentoResponse.de(comportamentoRepository.save(comp));
    }

    @Transactional
    public void remover(Long id) { comportamentoRepository.delete(buscarOuFalhar(id)); }

    @Transactional(readOnly = true)
    public List<ComportamentoResponse> listar() { return comportamentoRepository.findAll().stream().map(ComportamentoResponse::de).toList(); }

    @Transactional(readOnly = true)
    public ComportamentoResponse buscar(Long id) { return ComportamentoResponse.de(buscarOuFalhar(id)); }

    private Comportamento buscarOuFalhar(Long id) {
        return comportamentoRepository.findById(id).orElseThrow(
                () -> new RecursoNaoEncontradoException("Comportamento com id " + id + " não encontrado."));
    }
}
