package br.com.techmister.jogador;

import br.com.techmister.comportamento.ComportamentoRepository;
import br.com.techmister.desempenho.DesempenhoRepository;
import br.com.techmister.exception.RecursoNaoEncontradoException;
import br.com.techmister.exception.RegraDeNegocioException;
import br.com.techmister.jogador.dto.JogadorRequest;
import br.com.techmister.jogador.dto.JogadorResponse;
import br.com.techmister.treino.TreinoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class JogadorService {

    private final JogadorRepository jogadorRepository;
    private final TreinoRepository treinoRepository;
    private final ComportamentoRepository comportamentoRepository;
    private final DesempenhoRepository desempenhoRepository;

    @Autowired
    public JogadorService(JogadorRepository jogadorRepository, TreinoRepository treinoRepository,
                          ComportamentoRepository comportamentoRepository, DesempenhoRepository desempenhoRepository) {
        this.jogadorRepository = jogadorRepository;
        this.treinoRepository = treinoRepository;
        this.comportamentoRepository = comportamentoRepository;
        this.desempenhoRepository = desempenhoRepository;
    }

    @Transactional
    public JogadorResponse cadastrar(JogadorRequest req) {
        jogadorRepository.findByDocumento(req.documento()).ifPresent(j -> {
            throw new RegraDeNegocioException("Já existe um jogador cadastrado com este documento.");
        });
        Jogador novo = Jogador.builder().nome(req.nome()).documento(req.documento())
                .posicao(req.posicao()).alturaCm(req.alturaCm()).pesoKg(req.pesoKg())
                .dataNascimento(req.dataNascimento()).ativo(true).build();
        return JogadorResponse.de(jogadorRepository.save(novo));
    }

    @Transactional
    public JogadorResponse alterar(Long id, JogadorRequest req) {
        Jogador jogador = buscarOuFalhar(id);
        jogadorRepository.findByDocumentoAndIdNot(req.documento(), id).ifPresent(j -> {
            throw new RegraDeNegocioException("Já existe outro jogador cadastrado com este documento.");
        });
        jogador.setNome(req.nome()); jogador.setDocumento(req.documento());
        jogador.setPosicao(req.posicao()); jogador.setAlturaCm(req.alturaCm());
        jogador.setPesoKg(req.pesoKg()); jogador.setDataNascimento(req.dataNascimento());
        return JogadorResponse.de(jogadorRepository.save(jogador));
    }

    @Transactional
    public void remover(Long id) {
        Jogador jogador = buscarOuFalhar(id);
        boolean temVinculo = treinoRepository.existsByJogadorId(id)
                || comportamentoRepository.existsByJogadorId(id)
                || desempenhoRepository.existsByJogadorId(id);
        if (temVinculo) throw new RegraDeNegocioException(
                "Este jogador não pode ser removido pois possui histórico de treinos/partidas. Deseja inativá-lo?");
        jogadorRepository.delete(jogador);
    }

    @Transactional(readOnly = true)
    public List<JogadorResponse> listar() {
        return jogadorRepository.findAll().stream().map(JogadorResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public JogadorResponse buscar(Long id) { return JogadorResponse.de(buscarOuFalhar(id)); }

    private Jogador buscarOuFalhar(Long id) {
        return jogadorRepository.findById(id).orElseThrow(
                () -> new RecursoNaoEncontradoException("Jogador com id " + id + " não encontrado."));
    }
}
