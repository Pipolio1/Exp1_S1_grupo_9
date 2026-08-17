package com.banco.batch.processor;

import com.banco.batch.model.CuentaInteres;
import com.banco.batch.model.InteresCsv;
import com.banco.batch.util.LegacyDataUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

public class InteresesProcessor implements org.springframework.batch.item.ItemProcessor<InteresCsv, CuentaInteres> {

    private final String semana;
    private final Set<String> procesados;

    private static final BigDecimal TASA_AHORRO = new BigDecimal("0.02");
    private static final BigDecimal TASA_PRESTAMO = new BigDecimal("0.05");

    public InteresesProcessor(String semana, Set<String> procesados) {
        this.semana = semana;
        this.procesados = procesados;
    }

    @Override
    public CuentaInteres process(InteresCsv item) {
        StringBuilder motivo = new StringBuilder();
        boolean aceptado = true;

        int cuentaId;
        try {
            cuentaId = LegacyDataUtils.parseInt(item.cuentaId());
        } catch (Exception e) {
            cuentaId = -1;
            aceptado = false;
            motivo.append("ID de cuenta invalido; ");
        }

        String nombre = item.nombre() == null || item.nombre().isBlank() ? "DESCONOCIDO" : item.nombre().trim();

        BigDecimal saldo;
        try {
            saldo = LegacyDataUtils.parseDecimal(item.saldo());
        } catch (Exception e) {
            saldo = BigDecimal.ZERO;
            aceptado = false;
            motivo.append("Saldo invalido; ");
        }

        int edad;
        try {
            edad = LegacyDataUtils.parseInt(item.edad());
            if (edad < 18 || edad > 100) {
                aceptado = false;
                motivo.append("Edad fuera de rango: ").append(edad).append("; ");
            }
        } catch (Exception e) {
            edad = 0;
            aceptado = false;
            motivo.append("Edad invalida; ");
        }

        String tipoNormalizado = LegacyDataUtils.normalizeType(item.tipo());
        String tipoFinal;
        BigDecimal tasa;
        switch (tipoNormalizado) {
            case "ahorro" -> {
                tipoFinal = "AHORRO";
                tasa = TASA_AHORRO;
            }
            case "prestamo" -> {
                tipoFinal = "PRESTAMO";
                tasa = TASA_PRESTAMO;
            }
            default -> {
                tipoFinal = tipoNormalizado.toUpperCase();
                tasa = BigDecimal.ZERO;
                aceptado = false;
                motivo.append("Tipo de cuenta no valido: ").append(item.tipo()).append("; ");
            }
        }

        String key = String.join("|", semana, String.valueOf(cuentaId), nombre,
                String.valueOf(saldo), String.valueOf(edad), tipoFinal);
        if (procesados.contains(key)) {
            aceptado = false;
            motivo.append("Registro duplicado; ");
        } else {
            procesados.add(key);
        }

        BigDecimal saldoConInteres = saldo.multiply(BigDecimal.ONE.add(tasa))
                .setScale(2, RoundingMode.HALF_UP);

        return new CuentaInteres(
                semana,
                cuentaId,
                nombre,
                saldo,
                edad,
                tipoFinal,
                tasa,
                saldoConInteres,
                aceptado,
                motivo.toString().trim()
        );
    }
}
