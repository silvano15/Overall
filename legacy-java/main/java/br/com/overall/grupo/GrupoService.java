package br.com.overall.grupo;

import br.com.overall.exception.RecursoNaoEncontradoException;
import br.com.overall.exception.RegraDeNegocioException;
import br.com.overall.grupo.dto.GrupoRequest;
import br.com.overall.grupo.dto.GrupoResponse;
import br.com.overall.jogador.JogadorRepository;
import br.com.overall.pelada.PeladaRepository;
import br.com.overall.pelada.PeladaStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GrupoService {

    private final GrupoPeladaRepository grupoRepository;
    private final JogadorRepository jogadorRepository;
    private final PeladaRepository peladaRepository;

    public GrupoService(GrupoPeladaRepository grupoRepository,
                        JogadorRepository jogadorRepository,
                        PeladaRepository peladaRepository) {
        this.grupoRepository = grupoRepository;
        this.jogadorRepository = jogadorRepository;
        this.peladaRepository = peladaRepository;
    }

    @Transactional
    public GrupoResponse criar(GrupoRequest req) {
        return montar(grupoRepository.save(new GrupoPelada(req.nome().trim())));
    }

    @Transactional
    public GrupoResponse atualizar(Long id, GrupoRequest req) {
        GrupoPelada g = buscar(id);
        g.setNome(req.nome().trim());
        return montar(grupoRepository.save(g));
    }

    @Transactional
    public void excluir(Long id) {
        GrupoPelada g = buscar(id);
        if (jogadorRepository.existsByGrupoId(id))
            throw new RegraDeNegocioException("Esse grupo já tem jogadores cadastrados e não pode ser excluído.");
        grupoRepository.delete(g);
    }

    @Transactional(readOnly = true)
    public List<GrupoResponse> listar() {
        return grupoRepository.findAllByOrderByNomeAsc().stream().map(this::montar).toList();
    }

    @Transactional(readOnly = true)
    public GrupoResponse obter(Long id) {
        return montar(buscar(id));
    }

    private GrupoPelada buscar(Long id) {
        return grupoRepository.findById(id).orElseThrow(
                () -> new RecursoNaoEncontradoException("Grupo não encontrado."));
    }

    private GrupoResponse montar(GrupoPelada g) {
        long jogadores = jogadorRepository.countByGrupoIdAndAtivoTrue(g.getId());
        long peladas = peladaRepository.countByGrupoId(g.getId());
        boolean aberta = peladaRepository.existsByGrupoIdAndStatusNot(g.getId(), PeladaStatus.FINALIZADA);
        return new GrupoResponse(g.getId(), g.getNome(), jogadores, peladas, aberta);
    }
}
