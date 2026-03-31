package com.example.demo.persistence.repository;

import java.time.YearMonth;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RawDataTableManager {

    private final JdbcTemplate jdbcTemplate;
    private final Set<String> knownTables = ConcurrentHashMap.newKeySet();

    public RawDataTableManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureTableExists(String tableName) {
        validateTableName(tableName);
        if (knownTables.contains(tableName) && tableExists(tableName)) {
            return;
        }
        knownTables.remove(tableName);
        jdbcTemplate.execute("create table if not exists " + tableName + " like raw_data_template");
        knownTables.add(tableName);
    }

    public List<String> existingTables(List<String> tableNames) {
        List<String> existing = new ArrayList<String>();
        for (String tableName : tableNames) {
            validateTableName(tableName);
            if (knownTables.contains(tableName) || tableExists(tableName)) {
                knownTables.add(tableName);
                existing.add(tableName);
            }
        }
        return existing;
    }

    public List<YearMonth> listExistingMonths() {
        List<String> tableNames = jdbcTemplate.queryForList(
            "select table_name from information_schema.tables where table_schema = database() and table_name like 'raw_data\\_%'",
            String.class
        );
        if (tableNames == null || tableNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<YearMonth> months = new ArrayList<YearMonth>();
        for (String tableName : tableNames) {
            if (!tableName.matches("raw_data_\\d{6}")) {
                continue;
            }
            months.add(YearMonth.of(
                Integer.parseInt(tableName.substring(9, 13)),
                Integer.parseInt(tableName.substring(13, 15))
            ));
        }
        return months;
    }

    public void dropMonthTable(YearMonth month) {
        String tableName = "raw_data_" + month.toString().replace("-", "");
        validateTableName(tableName);
        jdbcTemplate.execute("drop table if exists " + tableName);
        knownTables.remove(tableName);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "select count(*) from information_schema.tables where table_schema = database() and table_name = ?",
            Integer.class,
            tableName
        );
        return count != null && count.intValue() > 0;
    }

    private void validateTableName(String tableName) {
        if (tableName == null || !tableName.matches("raw_data_\\d{6}")) {
            throw new IllegalArgumentException("Unsupported raw data table: " + tableName);
        }
    }
}
