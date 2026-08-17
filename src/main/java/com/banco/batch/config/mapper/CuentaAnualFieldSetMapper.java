package com.banco.batch.config.mapper;

import com.banco.batch.model.CuentaAnualCsv;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

public class CuentaAnualFieldSetMapper implements FieldSetMapper<CuentaAnualCsv> {

    @Override
    public CuentaAnualCsv mapFieldSet(FieldSet fieldSet) {
        return new CuentaAnualCsv(
                fieldSet.readString("cuenta_id"),
                fieldSet.readString("fecha"),
                fieldSet.readString("transaccion"),
                fieldSet.readString("monto"),
                fieldSet.readString("descripcion")
        );
    }
}
