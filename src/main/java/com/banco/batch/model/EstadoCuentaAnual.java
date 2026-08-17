package com.banco.batch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoCuentaAnual {

    private Long id;
    private String semana;
    private int cuentaId;
    private int anio;
    private BigDecimal totalDepositos;
    private BigDecimal totalRetiros;
    private BigDecimal totalCompras;
    private BigDecimal totalPagos;
    private BigDecimal saldoAnual;

    public EstadoCuentaAnual(String semana, int cuentaId, int anio, BigDecimal totalDepositos,
                             BigDecimal totalRetiros, BigDecimal totalCompras, BigDecimal totalPagos,
                             BigDecimal saldoAnual) {
        this.semana = semana;
        this.cuentaId = cuentaId;
        this.anio = anio;
        this.totalDepositos = totalDepositos;
        this.totalRetiros = totalRetiros;
        this.totalCompras = totalCompras;
        this.totalPagos = totalPagos;
        this.saldoAnual = saldoAnual;
    }
}
