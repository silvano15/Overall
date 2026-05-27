package br.com.techmister.treino;

import br.com.techmister.exception.RecursoNaoEncontradoException;
import br.com.techmister.exception.RegraDeNegocioException;
import br.com.techmister.jogador.Jogador;
import br.com.techmister.jogador.JogadorRepository;
import br.com.techmister.treino.dto.TreinoRequest;
import br.com.techmister.treino.dto.TreinoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class TreinoService {

    private final TreinoRepository treinoRepository;
    private final JogadorRepository jogadorRepository;

    @Autowired
    public TreinoService(TreinoRepository treinoRepository, JogadorRepository jogadorRepository) {
        this.treinoRepository = treinoRepository;
        this.jogadorRepository = jogadorRepository;
    }

    @Transactional
    public TreinoResponse cadastrar(TreinoRequest req) {
        Jogador jogador = jogadorRepository.findById(req.jogadorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Jogador com id " + req.jogadorId() + " não encontrado."));
        treinoRepository.findByJogadorIdAndDataTreino(jogador.getId(), req.dataTreino()).ifPresent(t -> {
            throw new RegraDeNegocioException("Estatísticas de treino já registradas para este jogador hoje. Utilize a opção Alterar.");
        });
        Treino novo = Treino.builder().jogador(jogador).dataTreino(req.dataTreino())
                .acertos(req.acertos()).erros(req.erros()).build();
        return TreinoResponse.de(treinoRepository.save(novo));
    }

    @Transactional
    public TreinoResponse alterar(Long id, TreinoRequest req) {
        Treino treino = buscarOuFalhar(id);
        Jogador jogador = jogadorRepository.findById(req.jogadorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Jogador com id " + req.jogadorId() + " não encontrado."));
        treino.setJogador(jogador); treino.setDataTreino(req.dataTreino());
        treino.setAcertos(req.acertos()); treino.setErros(req.erros());
        return TreinoResponse.de(treinoRepository.save(treino));
    }

    @Transactional
    public void remover(Long id) { treinoRepository.delete(buscarOuFalhar(id)); }

    @Transactional(readOnly = true)
    public List<TreinoResponse> listarDaSemanaAtual() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fim = hoje.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return treinoRepository.findByDataTreinoBetween(inicio, fim).stream().map(TreinoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<TreinoResponse> listarTodos() { return treinoRepository.findAll().stream().map(TreinoResponse::de).toList(); }

    @Transactional(readOnly = true)
    public TreinoResponse buscar(Long id) { return TreinoResponse.de(buscarOuFalhar(id)); }

    private Treino buscarOuFalhar(Long id) {
        return treinoRepository.findById(id).orElseThrow(
                () -> new RecursoNaoEncontradoException("Treino com id " + id + " não encontrado."));
    }
}
