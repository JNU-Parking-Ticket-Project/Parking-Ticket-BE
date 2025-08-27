package com.jnu.ticketbatch.flow;

import java.util.Map;

public interface RegistrationProcessor {

    boolean process(Map<String, String> registration);
}
