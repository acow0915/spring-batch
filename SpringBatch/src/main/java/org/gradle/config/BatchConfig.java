package org.gradle.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.gradle.entity.DataPartitioner;
import org.gradle.entity.TestData;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.orm.JpaNativeQueryProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	
	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	private DataSource dataSource;
	
	@Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean lef = new LocalContainerEntityManagerFactoryBean();
        lef.setPackagesToScan("org.gradle.entity");
        lef.setDataSource(dataSource);
        lef.setJpaVendorAdapter(jpaVendorAdapter());
        lef.setJpaProperties(new Properties());
        return lef;
    }
	
	@Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory); // 构建事务管理器
    }
	
	@Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setDatabase(Database.H2);
        jpaVendorAdapter.setGenerateDdl(true);
        jpaVendorAdapter.setShowSql(false);
        jpaVendorAdapter.setDatabasePlatform("org.hibernate.dialect.H2Dialect");
//        jpaVendorAdapter.setDatabasePlatform("org.hibernate.dialect.MySQLDialect");
        return jpaVendorAdapter;
    }
	
	
	@Bean
	@StepScope
	public JpaPagingItemReader<TestData> reader(@Value("#{jobParameters['lastTimeId']}")Long lastTimeId,
			@Value("#{stepExecutionContext['fromId']}") Long fromId,
			@Value("#{stepExecutionContext['toId']}") Long toId) {
	    JpaPagingItemReader<TestData> databaseReader = new JpaPagingItemReader<>();
	    databaseReader.setEntityManagerFactory(entityManagerFactory().getObject());
        
        JpaNativeQueryProvider<TestData> queryProvider = new JpaNativeQueryProvider<TestData>();
        queryProvider.setSqlQuery("select * from TEST_DATA where id >= :startId and id <= :endId ");
        queryProvider.setEntityClass(TestData.class);
        try {
			queryProvider.afterPropertiesSet();
		} catch (Exception e) {
			e.printStackTrace();
		}
        Map<String, Object> map = new HashMap<>();
        map.put("startId", lastTimeId + fromId);
        map.put("endId", lastTimeId + toId);
        
        databaseReader.setPageSize(3);
        databaseReader.setQueryProvider(queryProvider);
        databaseReader.setParameterValues(map);
        databaseReader.setSaveState(true);
        
	    return databaseReader;
	}
	
	/**
	 * 多工Step
	 * @return
	 */
	@Bean
	public Step partitionStep(){
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2);
        taskExecutor.setMaxPoolSize(2);
        taskExecutor.setAllowCoreThreadTimeOut(true);
        taskExecutor.afterPropertiesSet();
        
		return stepBuilderFactory.get("partitionStep")
				.partitioner("transForData", new DataPartitioner())
				.gridSize(5)
				.step(transForData())
				.taskExecutor(taskExecutor)
				.build();
	}
	
	/**
	 * 主要資料轉換Step
	 * @return
	 */
	@Bean
	public Step transForData() {
		return stepBuilderFactory.get("transForData")
				.<TestData, String>chunk(2)
				.reader(reader(0L, 0L, 0L))
				.processor(new ItemProcessor<TestData, String>() {
					@Override
					public String process(TestData item) throws Exception {
						Thread thread = Thread.currentThread();
						System.out.println(item.getAccount() + " in process, thread id = " + thread.getId());
						return item.getAccount() + " in process, thread id = " + thread.getId();
					}
				})
				.writer(new ItemWriter<String>() {
					@Override
					public void write(List<? extends String> items) throws Exception {
						//items.stream().forEach(System.out::println);
					}
				})
				.build();
	}
	
	/**
	 * Job 執行點
	 * @return
	 */
	@Bean
	public Job doJob(){
		SimpleJob job = (SimpleJob) jobBuilderFactory.get("doJob").start(partitionStep()).build();
		job.setRestartable(Boolean.TRUE);
		return job;
	}
}
