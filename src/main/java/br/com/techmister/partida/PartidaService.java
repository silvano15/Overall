package br.com.techmister.partida;

import br.com.techmister.desempenho.DesempenhoRepository;
import br.com.techmister.exception.RecursoNaoEncontradoException;
import br.com.techmister.exception.RegraDeNegocioException;
import br.com.techmister.partida.dto.PartidaRequest;
import br.com.techmister.partida.dto.PartidaResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class PartidaService {

    private final PartidaRepository partidaRepository;
    private final DesempenhoRepository desempenhoRepository;

    @Autowired
    public PartidaService(PartidaRepository partidaRepository, DesempenhoRepository desempenhoRepository) {
        this.partidaRepository = partidaRepository;
        this.desempenhoRepository = desempenhoRepository;
    }

    @Transactional
    public PartidaResponse cadastrar(PartidaRequest req) {
        partidaRepository.findByDataJogo(req.dataJogo()).ifPresent(p -> {
            throw new RegraDeNegocioException("Já existe uma partida agendada para esta data. Verifique o calendário.");
        });
        Partida nova = Partida.builder().adversario(req.adversario()).dataJogo(req.dataJogo())
                .golsPro(req.golsPro()).golsContra(req.golsContra()).local(req.local()).build();
        return PartidaResponse.de(partidaRepository.save(nova));
    }

    @Transactional
    public PartidaResponse alterar(Long id, PartidaRequest req) {
        Partida partida = buscarOuFalhar(id);
        partidaRepository.findByDataJogoAndIdNot(req.dataJogo(), id).ifPresent(p -> {
            throw new RegraDeNegocioException("Já existe uma partida marcada para esta data. Verifique o calendário.");
        });
        partida.setAdversario(req.adversario()); partida.setDataJogo(req.dataJogo());
        partida.setGolsPro(req.golsPro()); partida.setGolsContra(req.golsContra()); partida.setLocal(req.local());
        return PartidaResponse.de(partidaRepository.save(partida));
    }

    @Transactional
    public void remover(Long id) {
        Partida partida = buscarOuFalhar(id);
        if (desempenhoRepository.existsByPartidaId(id))
            throw new RegraDeNegocioException("Esta partida não pode ser removida pois possui dados de desempenho vinculados. Remova os desempenhos primeiro.");
        partidaRepository.delete(partida);
    }

    @Transactional(readOnly = true)
    public List<PartidaResponse> listar() { return partidaRepository.findAll().stream().map(PartidaResponse::de).toList(); }

    @Transactional(readOnly = true)
    public PartidaResponse buscar(Long id) { return PartidaResponse.de(buscarOuFalhar(id)); }

    private Partida buscarOuFalhar(Long id) {
        return partidaRepository.findById(id).orElseThrow(
                () -> new RecursoNaoEncontradoException("Partida com id " + id + " não encontrada."));
    }
}
