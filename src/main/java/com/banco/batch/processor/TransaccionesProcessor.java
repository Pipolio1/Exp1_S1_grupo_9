package com.banco.batch.processor;

import com.banco.batch.model.TransaccionCsv;
import com.banco.batch.model.TransaccionProcesada;
import com.banco.batch.util.LegacyDataUtils;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransaccionesProcessor implements ItemProcessor<TransaccionCsv, TransaccionProcesada> {

    private final String semana;

    public TransaccionesProcessor(String semana) {
        this.semana = semana;
    }

    @Override
    public TransaccionProcesada process(TransaccionCsv item) {
        boolean anomalia = false;
        StringBuilder motivo = new StringBuilder();

        LocalDate fecha;
        try {
            fecha = LegacyDataUtils.parseDate(item.fecha());
        } catch (Exception e) {
            fecha = null;
            anomalia = true;
            motivo.append("Fecha invalida: ").append(item.fecha()).append("; ");
        }

        BigDecimal monto;
        try {
            monto = LegacyDataUtils.parseDecimal(item.monto());
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                anomalia = true;
                motivo.append("Monto negativo o cero: ").append(item.monto()).append("; ");
            }
        } catch (Exception e) {
            monto = BigDecimal.ZERO;
            anomalia = true;
            motivo.append("Monto invalido: ").append(item.monto()).append("; ");
        }

        String tipoNormalizado = LegacyDataUtils.normalizeType(item.tipo());
        String tipoFinal;
        try {
            switch (tipoNormalizado) {
                case "debito" -> tipoFinal = "DEBITO";
                case "credito" -> tipoFinal = "CREDITO";
                default -> {
                    tipoFinal = tipoNormalizado.toUpperCase();
                    anomalia = true;
                    motivo.append("Tipo no valido: ").append(item.tipo()).append("; ");
                }
            }
        } catch (Exception e) {
            tipoFinal = "DESCONOCIDO";
            anomalia = true;
            motivo.append("Tipo invalido: ").append(item.tipo()).append("; ");
        }

        return new TransaccionProcesada(
                semana,
                fecha,
                monto,
                tipoFinal,
                anomalia,
                motivo.toString().trim()
        );
    }
}
