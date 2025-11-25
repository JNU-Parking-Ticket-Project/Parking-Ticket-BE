package com.jnu.ticketbatch.config;

import com.jnu.ticketbatch.flow.DataProcessor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.repository.SectorRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@DisallowConcurrentExecution
public class DataProcessingJob implements Job {

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private DataProcessor dataProcessor;

    public DataProcessingJob() {
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long eventId = (Long) context.getJobDetail().getJobDataMap().get("eventId");
        List<Sector> sectors = sectorRepository.findByEventId(eventId);
        ExecutorService executorService = Executors.newFixedThreadPool(sectors.size());
        for (Sector sector : sectors) {
            executorService.submit(() -> {
                try {
                    dataProcessor.start(sector.getId());
                } catch (Exception e) {
                    log.error("에러발생", e);
                }
            });
        }

    }
}
