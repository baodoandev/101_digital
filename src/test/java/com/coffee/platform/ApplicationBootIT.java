package com.coffee.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationBootIT extends AbstractIT {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void schemaIsCreatedAndSeedIsExcludedInTestContext() {
        Integer tables = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema='public' AND table_name IN
                ('app_user','shop','operator_assignment','menu_item','queue','customer','cust_order','order_item','queue_entry')
                """, Integer.class);
        assertThat(tables).isEqualTo(9);
        Integer seedShops = jdbc.queryForObject(
                "SELECT count(*) FROM shop WHERE id IN (1,2) AND name LIKE 'Chain Coffee%'", Integer.class);
        assertThat(seedShops).isZero();
    }
}
