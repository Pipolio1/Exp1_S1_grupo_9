package com.banco.batch.listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;

public class JobLoggerListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        System.out.println("[BATCH] Iniciando Job: " + jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        System.out.println("[BATCH] Finalizando Job: " + jobExecution.getJobInstance().getJobName() +
                " con estado " + jobExecution.getStatus());
        for (StepExecution step : jobExecution.getStepExecutions()) {
            System.out.println("[BATCH]   Step " + step.getStepName() +
                    " - Leidos: " + step.getReadCount() +
                    ", Escritos: " + step.getWriteCount() +
                    ", Saltados: " + step.getSkipCount() +
                    ", Filtrados: " + step.getFilterCount());
        }
    }
}
