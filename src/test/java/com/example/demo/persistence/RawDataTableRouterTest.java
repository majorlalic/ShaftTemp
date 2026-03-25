package com.example.demo.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.example.demo.persistence.repository.RawDataTableRouter;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RawDataTableRouterTest {

    private final RawDataTableRouter router = new RawDataTableRouter();

    @Test
    void shouldResolveMonthTable() {
        assertEquals("raw_data_202603", router.resolveTable(LocalDateTime.of(2026, 3, 25, 10, 0, 0)));
    }

    @Test
    void shouldResolveCrossMonthTables() {
        assertIterableEquals(
            Arrays.asList("raw_data_202603", "raw_data_202604", "raw_data_202605"),
            router.resolveTables(
                LocalDateTime.of(2026, 3, 31, 23, 59, 59),
                LocalDateTime.of(2026, 5, 1, 0, 0, 0)
            )
        );
    }
}
