package com.jnu.ticketbatch.flow;


import com.jnu.ticketinfrastructure.model.RegistrationInfo;

public interface RegistrationProcessor {

    boolean process(RegistrationInfo registrationInfo);
}
