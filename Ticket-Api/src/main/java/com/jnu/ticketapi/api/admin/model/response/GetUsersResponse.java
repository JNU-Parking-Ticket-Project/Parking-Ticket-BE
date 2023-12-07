package com.jnu.ticketapi.api.admin.model.response;


import com.jnu.ticketapi.api.admin.model.internal.UserDto;
import com.jnu.ticketdomain.domains.council.domain.Council;
import java.util.List;
import lombok.Builder;

@Builder
public record GetUsersResponse(List<UserDto> users) {
    public static GetUsersResponse of(List<Council> councilList) {
        return GetUsersResponse.builder().users(UserDto.of(councilList)).build();
    }
}
