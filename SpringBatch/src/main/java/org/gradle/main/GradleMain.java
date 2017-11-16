package org.gradle.main;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages={"org.gradle"})
@EnableBatchProcessing
public class GradleMain {

	public static void main(String[] args){
		SpringApplication.run(GradleMain.class, args);
	}
}
