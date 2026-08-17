package com.banco.batch.launcher;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
public class BatchJobLauncher implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    private final Map<String, String> jobFiles = Map.of(
            "reporteTransaccionesDiariasJob", "transacciones.csv",
            "calculoInteresesMensualesJob", "intereses.csv",
            "generacionEstadosCuentaAnualesJob", "cuentas_anuales.csv"
    );

    public BatchJobLauncher(JobLauncher jobLauncher, JobRegistry jobRegistry) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
    }

    @Override
    public void run(String... args) throws Exception {
        String jobName = extractArg(args, "job.name");
        String semana = extractArg(args, "semana");

        if (jobName == null) {
            System.out.println("[BATCH] Debe indicar el Job a ejecutar con --job.name=<nombre>");
            System.out.println("[BATCH] Jobs disponibles:");
            for (String name : jobRegistry.getJobNames()) {
                System.out.println("  - " + name);
            }
            System.out.println("[BATCH] Ejemplo: --job.name=reporteTransaccionesDiariasJob --semana=semana_1");
            return;
        }

        if (semana == null) {
            System.out.println("[BATCH] Debe indicar la semana con --semana=<semana_1|semana_2|semana_3>");
            return;
        }

        String fileName = jobFiles.get(jobName);
        if (fileName == null) {
            System.out.println("[BATCH] Job no reconocido: " + jobName);
            return;
        }

        String inputFile = "classpath:/data/" + semana + "/" + fileName;

        Job job = jobRegistry.getJob(jobName);
        JobParameters parameters = new JobParametersBuilder()
                .addString("input.file", inputFile)
                .addString("semana", semana)
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(job, parameters);
        System.out.println("[BATCH] Job " + jobName + " finalizo con estado " + execution.getStatus());
    }

    private String extractArg(String[] args, String key) {
        return Arrays.stream(args)
                .filter(arg -> arg.startsWith("--" + key + "="))
                .map(arg -> arg.substring(("--" + key + "=").length()))
                .findFirst()
                .orElse(null);
    }
}
