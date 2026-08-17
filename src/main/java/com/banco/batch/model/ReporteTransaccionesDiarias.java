package com.banco.batch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteTransaccionesDiarias {

    private Long id;
    private String semana;
    private LocalDate fecha;
    private int totalDebitos;
    private int totalCreditos;
    private BigDecimal totalMonto;
    private int cantidadAnomalias;

    public ReporteTransaccionesDiarias(String semana, LocalDate fecha, int totalDebitos,
                                         int totalCreditos, BigDecimal totalMonto, int cantidadAnomalias) {
        this.semana = semana;
        this.fecha = fecha;
        this.totalDebitos = totalDebitos;
        this.totalCreditos = totalCreditos;
        this.totalMonto = totalMonto;
        this.cantidadAnomalias = cantidadAnomalias;
    }
}
