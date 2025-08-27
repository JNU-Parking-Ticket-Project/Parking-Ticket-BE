package com.jnu.ticketbatch.flow;


import com.jnu.ticketinfrastructure.model.RegistrationInFoMapRecord;
import com.jnu.ticketinfrastructure.model.RegistrationInfo;

import java.util.Map;

public interface RegistrationProcessor {

    boolean process(RegistrationInfo registrationInfo);
}
