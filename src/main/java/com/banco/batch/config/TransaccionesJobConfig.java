package com.banco.batch.config;

import com.banco.batch.config.mapper.TransaccionFieldSetMapper;
import com.banco.batch.listener.BankSkipPolicy;
import com.banco.batch.listener.JobLoggerListener;
import com.banco.batch.listener.StepLoggerListener;
import com.banco.batch.model.ReporteTransaccionesDiarias;
import com.banco.batch.model.TransaccionCsv;
import com.banco.batch.model.TransaccionProcesada;
import com.banco.batch.processor.TransaccionesProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class TransaccionesJobConfig {

    @Bean
    public Job reporteTransaccionesDiariasJob(JobRepository jobRepository,
                                              Step transaccionesStep,
                                              Step resumenTransaccionesStep) {
        return new JobBuilder("reporteTransaccionesDiariasJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(new JobLoggerListener())
                .start(transaccionesStep)
                .next(resumenTransaccionesStep)
                .build();
    }

    @Bean
    public Step transaccionesStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  FlatFileItemReader<TransaccionCsv> transaccionesReader,
                                  TransaccionesProcessor transaccionesProcessor,
                                  JdbcBatchItemWriter<TransaccionProcesada> transaccionesWriter) {
        return new StepBuilder("transaccionesStep", jobRepository)
                .<TransaccionCsv, TransaccionProcesada>chunk(100, transactionManager)
                .reader(transaccionesReader)
                .processor(transaccionesProcessor)
                .writer(transaccionesWriter)
                .listener(new StepLoggerListener())
                .faultTolerant()
                .skipPolicy(new BankSkipPolicy())
                .build();
    }

    @Bean
    public Step resumenTransaccionesStep(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return new StepBuilder("resumenTransaccionesStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String semana = chunkContext.getStepContext().getStepExecution().getJobExecution().getJobParameters().getString("semana");
                    List<TransaccionProcesada> transacciones = jdbcTemplate.query(
                            "SELECT fecha, tipo, monto, anomalia FROM transacciones_procesadas WHERE semana = ?",
                            (rs, rowNum) -> new TransaccionProcesada(
                                    semana,
                                    rs.getDate("fecha").toLocalDate(),
                                    rs.getBigDecimal("monto"),
                                    rs.getString("tipo"),
                                    rs.getBoolean("anomalia"),
                                    null
                            ),
                            semana);

                    Map<LocalDate, ReporteTransaccionesDiarias> acumuladores = new HashMap<>();
                    for (TransaccionProcesada t : transacciones) {
                        LocalDate fecha = t.getFecha();
                        if (fecha == null) continue;

                        ReporteTransaccionesDiarias reporte = acumuladores.computeIfAbsent(fecha,
                                f -> new ReporteTransaccionesDiarias(semana, f, 0, 0, BigDecimal.ZERO, 0));

                        reporte.setTotalMonto(reporte.getTotalMonto().add(t.getMonto()));
                        if (t.isAnomalia()) {
                            reporte.setCantidadAnomalias(reporte.getCantidadAnomalias() + 1);
                        } else if ("DEBITO".equals(t.getTipo())) {
                            reporte.setTotalDebitos(reporte.getTotalDebitos() + 1);
                        } else if ("CREDITO".equals(t.getTipo())) {
                            reporte.setTotalCreditos(reporte.getTotalCreditos() + 1);
                        }
                    }

                    for (ReporteTransaccionesDiarias r : acumuladores.values()) {
                        jdbcTemplate.update(
                                "MERGE INTO reporte_transacciones_diarias (semana, fecha, total_debitos, total_creditos, total_monto, cantidad_anomalias) " +
                                "KEY (semana, fecha) VALUES (?, ?, ?, ?, ?, ?)",
                                r.getSemana(), Date.valueOf(r.getFecha()),
                                r.getTotalDebitos(), r.getTotalCreditos(),
                                r.getTotalMonto(), r.getCantidadAnomalias());
                    }
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<TransaccionCsv> transaccionesReader(
            @Value("#{jobParameters['input.file']}") Resource resource) {
        DefaultLineMapper<TransaccionCsv> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id", "fecha", "monto", "tipo");
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(new TransaccionFieldSetMapper());

        return new FlatFileItemReaderBuilder<TransaccionCsv>()
                .name("transaccionesReader")
                .resource(resource)
                .lineMapper(lineMapper)
                .linesToSkip(1)
                .build();
    }

    @Bean
    @StepScope
    public TransaccionesProcessor transaccionesProcessor(
            @Value("#{jobParameters['semana']}") String semana) {
        return new TransaccionesProcessor(semana);
    }

    @Bean
    public JdbcBatchItemWriter<TransaccionProcesada> transaccionesWriter(DataSource dataSource) {
        JdbcBatchItemWriter<TransaccionProcesada> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.setSql("INSERT INTO transacciones_procesadas (semana, fecha, monto, tipo, anomalia, motivo_anomalia) " +
                "VALUES (:semana, :fecha, :monto, :tipo, :anomalia, :motivoAnomalia)");
        return writer;
    }
}
