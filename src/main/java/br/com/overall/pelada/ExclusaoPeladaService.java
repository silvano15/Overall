package br.com.overall.pelada;

import br.com.overall.estatistica.EstatisticaAgregador;
import br.com.overall.estatistica.EstatisticaRepository;
import br.com.overall.estatistica.Estatisticas;
import br.com.overall.estatistica.OverallCalculator;
import br.com.overall.exception.RecursoNaoEncontradoException;
import br.com.overall.exception.RegraDeNegocioException;
import br.com.overall.jogador.Jogador;
import br.com.overall.jogador.JogadorRepository;
import br.com.overall.partida.GolRepository;
import br.com.overall.partida.Partida;
import br.com.overall.partida.PartidaJogadorRepository;
import br.com.overall.partida.PartidaRepository;
import br.com.overall.partida.PenaltiRepository;
import br.com.overall.partida.SubstituicaoRepository;
import br.com.overall.usuario.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Apaga uma pelada inteira (partidas, gols, estatísticas...), inclusive finalizada. Como é destrutivo e
 * irreversível, exige a senha do usuário logado: quem pegar o celular desbloqueado não apaga o histórico.
 */
@Service
public class ExclusaoPeladaService {

    private final PeladaRepository peladaRepository;
    private final PeladaJogadorRepository peladaJogadorRepository;
    private final TimePeladaRepository timePeladaRepository;
    private final TimePeladaJogadorRepository timePeladaJogadorRepository;
    private final PartidaRepository partidaRepository;
    private final PartidaJogadorRepository partidaJogadorRepository;
    private final GolRepository golRepository;
    private final SubstituicaoRepository substituicaoRepository;
    private final PenaltiRepository penaltiRepository;
    private final EstatisticaRepository estatisticaRepository;
    private final EstatisticaAgregador agregador;
    private final JogadorRepository jogadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ExclusaoPeladaService(PeladaRepository peladaRepository,
                                 PeladaJogadorRepository peladaJogadorRepository,
                                 TimePeladaRepository timePeladaRepository,
                                 TimePeladaJogadorRepository timePeladaJogadorRepository,
                                 PartidaRepository partidaRepository,
                                 PartidaJogadorRepository partidaJogadorRepository,
                                 GolRepository golRepository,
                                 SubstituicaoRepository substituicaoRepository,
                                 PenaltiRepository penaltiRepository,
                                 EstatisticaRepository estatisticaRepository,
                                 EstatisticaAgregador agregador,
                                 JogadorRepository jogadorRepository,
                                 UsuarioRepository usuarioRepository,
                                 PasswordEncoder passwordEncoder) {
        this.peladaRepository = peladaRepository;
        this.peladaJogadorRepository = peladaJogadorRepository;
        this.timePeladaRepository = timePeladaRepository;
        this.timePeladaJogadorRepository = timePeladaJogadorRepository;
        this.partidaRepository = partidaRepository;
        this.partidaJogadorRepository = partidaJogadorRepository;
        this.golRepository = golRepository;
        this.substituicaoRepository = substituicaoRepository;
        this.penaltiRepository = penaltiRepository;
        this.estatisticaRepository = estatisticaRepository;
        this.agregador = agregador;
        this.jogadorRepository = jogadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void excluir(Long peladaId, String username, String senha) {
        var usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RegraDeNegocioException("Senha incorreta."));
        if (!passwordEncoder.matches(senha, usuario.getSenha()))
            throw new RegraDeNegocioException("Senha incorreta.");

        Pelada pelada = peladaRepository.findById(peladaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pelada não encontrada."));
        List<Long> jogadores = peladaJogadorRepository.findByPeladaId(peladaId).stream()
                .map(PeladaJogador::getJogadorId).toList();

        // Ordem das chaves estrangeiras: do mais "de baixo" para a própria pelada.
        List<Partida> partidas = partidaRepository.findByPeladaIdOrderByOrdemAsc(peladaId);
        for (Partida p : partidas) {
            golRepository.deleteByPartidaId(p.getId());
            substituicaoRepository.deleteByPartidaId(p.getId());
            penaltiRepository.deleteByPartidaId(p.getId());
            partidaJogadorRepository.deleteByPartidaId(p.getId());
        }
        estatisticaRepository.deleteByPeladaId(peladaId);
        partidaRepository.flush();
        partidaRepository.deleteAll(partidas);
        partidaRepository.flush();

        List<Long> timeIds = timePeladaRepository.findByPeladaIdOrderByNumeroAsc(peladaId).stream()
                .map(TimePelada::getId).toList();
        if (!timeIds.isEmpty()) {
            timePeladaJogadorRepository.deleteByTimeIdIn(timeIds);
            timePeladaRepository.deleteByPeladaId(peladaId);
            timePeladaRepository.flush();
        }
        peladaJogadorRepository.deleteByPeladaId(peladaId);
        peladaRepository.delete(pelada);
        peladaRepository.flush();

        // O overall guardado no jogador só muda ao finalizar uma pelada: recalcula sem a que saiu.
        Map<Long, Estatisticas> geral = agregador.geral(pelada.getGrupoId());
        for (Jogador j : jogadorRepository.findAllById(jogadores)) {
            j.setOverall(OverallCalculator.calcular(geral.get(j.getId()), j.isGoleiro()));
            jogadorRepository.save(j);
        }
    }
}
