package com.banco.batch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuentaAnualProcesada {

    private Long id;
    private String semana;
    private int cuentaId;
    private LocalDate fecha;
    private String transaccion;
    private BigDecimal monto;
    private String descripcion;
    private boolean anomalia;
    private String motivoAnomalia;

    public CuentaAnualProcesada(String semana, int cuentaId, LocalDate fecha, String transaccion,
                              BigDecimal monto, String descripcion, boolean anomalia, String motivoAnomalia) {
        this.semana = semana;
        this.cuentaId = cuentaId;
        this.fecha = fecha;
        this.transaccion = transaccion;
        this.monto = monto;
        this.descripcion = descripcion;
        this.anomalia = anomalia;
        this.motivoAnomalia = motivoAnomalia;
    }
}
