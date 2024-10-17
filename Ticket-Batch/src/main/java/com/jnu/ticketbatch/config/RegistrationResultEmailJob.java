package com.jnu.ticketbatch.config;

import groovy.util.logging.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@DisallowConcurrentExecution
public class RegistrationResultEmailJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(RegistrationResultEmailJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {



        } catch (Exception e) {
            log.error("RegistrationResultEmailJob Exception: {}", e.getMessage());
        }
    }
}
