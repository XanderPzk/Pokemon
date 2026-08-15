package com.alex.pokedex.pokemon.pokeapi;

import com.alex.pokedex.config.PokeApiConfig;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * Minimal application for PokeAPI integration tests.
 *
 * <p>Scoped to the {@code pokeapi-test} profile so this configuration is not component-scanned into
 * the full application context during {@code PokedexApplicationTests}.
 */
@SpringBootApplication(
        scanBasePackages = "com.alex.pokedex.pokemon",
        exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            ManagementWebSecurityAutoConfiguration.class
        })
@Import(PokeApiConfig.class)
@EnableCaching(order = 0)
@Profile("pokeapi-test")
public class PokeApiTestApplication {}
