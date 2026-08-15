package com.alex.pokedex;

import static org.assertj.core.api.Assertions.assertThat;

import com.alex.pokedex.support.PostgresTestSupport;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("com.alex.pokedex.support.PostgresTestSupport#isAvailable")
class PostgresConnectivityTest {

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        PostgresTestSupport.register(registry, "it_postgres_connectivity");
    }

    @Autowired private DataSource dataSource;

    @Test
    void flywayMigrationRunsAgainstPostgres() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM flyway_schema_history", Integer.class);

        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testProfileDoesNotSeedDemoUser() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Integer userCount =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);

        assertThat(userCount).isZero();
    }
}
