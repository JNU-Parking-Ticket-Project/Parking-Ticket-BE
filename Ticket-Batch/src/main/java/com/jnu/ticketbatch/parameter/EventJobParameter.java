package com.jnu.ticketbatch.parameter;


import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import java.util.Objects;
import lombok.Getter;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.beans.factory.annotation.Value;

@Getter
public class EventJobParameter {

    public EventJobParameter(EventAdaptor eventAdaptor) {
        this.eventAdaptor = eventAdaptor;
    }

    private EventAdaptor eventAdaptor;
    private Event event;

    @Value("#{jobParameters[eventId]}")
    public void setDate(Long eventId) throws JobParametersInvalidException {
        if (Objects.isNull(eventId)) {
            throw new JobParametersInvalidException("이벤트 아이디가 필요합니다.");
        }
        this.event = eventAdaptor.findById(eventId);
    }
}
