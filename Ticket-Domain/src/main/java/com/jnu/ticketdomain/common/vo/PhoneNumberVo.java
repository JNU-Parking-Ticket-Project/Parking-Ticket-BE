package com.jnu.ticketdomain.common.vo;


import com.fasterxml.jackson.annotation.JsonValue;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.jnu.ticketdomain.common.util.PhoneNumberInstance;
import com.jnu.ticketdomain.domains.user.exception.UserPhoneNumberInvalidException;
import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneNumberVo {

    // +82 10-xxxx-xxxx format 으로 저장.
    private String phoneNumber;

    public PhoneNumberVo(String rawPhoneNumber) {
        this.phoneNumber = rawPhoneNumber;
    }

    public static PhoneNumberVo valueOf(String rawPhoneNumber) {
        return new PhoneNumberVo(rawPhoneNumber);
    }

    private static PhoneNumber getPhoneNumber(String rawPhoneNumber) {
        PhoneNumberUtil instance = PhoneNumberInstance.instance;
        try {
            return instance.parse(rawPhoneNumber, "KR");
        } catch (NumberParseException e) {
            throw UserPhoneNumberInvalidException.EXCEPTION;
        }
    }

    /** 010-xxxx-xxxx format */
    @JsonValue
    public String getNationalFormat() {
        PhoneNumberUtil instance = PhoneNumberInstance.instance;
        PhoneNumber phoneNumber = getPhoneNumber(this.phoneNumber);
        return instance.format(phoneNumber, PhoneNumberFormat.NATIONAL);
    }

    /** +82 10-xxxx-xxxx format */
    public String getInternationalFormat() {
        PhoneNumberUtil instance = PhoneNumberInstance.instance;
        PhoneNumber phoneNum = getPhoneNumber(this.phoneNumber);
        return instance.format(phoneNum, PhoneNumberFormat.INTERNATIONAL);
    }

    /** 01000000000 format */
    public String getNaverSmsToNumber() {
        try {
            String nationalFormat = this.getNationalFormat();
            return nationalFormat.replaceAll("-", "");
        } catch (NumberFormatException e) {
            // Handle the exception here
            throw UserPhoneNumberInvalidException.EXCEPTION;
        }
    }
}
