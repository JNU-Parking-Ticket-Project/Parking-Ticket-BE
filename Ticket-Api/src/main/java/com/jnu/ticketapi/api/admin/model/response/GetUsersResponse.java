package com.jnu.ticketapi.api.admin.model.response;


import com.jnu.ticketapi.api.admin.model.internal.UserDto;
import com.jnu.ticketdomain.domains.council.domain.Council;
import java.util.List;
import lombok.Builder;

@Builder
public record GetUsersResponse(List<UserDto> user) {
    public static GetUsersResponse of(List<Council> councilList) {
        return GetUsersResponse.builder().user(UserDto.of(councilList)).build();
    }
}
