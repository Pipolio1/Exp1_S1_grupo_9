package com.banco.batch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionProcesada {

    private Long id;
    private String semana;
    private LocalDate fecha;
    private BigDecimal monto;
    private String tipo;
    private boolean anomalia;
    private String motivoAnomalia;

    public TransaccionProcesada(String semana, LocalDate fecha, BigDecimal monto, String tipo,
                                boolean anomalia, String motivoAnomalia) {
        this.semana = semana;
        this.fecha = fecha;
        this.monto = monto;
        this.tipo = tipo;
        this.anomalia = anomalia;
        this.motivoAnomalia = motivoAnomalia;
    }
}
