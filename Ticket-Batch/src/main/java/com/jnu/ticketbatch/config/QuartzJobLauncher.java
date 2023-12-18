package com.jnu.ticketbatch.config;


import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

@RequiredArgsConstructor
public class QuartzJobLauncher implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        ApplicationContext applicationContext =
                (ApplicationContext)
                        context.getJobDetail().getJobDataMap().get("applicationContext");

        JobDataMap jobDataMap = context.getMergedJobDataMap();
        Long eventId = jobDataMap.getLong("eventId");
        EventAdaptor eventAdaptor = applicationContext.getBean(EventAdaptor.class);
        Event event = eventAdaptor.findById(eventId);
        eventAdaptor.updateEventStatus(event, EventStatus.OPEN);
    }
}
