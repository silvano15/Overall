package br.com.overall.jogador;

import br.com.overall.estatistica.EstatisticaAgregador;
import br.com.overall.estatistica.EstatisticaRepository;
import br.com.overall.estatistica.Estatisticas;
import br.com.overall.exception.RecursoNaoEncontradoException;
import br.com.overall.jogador.dto.JogadorRequest;
import br.com.overall.jogador.dto.JogadorResponse;
import br.com.overall.pelada.PeladaJogadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class JogadorService {

    private final JogadorRepository jogadorRepository;
    private final EstatisticaRepository estatisticaRepository;
    private final EstatisticaAgregador agregador;
    private final PeladaJogadorRepository peladaJogadorRepository;

    public JogadorService(JogadorRepository jogadorRepository,
                          EstatisticaRepository estatisticaRepository,
                          EstatisticaAgregador agregador,
                          PeladaJogadorRepository peladaJogadorRepository) {
        this.jogadorRepository = jogadorRepository;
        this.estatisticaRepository = estatisticaRepository;
        this.agregador = agregador;
        this.peladaJogadorRepository = peladaJogadorRepository;
    }

    @Transactional
    public JogadorResponse cadastrar(JogadorRequest req) {
        String foto = req.foto() == null || req.foto().isBlank() ? null : req.foto();
        Jogador salvo = jogadorRepository.save(
                new Jogador(req.nome().trim(), foto, Boolean.TRUE.equals(req.goleiro())));
        return JogadorResponse.de(salvo, null);
    }

    @Transactional
    public JogadorResponse alterar(Long id, JogadorRequest req) {
        Jogador j = buscarOuFalhar(id);
        j.setNome(req.nome().trim());
        if (req.foto() != null) j.setFoto(req.foto().isBlank() ? null : req.foto()); // null = mantém
        if (req.goleiro() != null) j.setGoleiro(req.goleiro());
        return JogadorResponse.de(jogadorRepository.save(j), agregador.geral().get(id));
    }

    /**
     * Remove o jogador. Se ele já tem histórico (lista de alguma pelada ou estatísticas), é apenas arquivado:
     * some das listas, mas rankings e histórico continuam corretos.
     */
    @Transactional
    public void remover(Long id) {
        Jogador j = buscarOuFalhar(id);
        if (peladaJogadorRepository.existsByJogadorId(id) || estatisticaRepository.existsByJogadorId(id)) {
            j.setAtivo(false);
            jogadorRepository.save(j);
        } else {
            jogadorRepository.delete(j);
        }
    }

    @Transactional
    public JogadorResponse reativar(Long id) {
        Jogador j = buscarOuFalhar(id);
        j.setAtivo(true);
        return JogadorResponse.de(jogadorRepository.save(j), agregador.geral().get(id));
    }

    /** Todos os jogadores (inclusive arquivados: o cliente usa como cache de nomes e fotos). */
    @Transactional(readOnly = true)
    public List<JogadorResponse> listar() {
        Map<Long, Estatisticas> estatisticas = agregador.geral();
        return jogadorRepository.findAll().stream()
                .sorted(Comparator.comparing(Jogador::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(j -> JogadorResponse.de(j, estatisticas.get(j.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public JogadorResponse buscar(Long id) {
        return JogadorResponse.de(buscarOuFalhar(id), agregador.geral().get(id));
    }

    private Jogador buscarOuFalhar(Long id) {
        return jogadorRepository.findById(id).orElseThrow(
                () -> new RecursoNaoEncontradoException("Jogador com id " + id + " não encontrado."));
    }
}
