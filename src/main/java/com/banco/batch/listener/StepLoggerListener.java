package com.banco.batch.listener;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;

public class StepLoggerListener {

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        System.out.println("[BATCH] Iniciando Step: " + stepExecution.getStepName());
    }

    @AfterStep
    public void afterStep(StepExecution stepExecution) {
        System.out.println("[BATCH] Finalizando Step: " + stepExecution.getStepName() +
                " - Leidos: " + stepExecution.getReadCount() +
                ", Escritos: " + stepExecution.getWriteCount() +
                ", Saltados: " + stepExecution.getSkipCount() +
                ", Filtrados: " + stepExecution.getFilterCount());
    }
}
