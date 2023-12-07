package com.jnu.ticketapi.api.admin.controller;


import com.jnu.ticketapi.api.admin.model.request.UpdateRoleRequest;
import com.jnu.ticketapi.api.admin.model.response.GetUserListResponse;
import com.jnu.ticketapi.api.admin.model.response.UpdateRoleResponse;
import com.jnu.ticketapi.api.admin.service.AdminUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public ResponseEntity<GetUserListResponse> getUserList() {
        GetUserListResponse response = adminUseCase.getUserList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "권한 설정", description = "사용자의 권한을 설정(ADMIN인 유저만 권한 설정을 할 수 있음)")
    @PutMapping("/admin/role/{userId}")
    public ResponseEntity<UpdateRoleResponse> updateRole(
            @PathVariable("userId") Long userId, @RequestBody UpdateRoleRequest request) {
        UpdateRoleResponse response = adminUseCase.updateRole(userId, request.role());
        return ResponseEntity.ok(response);
    }
}