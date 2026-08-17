package com.banco.batch.config.mapper;

import com.banco.batch.model.TransaccionCsv;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

public class TransaccionFieldSetMapper implements FieldSetMapper<TransaccionCsv> {

    @Override
    public TransaccionCsv mapFieldSet(FieldSet fieldSet) {
        return new TransaccionCsv(
                fieldSet.readString("id"),
                fieldSet.readString("fecha"),
                fieldSet.readString("monto"),
                fieldSet.readString("tipo")
        );
    }
}
