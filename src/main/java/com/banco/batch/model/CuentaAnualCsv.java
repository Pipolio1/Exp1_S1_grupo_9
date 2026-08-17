package com.banco.batch.model;

public record CuentaAnualCsv(
        String cuentaId,
        String fecha,
        String transaccion,
        String monto,
        String descripcion
) {
}
