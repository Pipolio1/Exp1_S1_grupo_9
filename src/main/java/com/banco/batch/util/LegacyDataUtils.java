package com.banco.batch.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class LegacyDataUtils {

    private LegacyDataUtils() {
    }

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    public static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Fecha vacia");
        }
        String normalized = value.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // probar siguiente formato
            }
        }
        throw new IllegalArgumentException("Formato de fecha no valido: " + value);
    }

    public static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Monto vacio");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Monto no numerico: " + value);
        }
    }

    public static int parseInt(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Valor entero vacio");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor entero no valido: " + value);
        }
    }

    public static String normalizeType(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase().replace('ó', 'o').replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ú', 'u');
    }
}
