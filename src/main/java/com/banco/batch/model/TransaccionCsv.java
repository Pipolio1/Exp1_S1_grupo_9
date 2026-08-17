package com.banco.batch.model;

public record TransaccionCsv(
        String id,
        String fecha,
        String monto,
        String tipo
) {
}
