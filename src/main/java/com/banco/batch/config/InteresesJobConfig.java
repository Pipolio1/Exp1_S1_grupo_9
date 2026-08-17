package com.banco.batch.config;

import com.banco.batch.config.mapper.InteresFieldSetMapper;
import com.banco.batch.listener.BankSkipPolicy;
import com.banco.batch.listener.JobLoggerListener;
import com.banco.batch.listener.StepLoggerListener;
import com.banco.batch.model.CuentaInteres;
import com.banco.batch.model.InteresCsv;
import com.banco.batch.processor.InteresesProcessor;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class InteresesJobConfig {

    @Bean
    public Job calculoInteresesMensualesJob(JobRepository jobRepository,
                                            Step interesesStep) {
        return new JobBuilder("calculoInteresesMensualesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(new JobLoggerListener())
                .start(interesesStep)
                .build();
    }

    @Bean
    public Step interesesStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              FlatFileItemReader<InteresCsv> interesesReader,
                              InteresesProcessor interesesProcessor,
                              JdbcBatchItemWriter<CuentaInteres> interesesWriter) {
        return new StepBuilder("interesesStep", jobRepository)
                .<InteresCsv, CuentaInteres>chunk(100, transactionManager)
                .reader(interesesReader)
                .processor(interesesProcessor)
                .writer(interesesWriter)
                .listener(new StepLoggerListener())
                .faultTolerant()
                .skipPolicy(new BankSkipPolicy())
                .build();
    }

    @Bean
    public Set<String> interesesProcesados() {
        return Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    @Bean
    @StepScope
    public FlatFileItemReader<InteresCsv> interesesReader(
            @Value("#{jobParameters['input.file']}") Resource resource) {
        DefaultLineMapper<InteresCsv> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("cuenta_id", "nombre", "saldo", "edad", "tipo");
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(new InteresFieldSetMapper());

        return new FlatFileItemReaderBuilder<InteresCsv>()
                .name("interesesReader")
                .resource(resource)
                .lineMapper(lineMapper)
                .linesToSkip(1)
                .build();
    }

    @Bean
    @StepScope
    public InteresesProcessor interesesProcessor(
            @Value("#{jobParameters['semana']}") String semana,
            Set<String> interesesProcesados) {
        interesesProcesados.clear();
        return new InteresesProcessor(semana, interesesProcesados);
    }

    @Bean
    public JdbcBatchItemWriter<CuentaInteres> interesesWriter(DataSource dataSource) {
        JdbcBatchItemWriter<CuentaInteres> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.setSql("INSERT INTO cuentas_intereses (semana, cuenta_id, nombre, saldo_original, edad, tipo, tasa_interes, saldo_con_interes, aceptado, motivo_rechazo) " +
                "VALUES (:semana, :cuentaId, :nombre, :saldoOriginal, :edad, :tipo, :tasaInteres, :saldoConInteres, :aceptado, :motivoRechazo)");
        return writer;
    }
}
