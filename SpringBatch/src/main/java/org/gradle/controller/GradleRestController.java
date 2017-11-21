package org.gradle.controller;

import java.util.Calendar;
import java.util.Date;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/tl")
public class GradleRestController {
	
	@Autowired
    JobLauncher jobLauncher;

    @Autowired
    Job job;

	@RequestMapping(value="/sayHello")
	public ResponseEntity<String> sayHello(@RequestParam("user") String user){
		return new ResponseEntity<String>("Hello~" + user, HttpStatus.ACCEPTED);
	}
	
	@RequestMapping(value="/runJob")
	public void runJob(){
		try {
			Calendar c = Calendar.getInstance();
	        c.add(Calendar.DAY_OF_MONTH, -35);
	        
	        
			JobParameters jobParameters = new JobParametersBuilder()  
                    .addDate("date", new Date())  
//                    .addDate("fireDate", c.getTime())
                    .addLong("lastTimeId", 0L)
                    .toJobParameters(); 
			
			jobLauncher.run(job, jobParameters);
		} catch (JobExecutionAlreadyRunningException | JobRestartException 
				| JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
			e.printStackTrace();
		}
	}
}
