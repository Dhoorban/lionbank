package com.lionbank.config;
import com.lionbank.dao.TransactionRecord;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;


import javax.sql.DataSource;

@Configuration
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public BatchConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public FlatFileItemReader<TransactionRecord> reader() {
        FlatFileItemReader<TransactionRecord> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("dataSource.txt"));
        reader.setLinesToSkip(1); // Skip header row
        reader.setLineMapper(new DefaultLineMapper<TransactionRecord>() {{
            setLineTokenizer(new DelimitedLineTokenizer("|") {{ // Explicitly set delimiter
                setNames("accountNumber", "transactionAmount", "description", "transactionDate", "transactionTime", "customerId");
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<TransactionRecord>() {{
                setTargetType(TransactionRecord.class);
            }});
        }});
        return reader;
    }

    @Bean
    public JdbcBatchItemWriter<TransactionRecord> writer(DataSource dataSource) {
        JdbcBatchItemWriter<TransactionRecord> writer = new JdbcBatchItemWriter<>();
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.setSql("""
            INSERT INTO transactions (account_number, transaction_amount, description, transaction_date, transaction_time, customer_id) 
            VALUES (:accountNumber, :transactionAmount, :description, :transactionDate, :transactionTime, :customerId)
        """);
        writer.setDataSource(dataSource);
        return writer;
    }

    @Bean
    public Step step1(FlatFileItemReader<TransactionRecord> reader, JdbcBatchItemWriter<TransactionRecord> writer) {
        return new StepBuilder("step1", jobRepository)
                .<TransactionRecord, TransactionRecord>chunk(10, transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    public Job importTransactionJob(Step step1) {
        return new JobBuilder("importTransactionJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step1)
                .build();
    }
}
