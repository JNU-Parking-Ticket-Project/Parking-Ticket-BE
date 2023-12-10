package com.jnu.ticketapi.api.admin.model.internal;


import com.jnu.ticketdomain.domains.council.domain.Council;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;

@Builder
public record UserDto(Long userId, String name, String studentNum, String phoneNum, String role) {
    public static List<UserDto> of(List<Council> councilList) {
        return councilList.stream()
                .map(
                        council ->
                                UserDto.builder()
                                        .userId(council.getUser().getId())
                                        .name(council.getName())
                                        .studentNum(council.getStudentNum())
                                        .phoneNum(council.getPhoneNum())
                                        .role(council.getUser().getUserRole().toString())
                                        .build())
                .collect(Collectors.toList());
    }
}
