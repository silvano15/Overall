package br.com.overall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Relógio único da aplicação. Decide "hoje" (liberação do sorteio) no fuso do ADM e permite
 * substituir o relógio nos testes.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${overall.timezone:America/Recife}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
