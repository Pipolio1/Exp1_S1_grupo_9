package com.banco.batch.config.mapper;

import com.banco.batch.model.InteresCsv;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

public class InteresFieldSetMapper implements FieldSetMapper<InteresCsv> {

    @Override
    public InteresCsv mapFieldSet(FieldSet fieldSet) {
        return new InteresCsv(
                fieldSet.readString("cuenta_id"),
                fieldSet.readString("nombre"),
                fieldSet.readString("saldo"),
                fieldSet.readString("edad"),
                fieldSet.readString("tipo")
        );
    }
}
