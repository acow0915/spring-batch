package org.gradle.config;

import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BatchConfig {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	
	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	
	@Bean
	public Step transForData( ) {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.setAllowCoreThreadTimeOut(true);
        taskExecutor.afterPropertiesSet();
        
		return stepBuilderFactory.get("transForData")
				.<String, String>chunk(10)
				.reader(new ItemReader<String>() {
					@Override
					public String read()
							throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
						Thread thread = Thread.currentThread();
						System.out.println("test1, thread id = " + thread.getId());
//						return "test1, thread id = " + thread.getId();
						return null;
					}
				})
				.processor(new ItemProcessor<String, String>() {

					@Override
					public String process(String item) throws Exception {
						Thread thread = Thread.currentThread();
						System.out.println(item + " in process, thread id = " + thread.getId());
						return item + " in process, thread id = " + thread.getId();
					}
				})
				.writer(new ItemWriter<String>() {
					@Override
					public void write(List<? extends String> items) throws Exception {
						//items.stream().forEach(System.out::println);
					}
				})
				.taskExecutor(taskExecutor)
				.build();
	}
	
	@Bean
	public Job doJob(){
		SimpleJob job = (SimpleJob) jobBuilderFactory.get("doJob").start(transForData()).build();
		job.setRestartable(Boolean.TRUE);
		return job;
	}
}
