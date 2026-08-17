package com.banco.batch.config;

import com.banco.batch.config.mapper.CuentaAnualFieldSetMapper;
import com.banco.batch.listener.BankSkipPolicy;
import com.banco.batch.listener.JobLoggerListener;
import com.banco.batch.listener.StepLoggerListener;
import com.banco.batch.model.CuentaAnualCsv;
import com.banco.batch.model.CuentaAnualProcesada;
import com.banco.batch.model.EstadoCuentaAnual;
import com.banco.batch.processor.CuentasAnualesProcessor;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class CuentasAnualesJobConfig {

    @Bean
    public Job generacionEstadosCuentaAnualesJob(JobRepository jobRepository,
                                                 Step cuentasAnualesStep,
                                                 Step resumenEstadosCuentaStep) {
        return new JobBuilder("generacionEstadosCuentaAnualesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(new JobLoggerListener())
                .start(cuentasAnualesStep)
                .next(resumenEstadosCuentaStep)
                .build();
    }

    @Bean
    public Step cuentasAnualesStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  FlatFileItemReader<CuentaAnualCsv> cuentasAnualesReader,
                                  CuentasAnualesProcessor cuentasAnualesProcessor,
                                  JdbcBatchItemWriter<CuentaAnualProcesada> cuentasAnualesWriter) {
        return new StepBuilder("cuentasAnualesStep", jobRepository)
                .<CuentaAnualCsv, CuentaAnualProcesada>chunk(100, transactionManager)
                .reader(cuentasAnualesReader)
                .processor(cuentasAnualesProcessor)
                .writer(cuentasAnualesWriter)
                .listener(new StepLoggerListener())
                .faultTolerant()
                .skipPolicy(new BankSkipPolicy())
                .build();
    }

    @Bean
    public Step resumenEstadosCuentaStep(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return new StepBuilder("resumenEstadosCuentaStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String semana = chunkContext.getStepContext().getStepExecution().getJobExecution().getJobParameters().getString("semana");
                    List<CuentaAnualProcesada> movimientos = jdbcTemplate.query(
                            "SELECT cuenta_id, fecha, transaccion, monto FROM cuentas_anuales_procesadas WHERE semana = ?",
                            (rs, rowNum) -> new CuentaAnualProcesada(
                                    semana,
                                    rs.getInt("cuenta_id"),
                                    rs.getDate("fecha").toLocalDate(),
                                    rs.getString("transaccion"),
                                    rs.getBigDecimal("monto"),
                                    null,
                                    false,
                                    null
                            ),
                            semana);

                    Map<String, EstadoCuentaAnual> acumuladores = new HashMap<>();
                    for (CuentaAnualProcesada m : movimientos) {
                        if (m.getFecha() == null) continue;
                        String key = m.getCuentaId() + "-" + m.getFecha().getYear();
                        EstadoCuentaAnual estado = acumuladores.computeIfAbsent(key, k ->
                                new EstadoCuentaAnual(semana, m.getCuentaId(), m.getFecha().getYear(),
                                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

                        String transaccion = m.getTransaccion();
                        BigDecimal monto = m.getMonto() == null ? BigDecimal.ZERO : m.getMonto();

                        switch (transaccion) {
                            case "deposito" -> estado.setTotalDepositos(estado.getTotalDepositos().add(monto));
                            case "retiro" -> estado.setTotalRetiros(estado.getTotalRetiros().add(monto));
                            case "compra" -> estado.setTotalCompras(estado.getTotalCompras().add(monto));
                            case "pago" -> estado.setTotalPagos(estado.getTotalPagos().add(monto));
                            default -> {
                                // no se reconoce, se ignora en el resumen
                            }
                        }
                    }

                    for (EstadoCuentaAnual estado : acumuladores.values()) {
                        BigDecimal saldo = estado.getTotalDepositos()
                                .add(estado.getTotalPagos())
                                .subtract(estado.getTotalRetiros())
                                .subtract(estado.getTotalCompras());
                        estado.setSaldoAnual(saldo);

                        jdbcTemplate.update(
                                "MERGE INTO estados_cuenta_anuales (semana, cuenta_id, anio, total_depositos, total_retiros, total_compras, total_pagos, saldo_anual) " +
                                "KEY (semana, cuenta_id, anio) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                estado.getSemana(), estado.getCuentaId(), estado.getAnio(),
                                estado.getTotalDepositos(), estado.getTotalRetiros(),
                                estado.getTotalCompras(), estado.getTotalPagos(), estado.getSaldoAnual());
                    }
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<CuentaAnualCsv> cuentasAnualesReader(
            @Value("#{jobParameters['input.file']}") Resource resource) {
        DefaultLineMapper<CuentaAnualCsv> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("cuenta_id", "fecha", "transaccion", "monto", "descripcion");
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(new CuentaAnualFieldSetMapper());

        return new FlatFileItemReaderBuilder<CuentaAnualCsv>()
                .name("cuentasAnualesReader")
                .resource(resource)
                .lineMapper(lineMapper)
                .linesToSkip(1)
                .build();
    }

    @Bean
    @StepScope
    public CuentasAnualesProcessor cuentasAnualesProcessor(
            @Value("#{jobParameters['semana']}") String semana) {
        return new CuentasAnualesProcessor(semana);
    }

    @Bean
    public JdbcBatchItemWriter<CuentaAnualProcesada> cuentasAnualesWriter(DataSource dataSource) {
        JdbcBatchItemWriter<CuentaAnualProcesada> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.setSql("INSERT INTO cuentas_anuales_procesadas (semana, cuenta_id, fecha, transaccion, monto, descripcion, anomalia, motivo_anomalia) " +
                "VALUES (:semana, :cuentaId, :fecha, :transaccion, :monto, :descripcion, :anomalia, :motivoAnomalia)");
        return writer;
    }
}
