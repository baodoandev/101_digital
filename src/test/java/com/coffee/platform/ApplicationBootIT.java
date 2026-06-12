package com.coffee.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.liquibase.contexts=test")
class ApplicationBootIT {

    static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    static { DB.start(); }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", DB::getJdbcUrl);
        r.add("spring.datasource.username", DB::getUsername);
        r.add("spring.datasource.password", DB::getPassword);
    }

    @Autowired JdbcTemplate jdbc;

    @Test
    void schemaIsCreatedAndSeedIsExcludedInTestContext() {
        Integer tables = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema='public' AND table_name IN
                ('app_user','shop','operator_assignment','menu_item','queue','customer','cust_order','order_item','queue_entry')
                """, Integer.class);
        assertThat(tables).isEqualTo(9);
        Integer shops = jdbc.queryForObject("SELECT count(*) FROM shop", Integer.class);
        assertThat(shops).isZero();
    }
}
