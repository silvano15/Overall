package br.com.overall.grupo;

import br.com.overall.grupo.dto.GrupoRequest;
import br.com.overall.grupo.dto.GrupoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/grupos")
@Tag(name = "Grupos", description = "Grupos de pelada: cada um com seu elenco, suas peladas e seu ranking")
public class GrupoController {

    private final GrupoService grupoService;

    public GrupoController(GrupoService grupoService) {
        this.grupoService = grupoService;
    }

    @PostMapping
    @Operation(summary = "Cria um grupo de pelada")
    public ResponseEntity<GrupoResponse> criar(@Valid @RequestBody GrupoRequest req, UriComponentsBuilder uri) {
        GrupoResponse criado = grupoService.criar(req);
        URI location = uri.path("/grupos/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Renomeia o grupo")
    public GrupoResponse atualizar(@PathVariable Long id, @Valid @RequestBody GrupoRequest req) {
        return grupoService.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um grupo sem jogadores cadastrados")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        grupoService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    @Operation(summary = "Lista todos os grupos de pelada")
    public List<GrupoResponse> listar() {
        return grupoService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um grupo pelo id")
    public GrupoResponse obter(@PathVariable Long id) {
        return grupoService.obter(id);
    }
}
