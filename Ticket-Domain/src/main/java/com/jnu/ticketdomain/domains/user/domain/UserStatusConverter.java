package com.jnu.ticketdomain.domains.user.domain;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {

    @Override
    public String convertToDatabaseColumn(UserStatus userStatus) {
        if (userStatus == null) {
            return null;
        }
        return userStatus.getValue();
    }

    @Override
    public UserStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        return switch (dbData) {
            case "합격" -> UserStatus.SUCCESS;
            case "예비" -> UserStatus.PREPARE;
            case "불합격" -> UserStatus.FAIL;
            default -> throw new IllegalArgumentException("Unknown value: " + dbData);
        };
    }
}
