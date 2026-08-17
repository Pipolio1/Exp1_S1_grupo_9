package com.banco.batch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuentaInteres {

    private Long id;
    private String semana;
    private int cuentaId;
    private String nombre;
    private BigDecimal saldoOriginal;
    private int edad;
    private String tipo;
    private BigDecimal tasaInteres;
    private BigDecimal saldoConInteres;
    private boolean aceptado;
    private String motivoRechazo;

    public CuentaInteres(String semana, int cuentaId, String nombre, BigDecimal saldoOriginal,
                         int edad, String tipo, BigDecimal tasaInteres, BigDecimal saldoConInteres,
                         boolean aceptado, String motivoRechazo) {
        this.semana = semana;
        this.cuentaId = cuentaId;
        this.nombre = nombre;
        this.saldoOriginal = saldoOriginal;
        this.edad = edad;
        this.tipo = tipo;
        this.tasaInteres = tasaInteres;
        this.saldoConInteres = saldoConInteres;
        this.aceptado = aceptado;
        this.motivoRechazo = motivoRechazo;
    }
}
