package com.jnu.ticketinfrastructure.model;


import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.util.Objects;
import org.json.JSONObject;

public final class RegistrationPayloadConverter {

    private RegistrationPayloadConverter() {}

    public static String toJson(Registration registration) {
        JSONObject registrationJson = new JSONObject();
        registrationJson.put("email", registration.getEmail());
        registrationJson.put("name", registration.getName());
        registrationJson.put("studentNum", registration.getStudentNum());
        registrationJson.put("affiliation", registration.getAffiliation());
        registrationJson.put("department", registration.getDepartment());
        registrationJson.put("carNum", registration.getCarNum());
        registrationJson.put("phoneNum", registration.getPhoneNum());
        registrationJson.put("isDeleted", registration.isDeleted());
        registrationJson.put("isLight", registration.isLight());
        registrationJson.put("isSaved", registration.isSaved());
        registrationJson.put("id", registration.getId());
        registrationJson.put("createdAt", registration.getCreatedAt());
        registrationJson.put("eventId", registration.getEventId());
        return registrationJson.toString();
    }

    public static boolean hasSameBusinessFields(
            String registrationPayload, Registration registration) {
        try {
            return hasSameBusinessFields(
                    new JSONObject(registrationPayload), new JSONObject(toJson(registration)));
        } catch (RuntimeException malformedPayload) {
            return false;
        }
    }

    public static boolean hasSameBusinessFields(
            String registrationPayload, String candidatePayload) {
        try {
            return hasSameBusinessFields(
                    new JSONObject(registrationPayload), new JSONObject(candidatePayload));
        } catch (RuntimeException malformedPayload) {
            return false;
        }
    }

    private static boolean hasSameBusinessFields(JSONObject payload, JSONObject candidate) {
        return Objects.equals(nullableString(payload, "email"), nullableString(candidate, "email"))
                && Objects.equals(
                        nullableString(payload, "name"), nullableString(candidate, "name"))
                && Objects.equals(
                        nullableString(payload, "studentNum"),
                        nullableString(candidate, "studentNum"))
                && Objects.equals(
                        nullableString(payload, "affiliation"),
                        nullableString(candidate, "affiliation"))
                && Objects.equals(
                        nullableString(payload, "department"),
                        nullableString(candidate, "department"))
                && Objects.equals(
                        nullableString(payload, "carNum"), nullableString(candidate, "carNum"))
                && Objects.equals(
                        nullableString(payload, "phoneNum"), nullableString(candidate, "phoneNum"))
                && payload.has("isLight")
                && candidate.has("isLight")
                && !payload.isNull("isLight")
                && !candidate.isNull("isLight")
                && payload.getBoolean("isLight") == candidate.getBoolean("isLight")
                && Objects.equals(
                        nullableLong(payload, "eventId"), nullableLong(candidate, "eventId"));
    }

    private static String nullableString(JSONObject payload, String key) {
        return payload.has(key) && !payload.isNull(key) ? payload.getString(key) : null;
    }

    private static Long nullableLong(JSONObject payload, String key) {
        return payload.has(key) && !payload.isNull(key) ? payload.getLong(key) : null;
    }
}
