package com.banco.batch.processor;

import com.banco.batch.model.CuentaAnualCsv;
import com.banco.batch.model.CuentaAnualProcesada;
import com.banco.batch.util.LegacyDataUtils;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CuentasAnualesProcessor implements ItemProcessor<CuentaAnualCsv, CuentaAnualProcesada> {

    private final String semana;

    public CuentasAnualesProcessor(String semana) {
        this.semana = semana;
    }

    @Override
    public CuentaAnualProcesada process(CuentaAnualCsv item) {
        boolean anomalia = false;
        StringBuilder motivo = new StringBuilder();

        int cuentaId;
        try {
            cuentaId = LegacyDataUtils.parseInt(item.cuentaId());
        } catch (Exception e) {
            cuentaId = -1;
            anomalia = true;
            motivo.append("ID de cuenta invalido; ");
        }

        LocalDate fecha;
        try {
            fecha = LegacyDataUtils.parseDate(item.fecha());
        } catch (Exception e) {
            fecha = null;
            anomalia = true;
            motivo.append("Fecha invalida: ").append(item.fecha()).append("; ");
        }

        String transaccionNormalizada = LegacyDataUtils.normalizeType(item.transaccion());
        if (transaccionNormalizada.isBlank()) {
            transaccionNormalizada = "desconocida";
            anomalia = true;
            motivo.append("Transaccion vacia; ");
        }

        BigDecimal monto;
        try {
            monto = LegacyDataUtils.parseDecimal(item.monto());
        } catch (Exception e) {
            monto = BigDecimal.ZERO;
            anomalia = true;
            motivo.append("Monto invalido: ").append(item.monto()).append("; ");
        }

        String descripcion = item.descripcion() == null ? "" : item.descripcion().trim();
        if (descripcion.isBlank()) {
            anomalia = true;
            motivo.append("Descripcion faltante; ");
        }

        return new CuentaAnualProcesada(
                semana,
                cuentaId,
                fecha,
                transaccionNormalizada,
                monto,
                descripcion,
                anomalia,
                motivo.toString().trim()
        );
    }
}
