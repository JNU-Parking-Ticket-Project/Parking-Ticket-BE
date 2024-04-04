package com.jnu.ticketapi.api.admin.controller;


import com.jnu.ticketapi.api.admin.docs.UpdateRoleExceptionDocs;
import com.jnu.ticketapi.api.admin.model.request.UpdateRoleRequest;
import com.jnu.ticketapi.api.admin.model.response.GetUsersResponse;
import com.jnu.ticketapi.api.admin.model.response.UpdateRoleResponse;
import com.jnu.ticketapi.api.admin.service.AdminUseCase;
import com.jnu.ticketcommon.annotation.ApiErrorExceptionsExample;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "access-token")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "8. [관리자]")
public class AdminController {
    private final AdminUseCase adminUseCase;

    @Operation(summary = "학생회 회원가입 목록 조회", description = "학생회 회원가입 목록을 조회(ADMIN인 유저만 사용 가능)")
    @GetMapping("/admin/councils")
    public ResponseEntity<GetUsersResponse> getUserList() {
        GetUsersResponse response = adminUseCase.getUserList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "권한 설정", description = "사용자의 권한을 설정(ADMIN인 유저만 권한 설정을 할 수 있음)")
    @PutMapping("/admin/role/{userId}")
    @ApiErrorExceptionsExample(UpdateRoleExceptionDocs.class)
    public ResponseEntity<UpdateRoleResponse> updateRole(
            @PathVariable("userId") Long userId, @RequestBody @Valid UpdateRoleRequest request) {
        UpdateRoleResponse response = adminUseCase.updateRole(userId, request.role().getValue());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/error")
    public void error() {
        throw new RuntimeException("error");
    }
}
