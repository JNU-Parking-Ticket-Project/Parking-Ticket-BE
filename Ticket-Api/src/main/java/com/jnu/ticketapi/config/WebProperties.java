package com.jnu.ticketapi.config;


import java.util.List;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Getter
@AllArgsConstructor
@ConstructorBinding
@ConfigurationProperties(prefix = "web.log")
public class WebProperties {
    private List<String> urlNoLogging;
    private List<String> anonymousLogging;

    public boolean isNoLoggable(String path) {
        for (String pattern : urlNoLogging) {
            if (isMatchPattern(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAnonymous(String path) {
        for (String pattern : anonymousLogging) {
            if (isMatchPattern(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMatchPattern(String pattern, String input) {
        String regex;
        if (pattern.contains("**")) {
            regex = pattern.replace(".", "\\.").replace("**", ".*").replace("*", "[^/]*");
            regex = "^" + regex + "$";

            return Pattern.matches(regex, input);
        }
        regex = pattern.replace("*", ".*");
        return Pattern.matches(regex, input);
    }
}
