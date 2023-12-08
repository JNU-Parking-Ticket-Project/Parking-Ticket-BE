package com.jnu.ticketapi.api.council.model.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExcelDataTransfer {
    private String userSector;
    private String userPhoneNumber;
    private String userEmail;
    private String userName;
    private String studentNumber;
    private String userAffiliation;
    private String carNumber;
    private String carIsLight;

}
