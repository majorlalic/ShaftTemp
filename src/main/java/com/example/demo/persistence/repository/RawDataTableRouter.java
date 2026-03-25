package com.example.demo.persistence.repository;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RawDataTableRouter {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    public String resolveTable(LocalDateTime collectTime) {
        LocalDateTime safeTime = collectTime == null ? LocalDateTime.now() : collectTime;
        return "raw_data_" + YearMonth.from(safeTime).format(MONTH_FORMATTER);
    }

    public List<String> resolveTables(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        YearMonth start = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        List<String> tables = new ArrayList<String>();
        YearMonth cursor = start;
        while (!cursor.isAfter(end)) {
            tables.add("raw_data_" + cursor.format(MONTH_FORMATTER));
            cursor = cursor.plusMonths(1);
        }
        return tables;
    }
}
