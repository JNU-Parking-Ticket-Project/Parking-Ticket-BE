package com.jnu.ticketapi.api.admin.model.response;


import com.jnu.ticketapi.api.admin.model.internal.UserDto;
import com.jnu.ticketdomain.domains.council.domain.Council;
import java.util.List;
import lombok.Builder;

@Builder
public record GetUserListResponse(List<UserDto> user) {
    public static GetUserListResponse of(List<Council> councilList) {
        return GetUserListResponse.builder().user(UserDto.of(councilList)).build();
    }
}
