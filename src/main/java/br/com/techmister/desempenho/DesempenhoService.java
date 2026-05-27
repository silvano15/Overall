package br.com.techmister.desempenho;

import br.com.techmister.desempenho.dto.DesempenhoRequest;
import br.com.techmister.desempenho.dto.DesempenhoResponse;
import br.com.techmister.exception.RecursoNaoEncontradoException;
import br.com.techmister.exception.RegraDeNegocioException;
import br.com.techmister.jogador.Jogador;
import br.com.techmister.jogador.JogadorRepository;
import br.com.techmister.partida.Partida;
import br.com.techmister.partida.PartidaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class DesempenhoService {

    private final DesempenhoRepository desempenhoRepository;
    private final PartidaRepository partidaRepository;
    private final JogadorRepository jogadorRepository;

    @Autowired
    public DesempenhoService(DesempenhoRepository desempenhoRepository, PartidaRepository partidaRepository, JogadorRepository jogadorRepository) {
        this.desempenhoRepository = desempenhoRepository;
        this.partidaRepository = partidaRepository;
        this.jogadorRepository = jogadorRepository;
    }

    @Transactional
    public DesempenhoResponse cadastrar(DesempenhoRequest req) {
        Partida partida = partidaRepository.findById(req.partidaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Partida com id " + req.partidaId() + " não encontrada."));
        if (partida.getDataJogo().isAfter(LocalDate.now()))
            throw new RegraDeNegocioException("Não é possível registrar desempenho para uma partida que ainda não ocorreu.");
        Jogador jogador = jogadorRepository.findById(req.jogadorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Jogador com id " + req.jogadorId() + " não encontrado."));
        desempenhoRepository.findByPartidaIdAndJogadorId(partida.getId(), jogador.getId()).ifPresent(d -> {
            throw new RegraDeNegocioException("Já existe desempenho registrado para este jogador nesta partida.");
        });
        Desempenho novo = Desempenho.builder().partida(partida).jogador(jogador).nota(req.nota())
                .gols(req.gols()).assistencias(req.assistencias())
                .cartoesAmarelos(req.cartoesAmarelos()).cartoesVermelhos(req.cartoesVermelhos()).build();
        return DesempenhoResponse.de(desempenhoRepository.save(novo));
    }

    @Transactional
    public DesempenhoResponse alterar(Long id, DesempenhoRequest req) {
        Desempenho d = buscarOuFalhar(id);
        Partida partida = partidaRepository.findById(req.partidaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Partida com id " + req.partidaId() + " não encontrada."));
        Jogador jogador = jogadorRepository.findById(req.jogadorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Jogador com id " + req.jogadorId() + " não encontrado."));
        d.setPartida(partida); d.setJogador(jogador); d.setNota(req.nota());
        d.setGols(req.gols()); d.setAssistencias(req.assistencias());
        d.setCartoesAmarelos(req.cartoesAmarelos()); d.setCartoesVermelhos(req.cartoesVermelhos());
        return DesempenhoResponse.de(desempenhoRepository.save(d));
    }

    @Transactional
    public void remover(Long id) { desempenhoRepository.delete(buscarOuFalhar(id)); }

    @Transactional(readOnly = true)
    public List<DesempenhoResponse> listar() { return desempenhoRepository.findAll().stream().map(DesempenhoResponse::de).toList(); }

    @Transactional(readOnly = true)
    public DesempenhoResponse buscar(Long id) { return DesempenhoResponse.de(buscarOuFalhar(id)); }

    private Desempenho buscarOuFalhar(Long id) {
        return desempenhoRepository.findById(id).orElseThrow(
                () -> new RecursoNaoEncontradoException("Desempenho com id " + id + " não encontrado."));
    }
}
