package br.com.overall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Percorre uma pelada inteira pela API, contra H2 rodando as migrations reais:
 * jogadores → lista → sorteio → partidas (cronômetro, gols, substituição, empate, pênaltis) → finalização → rankings.
 * Os testes são ordenados e compartilham estado de propósito: cada um continua de onde o anterior parou.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FluxoDaPeladaTest {

    /** Relógio que o teste controla, para simular a passagem do tempo sem esperar. */
    static class RelogioDeTeste extends Clock {
        private volatile Instant agora = Instant.now();
        private final ZoneId zona = ZoneId.of("America/Recife");

        void avancar(Duration d) { agora = agora.plus(d); }
        @Override public ZoneId getZone() { return zona; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() { return new RelogioDeTeste(); }
    }

    private static final String FOTO_PNG = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired RelogioDeTeste relogio;

    // estado compartilhado entre os passos
    String token;
    long grupoId;
    long g1, g2, tardio;
    final List<Long> linha = new ArrayList<>();
    long peladaId;
    long timeA, timeB;
    List<Long> jogadoresA, jogadoresB;
    long partida1, partida2, partida3;

    // ---------------------------------------------------------------- helpers

    JsonNode chamar(MockHttpServletRequestBuilder b, Object corpo, int statusEsperado) throws Exception {
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (corpo != null) b.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(corpo));
        MvcResult r = mvc.perform(b).andReturn();
        String texto = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals(statusEsperado, r.getResponse().getStatus(), "resposta: " + texto);
        return texto.isBlank() ? null : mapper.readTree(texto);
    }

    JsonNode ok(MockHttpServletRequestBuilder b) throws Exception { return chamar(b, null, 200); }
    JsonNode ok(MockHttpServletRequestBuilder b, Object corpo) throws Exception { return chamar(b, corpo, 200); }
    JsonNode erro(MockHttpServletRequestBuilder b, Object corpo) throws Exception { return chamar(b, corpo, 400); }

    long criarJogador(String nome, boolean goleiro) throws Exception {
        return chamar(post("/jogadores?grupoId=" + grupoId), Map.of("nome", nome, "goleiro", goleiro, "foto", FOTO_PNG), 201).get("id").asLong();
    }

    List<Long> ids(JsonNode times, int indice) {
        List<Long> ids = new ArrayList<>();
        times.get(indice).get("jogadores").forEach(j -> ids.add(j.get("jogadorId").asLong()));
        return ids;
    }

    Map<String, Object> config(long a, long b, int minutos, int gols) {
        return Map.of("timeAId", a, "timeBId", b, "duracaoMinutos", minutos, "golsParaEncerrar", gols);
    }

    Map<String, Object> gol(long timeId, long autor, Long assistente) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("timeId", timeId); m.put("autorId", autor); m.put("assistenteId", assistente);
        return m;
    }

    // ---------------------------------------------------------------- passos

    @Test @Order(1)
    void semTokenNaoEntra() throws Exception {
        String antes = token;
        token = null;
        mvc.perform(get("/jogadores?grupoId=" + grupoId)).andExpect(r -> assertTrue(
                r.getResponse().getStatus() == 401 || r.getResponse().getStatus() == 403));
        token = antes;
    }

    @Test @Order(2)
    void loginDoAdminSemeadoPelaMigration() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"senha\":\"errada\"}"))
                .andExpect(r -> assertEquals(401, r.getResponse().getStatus()));
        JsonNode login = chamar(post("/auth/login"), Map.of("username", "admin", "senha", "admin123"), 200);
        token = login.get("token").asText();
        assertEquals("ADMIN", login.get("perfil").asText());

        JsonNode grupo = chamar(post("/grupos"), Map.of("nome", "  Galera do Teste  "), 201);
        grupoId = grupo.get("id").asLong();
        assertEquals("Galera do Teste", grupo.get("nome").asText());
        erro(post("/grupos"), Map.of("nome", " "));
    }

    @Test @Order(3)
    void cadastroDeJogadoresSoExigeNomeEFoto() throws Exception {
        erro(post("/jogadores?grupoId=" + grupoId), Map.of("nome", "  "));
        erro(post("/jogadores?grupoId=" + grupoId), Map.of("nome", "Fulano", "foto", "http://malicioso/x.png"));
        erro(post("/jogadores?grupoId=" + grupoId), Map.of("nome", "Fulano", "foto", "data:text/html;base64,PHNjcmlwdD4="));

        g1 = criarJogador("Goleiro Um", true);
        g2 = criarJogador("Goleiro Dois", true);
        for (int i = 1; i <= 6; i++) linha.add(criarJogador("Linha " + i, false));
        tardio = chamar(post("/jogadores?grupoId=" + grupoId), Map.of("nome", "Chegou Tarde"), 201).get("id").asLong();

        JsonNode lista = ok(get("/jogadores?grupoId=" + grupoId));
        assertEquals(9, lista.size());
        JsonNode primeiro = lista.get(0);
        assertEquals(60, primeiro.get("overall").asInt(), "quem nunca jogou começa em 60");
        assertEquals(0, primeiro.get("estatisticas").get("partidas").asInt());
    }

    @Test @Order(4)
    void editarJogadorMantemFotoQuandoNaoEnviada() throws Exception {
        JsonNode editado = ok(put("/jogadores/" + linha.get(0)), Map.of("nome", "Linha 1 Editado"));
        assertEquals("Linha 1 Editado", editado.get("nome").asText());
        assertEquals(FOTO_PNG, editado.get("foto").asText());
        ok(put("/jogadores/" + linha.get(0)), Map.of("nome", "Linha 1"));
    }

    @Test @Order(5)
    void criarPeladaValidaDataELimitaUmaEmAberto() throws Exception {
        LocalDate hoje = LocalDate.now(relogio);
        erro(post("/peladas?grupoId=" + grupoId), Map.of("dataJogo", hoje.minusDays(1).toString(), "horaJogo", "08:00"));

        JsonNode criada = chamar(post("/peladas?grupoId=" + grupoId), Map.of("dataJogo", hoje.plusDays(3).toString(), "horaJogo", "08:30"), 201);
        peladaId = criada.get("id").asLong();
        assertEquals("ABERTA", criada.get("status").asText());
        assertFalse(criada.get("sorteioLiberado").asBoolean(), "sorteio só no dia");

        erro(post("/peladas?grupoId=" + grupoId), Map.of("dataJogo", hoje.plusDays(4).toString(), "horaJogo", "08:00"));
        assertEquals(peladaId, ok(get("/peladas/atual?grupoId=" + grupoId)).get("id").asLong());
    }

    @Test @Order(6)
    void listaDaSemanaEOSorteioSoNoDia() throws Exception {
        for (long id : List.of(g1, g2)) ok(put("/peladas/" + peladaId + "/lista/" + id));
        for (long id : linha) ok(put("/peladas/" + peladaId + "/lista/" + id));
        JsonNode lista = ok(put("/peladas/" + peladaId + "/lista/" + linha.get(0))); // idempotente
        assertEquals(8, lista.get("lista").size());

        // antes do dia: bloqueado
        JsonNode e = erro(post("/peladas/" + peladaId + "/sorteio"), Map.of("jogadoresPorTime", 4));
        assertTrue(e.get("mensagem").asText().contains("no dia da pelada"), e.toString());

        // "chega o dia": ADM corrige a data para hoje e o sorteio é liberado
        String hoje = LocalDate.now(relogio).toString();
        JsonNode liberada = ok(put("/peladas/" + peladaId), Map.of("dataJogo", hoje, "horaJogo", "08:30"));
        assertTrue(liberada.get("sorteioLiberado").asBoolean());

        erro(post("/peladas/" + peladaId + "/sorteio"), Map.of("jogadoresPorTime", 8)); // só 1 time
    }

    @Test @Order(7)
    void sorteioExcluiGoleirosEMarcaOModo() throws Exception {
        // 6 jogadores de linha, 3 por time: os 2 goleiros ficam de fora do sorteio (não ocupam vaga em time algum).
        JsonNode sorteada = ok(post("/peladas/" + peladaId + "/sorteio"), Map.of("jogadoresPorTime", 3));
        assertEquals("SORTEADA", sorteada.get("status").asText());
        assertEquals("ALEATORIO", sorteada.get("modoSorteio").asText(), "sem histórico o sorteio é aleatório");
        JsonNode times = sorteada.get("times");
        assertEquals(2, times.size());
        assertEquals(3, times.get(0).get("jogadores").size());
        assertEquals(3, times.get(1).get("jogadores").size());

        timeA = times.get(0).get("id").asLong();
        timeB = times.get(1).get("id").asLong();
        jogadoresA = ids(times, 0);
        jogadoresB = ids(times, 1);
        assertFalse(jogadoresA.contains(g1) || jogadoresA.contains(g2)
                || jogadoresB.contains(g1) || jogadoresB.contains(g2), "goleiros não entram em time nenhum");
        assertEquals(timeA, sorteada.get("proximaSugerida").get("timeAId").asLong());
        assertEquals(timeB, sorteada.get("proximaSugerida").get("timeBId").asLong());

        // re-sortear enquanto não começou é permitido e mantém 2 times
        JsonNode denovo = ok(post("/peladas/" + peladaId + "/sorteio"), Map.of("jogadoresPorTime", 3));
        assertEquals(2, denovo.get("times").size());
        timeA = denovo.get("times").get(0).get("id").asLong();
        timeB = denovo.get("times").get(1).get("id").asLong();
        jogadoresA = ids(denovo.get("times"), 0);
        jogadoresB = ids(denovo.get("times"), 1);
    }

    @Test @Order(8)
    void primeiraPartidaTerminaNoLimiteDeGols() throws Exception {
        erro(post("/peladas/" + peladaId + "/partidas"), config(timeA, timeA, 5, 2)); // mesmo time
        JsonNode p = chamar(post("/peladas/" + peladaId + "/partidas"), config(timeA, timeB, 5, 2), 201);
        partida1 = p.get("id").asLong();
        assertEquals("AGUARDANDO", p.get("status").asText());
        erro(post("/peladas/" + peladaId + "/partidas"), config(timeA, timeB, 5, 2)); // já há uma em aberto

        // antes de iniciar, o ADM escolhe o goleiro de cada time: g1/g2 não estão escalados em time
        // nenhum (goleiros ficam de fora do sorteio), mas qualquer jogador da lista pode ir para o gol.
        JsonNode comGoleiroA = ok(put("/partidas/" + partida1 + "/goleiro"), Map.of("timeId", timeA, "jogadorId", g1));
        boolean g1NoGol = false;
        for (JsonNode a : comGoleiroA.get("timeA").get("jogadores"))
            if (a.get("jogadorId").asLong() == g1) g1NoGol = a.get("goleiro").asBoolean() && a.get("emCampo").asBoolean();
        assertTrue(g1NoGol, "jogador de fora do time entra marcado como goleiro e em campo");

        JsonNode comGoleiroB = ok(put("/partidas/" + partida1 + "/goleiro"), Map.of("timeId", timeB, "jogadorId", g2));
        boolean g2NoGol = false;
        for (JsonNode a : comGoleiroB.get("timeB").get("jogadores"))
            if (a.get("jogadorId").asLong() == g2) g2NoGol = a.get("goleiro").asBoolean() && a.get("emCampo").asBoolean();
        assertTrue(g2NoGol);

        // gol antes de iniciar não vale
        erro(post("/partidas/" + partida1 + "/gols"), gol(timeA, jogadoresA.get(0), null));

        JsonNode rodando = ok(post("/partidas/" + partida1 + "/iniciar"));
        assertEquals("EM_ANDAMENTO", rodando.get("status").asText());
        assertTrue(rodando.get("relogioCorrendo").asBoolean());

        // cronômetro: 90s correndo, pausa (não conta), retoma
        relogio.avancar(Duration.ofSeconds(90));
        assertEquals(90_000, ok(get("/partidas/" + partida1)).get("decorridoMs").asLong(), 50);
        ok(post("/partidas/" + partida1 + "/pausar"));
        relogio.avancar(Duration.ofSeconds(60));
        JsonNode pausada = ok(get("/partidas/" + partida1));
        assertEquals("PAUSADA", pausada.get("status").asText());
        assertEquals(90_000, pausada.get("decorridoMs").asLong(), 50);
        ok(post("/partidas/" + partida1 + "/retomar"));

        // autor de outro time é rejeitado; assistente não pode ser o próprio autor
        erro(post("/partidas/" + partida1 + "/gols"), gol(timeA, jogadoresB.get(0), null));
        erro(post("/partidas/" + partida1 + "/gols"), gol(timeA, jogadoresA.get(0), jogadoresA.get(0)));

        // gol com assistência + gol sem assistência (pênalti) → 2 gols = fim
        JsonNode g = ok(post("/partidas/" + partida1 + "/gols"), gol(timeA, jogadoresA.get(0), jogadoresA.get(1)));
        assertEquals(1, g.get("timeA").get("gols").asInt());
        assertEquals("EM_ANDAMENTO", g.get("status").asText());
        JsonNode fim = ok(post("/partidas/" + partida1 + "/gols"), gol(timeA, jogadoresA.get(2), null));
        assertEquals(2, fim.get("timeA").get("gols").asInt());
        assertEquals("ENCERRADA", fim.get("status").asText(), "limite de gols encerra a partida");
        assertEquals(timeA, fim.get("vencedorId").asLong());

        erro(post("/partidas/" + partida1 + "/gols"), gol(timeB, jogadoresB.get(0), null));
    }

    @Test @Order(9)
    void quemVenceFicaEODesafianteVemDaFila() throws Exception {
        JsonNode pelada = ok(get("/peladas/" + peladaId));
        assertEquals("EM_ANDAMENTO", pelada.get("status").asText());
        assertTrue(pelada.get("partidaEmAbertoId").isNull());
        assertEquals(timeA, pelada.get("proximaSugerida").get("timeAId").asLong(), "vencedor fica");
        assertEquals(timeB, pelada.get("proximaSugerida").get("timeBId").asLong());
        assertEquals(5, pelada.get("duracaoMinutos").asInt(), "config da última partida vira o padrão da próxima");
    }

    @Test @Order(10)
    void substituicaoEmpatePorTempoEPenaltis() throws Exception {
        // chega um jogador atrasado: entra na lista e depois na partida por substituição
        ok(put("/peladas/" + peladaId + "/lista/" + tardio));

        partida2 = chamar(post("/peladas/" + peladaId + "/partidas"), config(timeA, timeB, 5, 3), 201).get("id").asLong();
        ok(post("/partidas/" + partida2 + "/iniciar"));

        long saiu = jogadoresB.get(jogadoresB.size() - 1);
        Map<String, Object> troca = new java.util.HashMap<>();
        troca.put("timeId", timeB); troca.put("saiuId", saiu); troca.put("entrouId", tardio);
        JsonNode aposTroca = ok(post("/partidas/" + partida2 + "/substituicoes"), troca);
        assertEquals(1, aposTroca.get("substituicoes").size());
        boolean tardioEmCampo = false, saiuFora = false;
        for (JsonNode a : aposTroca.get("timeB").get("jogadores")) {
            if (a.get("jogadorId").asLong() == tardio) tardioEmCampo = a.get("emCampo").asBoolean();
            if (a.get("jogadorId").asLong() == saiu) saiuFora = !a.get("emCampo").asBoolean();
        }
        assertTrue(tardioEmCampo && saiuFora);
        erro(post("/partidas/" + partida2 + "/substituicoes"), troca); // ele já está em campo
        // quem saiu não pode fazer gol; quem entrou pode
        erro(post("/partidas/" + partida2 + "/gols"), gol(timeB, saiu, null));

        ok(post("/partidas/" + partida2 + "/gols"), gol(timeA, jogadoresA.get(0), jogadoresA.get(2)));
        ok(post("/partidas/" + partida2 + "/gols"), gol(timeB, tardio, null));

        // desfazer e refazer um gol
        JsonNode com = ok(post("/partidas/" + partida2 + "/gols"), gol(timeA, jogadoresA.get(1), null));
        long golErrado = com.get("gols").get(com.get("gols").size() - 1).get("id").asLong();
        JsonNode sem = ok(delete("/partidas/" + partida2 + "/gols/" + golErrado));
        assertEquals(1, sem.get("timeA").get("gols").asInt());

        // o tempo acaba 1x1: a partida fica pendente de decisão, sem ninguém com a tela aberta
        relogio.avancar(Duration.ofMinutes(6));
        JsonNode empate = ok(get("/partidas/" + partida2));
        assertEquals("EMPATE_PENDENTE", empate.get("status").asText());
        assertEquals(300_000, empate.get("decorridoMs").asLong());

        // o tipo de disputa é obrigatório, e a pré-determinada exige a quantidade
        erro(post("/partidas/" + partida2 + "/penaltis/iniciar"), Map.of());
        erro(post("/partidas/" + partida2 + "/penaltis/iniciar"), Map.of("modo", "PREDETERMINADO"));
        JsonNode aberta = ok(post("/partidas/" + partida2 + "/penaltis/iniciar"),
                Map.of("modo", "PREDETERMINADO", "quantidade", 5));
        assertEquals(5, aberta.get("penaltis").get("rodadas").asInt());
        assertEquals("PREDETERMINADO", aberta.get("penaltis").get("modo").asText());

        // cobrança de quem não é do time da vez (A bate primeiro) é rejeitada
        Map<String, Object> errada = Map.of("jogadorId", jogadoresB.get(0), "convertido", true);
        erro(post("/partidas/" + partida2 + "/penaltis"), errada);

        // A✓ B✗ A✓ B✗ A✓ → ainda aberto (B pode empatar); B✗ → A venceu
        long[] cobradoresA = {jogadoresA.get(0), jogadoresA.get(1), jogadoresA.get(2)};
        long[] cobradoresB = {jogadoresB.get(0), jogadoresB.get(1), tardio}; // tardio entrou no lugar de jogadoresB.get(2)
        for (int i = 0; i < 3; i++) {
            ok(post("/partidas/" + partida2 + "/penaltis"), Map.of("jogadorId", cobradoresA[i], "convertido", true));
            JsonNode s = ok(post("/partidas/" + partida2 + "/penaltis"), Map.of("jogadorId", cobradoresB[i], "convertido", false));
            if (i < 2) assertTrue(s.get("penaltis").get("decididoParaTimeId").isNull());
            else assertEquals(timeA, s.get("penaltis").get("decididoParaTimeId").asLong());
        }
        erro(post("/partidas/" + partida2 + "/penaltis"), Map.of("jogadorId", cobradoresA[0], "convertido", true));

        // desfaz a última cobrança (errei o lançamento) e refaz como gol do B → segue aberto
        JsonNode desfeito = ok(delete("/partidas/" + partida2 + "/penaltis/ultimo"));
        assertTrue(desfeito.get("penaltis").get("decididoParaTimeId").isNull());
        ok(post("/partidas/" + partida2 + "/penaltis"), Map.of("jogadorId", cobradoresB[2], "convertido", false));

        JsonNode encerrada = ok(post("/partidas/" + partida2 + "/penaltis/encerrar"));
        assertEquals("ENCERRADA", encerrada.get("status").asText());
        assertTrue(encerrada.get("vencedorId").isNull(), "no placar continua empate");
        assertEquals(timeA, encerrada.get("vencedorPenaltisId").asLong());
    }

    @Test @Order(11)
    void reiniciarZeraECancelarDescartaPartidaNaoIniciada() throws Exception {
        long p = chamar(post("/peladas/" + peladaId + "/partidas"), config(timeA, timeB, 5, 3), 201).get("id").asLong();
        ok(post("/partidas/" + p + "/iniciar"));
        ok(post("/partidas/" + p + "/gols"), gol(timeA, jogadoresA.get(0), null));
        relogio.avancar(Duration.ofSeconds(30));

        JsonNode zerada = ok(post("/partidas/" + p + "/reiniciar"));
        assertEquals("AGUARDANDO", zerada.get("status").asText());
        assertEquals(0, zerada.get("decorridoMs").asLong());
        assertEquals(0, zerada.get("timeA").get("gols").asInt());

        chamar(delete("/partidas/" + p), null, 204);
        assertNotNull(ok(get("/peladas/" + peladaId)).get("partidas"));
        assertEquals(2, ok(get("/peladas/" + peladaId)).get("partidas").size(), "a partida descartada não aparece");
    }

    @Test @Order(12)
    void encerrarManualmenteValePeloPlacar() throws Exception {
        partida3 = chamar(post("/peladas/" + peladaId + "/partidas"), config(timeB, timeA, 5, 3), 201).get("id").asLong();
        // finalizar a pelada com partida aberta não pode
        erro(post("/peladas/" + peladaId + "/finalizar"), null);

        ok(post("/partidas/" + partida3 + "/iniciar"));
        ok(post("/partidas/" + partida3 + "/gols"), gol(timeB, jogadoresB.get(0), jogadoresB.get(1)));
        JsonNode fim = ok(post("/partidas/" + partida3 + "/encerrar"));
        assertEquals("ENCERRADA", fim.get("status").asText());
        assertEquals(timeB, fim.get("vencedorId").asLong());
    }

    @Test @Order(13)
    void finalizarGeraResumoENovosOveralls() throws Exception {
        JsonNode resumo = chamar(post("/peladas/" + peladaId + "/finalizar"), null, 200);
        assertEquals("FINALIZADA", resumo.get("status").asText());
        assertEquals(3, resumo.get("partidasDisputadas").asInt());

        // ranking de times: 1 vitória, 1 empate (decidido nos pênaltis) e 1 derrota para cada lado
        JsonNode times = resumo.get("times");
        assertEquals(2, times.size());
        int jogos = times.get(0).get("jogos").asInt() + times.get(1).get("jogos").asInt();
        assertEquals(6, jogos);

        // artilheiro do dia e assistências
        assertTrue(resumo.get("artilheiros").size() >= 1);
        long totalGols = 0;
        for (JsonNode a : resumo.get("artilheiros")) totalGols += a.get("valor").asLong();
        // 2 (partida 1) + 2 (partida 2; o terceiro foi desfeito) + 1 (partida 3). Pênaltis não contam como gol.
        assertEquals(5, totalGols);

        // todos que jogaram têm nota e overall novo
        int comNota = 0;
        for (JsonNode j : resumo.get("jogadores")) {
            assertFalse(j.get("overallDepois").isNull(), j.toString());
            double nota = j.get("nota").asDouble();
            assertTrue(nota >= 3 && nota <= 10);
            comNota++;
        }
        assertEquals(9, comNota);

        // quem fez 1 gol e ganhou a partida 1 e a partida 3 deve ter passado de 60?
        // (não dá para afirmar sem conhecer o time; valida só que os overalls mudaram para alguém)
        boolean alguemMudou = false;
        for (JsonNode j : resumo.get("jogadores"))
            if (j.get("overallAntes").asInt() != j.get("overallDepois").asInt()) alguemMudou = true;
        assertTrue(alguemMudou, "o overall precisa refletir as estatísticas");

        // cache do jogador atualizado
        JsonNode jogadores = ok(get("/jogadores?grupoId=" + grupoId));
        int diferentesDe60 = 0;
        for (JsonNode j : jogadores) if (j.get("overall").asInt() != 60) diferentesDe60++;
        assertTrue(diferentesDe60 > 0);

        assertNull(chamar(get("/peladas/atual?grupoId=" + grupoId), null, 204));
    }

    @Test @Order(14)
    void rankingsDoMesDoAnoEGeral() throws Exception {
        for (String periodo : List.of("MES", "ANO", "GERAL")) {
            JsonNode r = ok(get("/rankings?grupoId=" + grupoId + "&periodo=" + periodo));
            assertEquals(1, r.get("peladas").asInt(), periodo);
            assertTrue(r.get("vencedores").size() > 0, periodo);
            assertTrue(r.get("goleadores").size() > 0, periodo);
            assertTrue(r.get("assistentes").size() > 0, periodo);
            assertEquals(9, r.get("notas").size(), periodo);
            assertEquals(9, r.get("overalls").size(), periodo);
            // ordenado do maior para o menor
            double anterior = Double.MAX_VALUE;
            for (JsonNode i : r.get("overalls")) {
                assertTrue(i.get("valor").asDouble() <= anterior);
                anterior = i.get("valor").asDouble();
            }
        }
        // o time A ganhou a partida 1 sem levar gol: o goleiro dele lidera os "sem sofrer"
        JsonNode goleiros = ok(get("/rankings?grupoId=" + grupoId + "&periodo=GERAL")).get("goleiros");
        assertTrue(goleiros.size() >= 1);
        assertTrue(goleiros.get(0).get("valor").asDouble() >= 1);

        // mês sem partidas → vazio
        LocalDate hoje = LocalDate.now(relogio);
        LocalDate outroMes = hoje.minusMonths(2);
        JsonNode vazio = ok(get("/rankings?grupoId=" + grupoId + "&periodo=MES&ano=" + outroMes.getYear() + "&mes=" + outroMes.getMonthValue()));
        assertEquals(0, vazio.get("peladas").asInt());
        assertEquals(0, vazio.get("goleadores").size());

        // rede de assistências: 3 gols com assistência (o gol da partida 1 sem assistência e o desfeito não contam)
        JsonNode rede = ok(get("/rankings/conexoes?grupoId=" + grupoId + "&periodo=GERAL"));
        long totalAssistencias = 0;
        java.util.Set<Long> nosDaRede = new java.util.HashSet<>();
        rede.get("nos").forEach(n -> nosDaRede.add(n.get("jogadorId").asLong()));
        for (JsonNode a : rede.get("arestas")) {
            totalAssistencias += a.get("quantidade").asLong();
            assertTrue(nosDaRede.contains(a.get("assistenteId").asLong()), "toda ponta da aresta é um nó");
            assertTrue(nosDaRede.contains(a.get("autorId").asLong()));
        }
        assertEquals(3, rede.get("arestas").size());
        assertEquals(3, totalAssistencias);
        assertEquals(0, ok(get("/rankings/conexoes?grupoId=" + grupoId + "&periodo=MES&ano=" + outroMes.getYear() + "&mes="
                + outroMes.getMonthValue())).get("arestas").size());

        erro(get("/rankings?grupoId=" + grupoId + "&periodo=SEMANA"), null);
    }

    @Test @Order(15)
    void segundaPeladaUsaOOverallParaBalancear() throws Exception {
        LocalDate hoje = LocalDate.now(relogio);
        long segunda = chamar(post("/peladas?grupoId=" + grupoId), Map.of("dataJogo", hoje.toString(), "horaJogo", "09:00"), 201).get("id").asLong();
        List<Long> todos = new ArrayList<>(List.of(g1, g2, tardio));
        todos.addAll(linha);
        for (long id : todos) ok(put("/peladas/" + segunda + "/lista/" + id));

        // Ainda são só 3 partidas de histórico por pelada: cada jogador tem 3 partidas ⇒ "tem dados"
        JsonNode sorteada = ok(post("/peladas/" + segunda + "/sorteio"), Map.of("jogadoresPorTime", 3));
        assertEquals("OVERALL", sorteada.get("modoSorteio").asText(), "com histórico o sorteio passa a usar overall");
        assertEquals(3, sorteada.get("times").size());

        // a pelada sem partidas pode ser excluída
        chamar(delete("/peladas/" + segunda), null, 204);
        assertNull(chamar(get("/peladas/atual?grupoId=" + grupoId), null, 204));
    }

    @Test @Order(16)
    void jogadorComHistoricoEArquivadoNaoApagado() throws Exception {
        chamar(delete("/jogadores/" + g1), null, 204);
        JsonNode lista = ok(get("/jogadores?grupoId=" + grupoId));
        boolean arquivado = false;
        for (JsonNode j : lista) if (j.get("id").asLong() == g1) arquivado = !j.get("ativo").asBoolean();
        assertTrue(arquivado);

        // arquivado não entra em lista nova
        long nova = chamar(post("/peladas?grupoId=" + grupoId), Map.of("dataJogo", LocalDate.now(relogio).toString(), "horaJogo", "10:00"), 201)
                .get("id").asLong();
        erro(put("/peladas/" + nova + "/lista/" + g1), null);
        assertEquals(g1, ok(post("/jogadores/" + g1 + "/reativar")).get("id").asLong());
        ok(put("/peladas/" + nova + "/lista/" + g1));
        chamar(delete("/peladas/" + nova), null, 204);

        // quem nunca jogou é apagado de verdade
        long sozinho = criarJogador("Sem Historico", false);
        chamar(delete("/jogadores/" + sozinho), null, 204);
        for (JsonNode j : ok(get("/jogadores?grupoId=" + grupoId))) assertTrue(j.get("id").asLong() != sozinho);
    }

    @Test @Order(17)
    void alterarSenha() throws Exception {
        erro(put("/conta/senha"), Map.of("senhaAtual", "errada", "novaSenha", "novaSenha123"));
        erro(put("/conta/senha"), Map.of("senhaAtual", "admin123", "novaSenha", "123"));
        chamar(put("/conta/senha"), Map.of("senhaAtual", "admin123", "novaSenha", "novaSenha123"), 204);
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"senha\":\"admin123\"}"))
                .andExpect(r -> assertEquals(401, r.getResponse().getStatus()));
        chamar(post("/auth/login"), Map.of("username", "admin", "senha", "novaSenha123"), 200);
    }

    /** Empata a partida (0x0) e devolve o estado já em EMPATE_PENDENTE. */
    JsonNode partidaEmpatada(long peladaId, long a, long b) throws Exception {
        long p = chamar(post("/peladas/" + peladaId + "/partidas"), config(a, b, 5, 3), 201).get("id").asLong();
        ok(post("/partidas/" + p + "/iniciar"));
        JsonNode empate = ok(post("/partidas/" + p + "/encerrar"));
        assertEquals("EMPATE_PENDENTE", empate.get("status").asText());
        return empate;
    }

    List<Long> emCampo(JsonNode lado) {
        List<Long> ids = new ArrayList<>();
        for (JsonNode j : lado.get("jogadores")) if (j.get("emCampo").asBoolean()) ids.add(j.get("jogadorId").asLong());
        return ids;
    }

    @Test @Order(18)
    void penaltisNormalPreDeterminadoEAlternadosComCiclo() throws Exception {
        LocalDate hoje = LocalDate.now(relogio);
        long pelada = chamar(post("/peladas?grupoId=" + grupoId), Map.of("dataJogo", hoje.toString(), "horaJogo", "11:00"), 201).get("id").asLong();
        // 4 jogadores de linha (goleiros não entram no sorteio, então não servem aqui).
        for (long id : List.of(linha.get(0), linha.get(1), linha.get(2), linha.get(3))) ok(put("/peladas/" + pelada + "/lista/" + id));
        JsonNode sorteada = ok(post("/peladas/" + pelada + "/sorteio"), Map.of("jogadoresPorTime", 2));
        long tA = sorteada.get("times").get(0).get("id").asLong();
        long tB = sorteada.get("times").get(1).get("id").asLong();

        // ---- NORMAL: todos batem → 2 cobranças por time (2 jogadores em campo)
        JsonNode empate = partidaEmpatada(pelada, tA, tB);
        long partida = empate.get("id").asLong();
        List<Long> a = emCampo(empate.get("timeA")), b = emCampo(empate.get("timeB"));
        JsonNode pen = ok(post("/partidas/" + partida + "/penaltis/iniciar"), Map.of("modo", "NORMAL")).get("penaltis");
        assertEquals(2, pen.get("rodadas").asInt());
        assertFalse(pen.get("alternadas").asBoolean());

        ok(post("/partidas/" + partida + "/penaltis"), Map.of("jogadorId", a.get(0), "convertido", true));
        ok(post("/partidas/" + partida + "/penaltis"), Map.of("jogadorId", b.get(0), "convertido", true));
        // o mesmo jogador não repete antes de todo o time bater
        erro(post("/partidas/" + partida + "/penaltis"), Map.of("jogadorId", a.get(0), "convertido", true));
        ok(post("/partidas/" + partida + "/penaltis"), Map.of("jogadorId", a.get(1), "convertido", true));
        pen = ok(post("/partidas/" + partida + "/penaltis"), Map.of("jogadorId", b.get(1), "convertido", true)).get("penaltis");

        // todos bateram e empatou 2x2: segue nas alternadas; o ciclo zera, mas o total continua contando
        assertEquals(2, pen.get("cobrancasA").asInt());
        assertEquals(2, pen.get("cobrancasB").asInt());
        assertTrue(pen.get("alternadas").asBoolean());
        assertEquals(0, pen.get("cicloA").size(), "ciclo zerado depois que o time inteiro bateu");
        assertTrue(pen.get("decididoParaTimeId").isNull());

        ok(post("/partidas/" + partida + "/penaltis"), Map.of("jogadorId", a.get(0), "convertido", true)); // pode bater de novo
        pen = ok(post("/partidas/" + partida + "/penaltis"), Map.of("jogadorId", b.get(0), "convertido", false)).get("penaltis");
        assertEquals(3, pen.get("cobrancasA").asInt());
        assertEquals(tA, pen.get("decididoParaTimeId").asLong());
        JsonNode fim = ok(post("/partidas/" + partida + "/penaltis/encerrar"));
        assertEquals(tA, fim.get("vencedorPenaltisId").asLong());

        // ---- ALTERNADO: direto nas alternadas (uma cobrança de cada por vez)
        empate = partidaEmpatada(pelada, tA, tB);
        partida = empate.get("id").asLong();
        a = emCampo(empate.get("timeA")); b = emCampo(empate.get("timeB"));
        pen = ok(post("/partidas/" + partida + "/penaltis/iniciar"), Map.of("modo", "ALTERNADO")).get("penaltis");
        assertEquals(0, pen.get("rodadas").asInt());
        assertTrue(pen.get("alternadas").asBoolean());
        ok(post("/partidas/" + partida + "/penaltis"), Map.of("jogadorId", a.get(0), "convertido", false));
        pen = ok(post("/partidas/" + partida + "/penaltis"), Map.of("jogadorId", b.get(0), "convertido", true)).get("penaltis");
        assertEquals(tB, pen.get("decididoParaTimeId").asLong(), "quem fica na frente após o par de cobranças vence");
        ok(post("/partidas/" + partida + "/penaltis/encerrar"));

        // ---- PRÉ-DETERMINADO com 1 cobrança: empatou → alternadas
        empate = partidaEmpatada(pelada, tA, tB);
        partida = empate.get("id").asLong();
        a = emCampo(empate.get("timeA")); b = emCampo(empate.get("timeB"));
        ok(post("/partidas/" + partida + "/penaltis/iniciar"), Map.of("modo", "PREDETERMINADO", "quantidade", 1));
        ok(post("/partidas/" + partida + "/penaltis"), Map.of("jogadorId", a.get(0), "convertido", true));
        pen = ok(post("/partidas/" + partida + "/penaltis"), Map.of("jogadorId", b.get(0), "convertido", true)).get("penaltis");
        assertTrue(pen.get("alternadas").asBoolean());
        assertTrue(pen.get("decididoParaTimeId").isNull());
        // sem a partida ser decidida, ela não fecha
        erro(post("/partidas/" + partida + "/penaltis/encerrar"), null);
    }


    @Test @Order(19)
    void gruposSaoIsoladosEntreSi() throws Exception {
        long outro = chamar(post("/grupos"), Map.of("nome", "Outro Grupo"), 201).get("id").asLong();
        assertTrue(ok(get("/grupos")).size() >= 3, "grupo padrão da migration + os dois do teste");

        // elenco, pelada atual e ranking do outro grupo começam vazios, sem enxergar o primeiro
        assertEquals(0, ok(get("/jogadores?grupoId=" + outro)).size());
        assertNull(chamar(get("/peladas/atual?grupoId=" + outro), null, 204));
        assertEquals(0, ok(get("/rankings?grupoId=" + outro + "&periodo=GERAL")).get("peladas").asInt());
        assertTrue(ok(get("/rankings?grupoId=" + grupoId + "&periodo=GERAL")).get("peladas").asInt() > 0);

        // jogador de outro grupo não entra na lista da pelada deste grupo
        long forasteiro = chamar(post("/jogadores?grupoId=" + outro), Map.of("nome", "Forasteiro"), 201).get("id").asLong();
        long peladaAqui = ok(get("/peladas/atual?grupoId=" + grupoId)).get("id").asLong(); // a do step 18 segue aberta
        JsonNode e = erro(put("/peladas/" + peladaAqui + "/lista/" + forasteiro), null);
        assertTrue(e.get("mensagem").asText().contains("outro grupo"), e.toString());

        // cada grupo pode ter a sua própria pelada em aberto
        chamar(post("/peladas?grupoId=" + outro),
                Map.of("dataJogo", LocalDate.now(relogio).toString(), "horaJogo", "13:00"), 201);

        // grupo com jogadores não pode ser excluído; grupo vazio pode
        erro(delete("/grupos/" + outro), null);
        long vazio = chamar(post("/grupos"), Map.of("nome", "Vazio"), 201).get("id").asLong();
        chamar(delete("/grupos/" + vazio), null, 204);
    }


    @Test @Order(20)
    void excluirPeladaFinalizadaExigeSenhaERecalculaOOverall() throws Exception {
        // sem senha, ou com senha errada: nada é apagado (400, não 401, para o app não achar que a sessão caiu)
        erro(post("/peladas/" + peladaId + "/exclusao"), Map.of("senha", ""));
        JsonNode e = erro(post("/peladas/" + peladaId + "/exclusao"), Map.of("senha", "errada"));
        assertTrue(e.get("mensagem").asText().contains("Senha incorreta"), e.toString());
        assertEquals("FINALIZADA", ok(get("/peladas/" + peladaId + "/resumo")).get("status").asText());

        // antes: quem jogou tem overall diferente de 60 por causa dessa pelada
        int mudados = 0;
        for (JsonNode j : ok(get("/jogadores?grupoId=" + grupoId))) if (j.get("overall").asInt() != 60) mudados++;
        assertTrue(mudados > 0);

        chamar(post("/peladas/" + peladaId + "/exclusao"), Map.of("senha", "novaSenha123"), 204);

        // a pelada some, e o overall de quem jogou volta ao que as demais peladas justificam
        chamar(get("/peladas/" + peladaId + "/resumo"), null, 404);
        for (JsonNode j : ok(get("/jogadores?grupoId=" + grupoId)))
            if (j.get("id").asLong() == g1) assertEquals(60, j.get("overall").asInt(), "sem partidas, volta ao overall inicial");
        chamar(post("/peladas/" + peladaId + "/exclusao"), Map.of("senha", "novaSenha123"), 404);
    }


    /** Roda uma pelada curta e completa (1 partida, 1 gol) no grupo e a finaliza; devolve o id. */
    long peladaCompleta(long grupo, List<Long> jogadores) throws Exception {
        long pel = chamar(post("/peladas?grupoId=" + grupo),
                Map.of("dataJogo", LocalDate.now(relogio).toString(), "horaJogo", "14:00"), 201).get("id").asLong();
        for (long id : jogadores) ok(put("/peladas/" + pel + "/lista/" + id));
        JsonNode s = ok(post("/peladas/" + pel + "/sorteio"), Map.of("jogadoresPorTime", 2));
        long a = s.get("times").get(0).get("id").asLong(), b = s.get("times").get(1).get("id").asLong();
        long autor = ids(s.get("times"), 0).get(0);
        long partida = chamar(post("/peladas/" + pel + "/partidas"), config(a, b, 5, 1), 201).get("id").asLong();
        ok(post("/partidas/" + partida + "/iniciar"));
        ok(post("/partidas/" + partida + "/gols"), gol(a, autor, null));
        chamar(post("/peladas/" + pel + "/finalizar"), null, 200);
        return pel;
    }

    @Test @Order(21)
    void excluirPeladaRefazOHistoricoDeOverallDasSeguintes() throws Exception {
        long grupo = chamar(post("/grupos"), Map.of("nome", "Historico"), 201).get("id").asLong();
        List<Long> quatro = new ArrayList<>();
        for (int i = 1; i <= 4; i++)
            quatro.add(chamar(post("/jogadores?grupoId=" + grupo), Map.of("nome", "H" + i), 201).get("id").asLong());

        long primeira = peladaCompleta(grupo, quatro);
        long segunda = peladaCompleta(grupo, quatro);
        // a segunda pelada partiu do overall que a primeira deixou (não do inicial)
        boolean partiuDaPrimeira = false;
        for (JsonNode d : ok(get("/peladas/" + segunda + "/resumo")).get("jogadores"))
            if (d.get("overallAntes").asInt() != 60) partiuDaPrimeira = true;
        assertTrue(partiuDaPrimeira, "a 2ª pelada precisa herdar o overall da 1ª");

        chamar(post("/peladas/" + primeira + "/exclusao"), Map.of("senha", "novaSenha123"), 204);

        // sem a 1ª, a 2ª vira a primeira do histórico: todos partem de 60 e o overall atual é o "depois" dela
        Map<Long, Integer> atual = new java.util.HashMap<>();
        for (JsonNode j : ok(get("/jogadores?grupoId=" + grupo))) atual.put(j.get("id").asLong(), j.get("overall").asInt());
        for (JsonNode d : ok(get("/peladas/" + segunda + "/resumo")).get("jogadores")) {
            assertEquals(60, d.get("overallAntes").asInt(), d.toString());
            assertEquals(atual.get(d.get("jogadorId").asLong()).intValue(), d.get("overallDepois").asInt(), d.toString());
        }
        chamar(get("/peladas/" + primeira + "/resumo"), null, 404);
        assertEquals(1, ok(get("/rankings?grupoId=" + grupo + "&periodo=GERAL")).get("peladas").asInt());
    }

}
