package com.jnu.ticketapi.api.user.controller;


import com.jnu.ticketapi.api.user.model.request.FindPasswordRequest;
import com.jnu.ticketapi.api.user.model.request.UpdatePasswordRequest;
import com.jnu.ticketapi.api.user.model.request.UpdateRoleRequest;
import com.jnu.ticketapi.api.user.model.response.FindPasswordResponse;
import com.jnu.ticketapi.api.user.model.response.UpdatePasswordResponse;
import com.jnu.ticketapi.api.user.model.response.UpdateRoleResponse;
import com.jnu.ticketapi.api.user.service.CredentialCodeUseCase;
import com.jnu.ticketapi.api.user.service.UpdatePasswordUseCase;
import com.jnu.ticketapi.api.user.service.UserUseCase;
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
@Tag(name = "5. [유저]")
public class UserController {
    private final UserUseCase userUseCase;
    private final CredentialCodeUseCase credentialCodeUseCase;
    private final UpdatePasswordUseCase updatePasswordUseCase;

    @Operation(summary = "권한 설정", description = "사용자의 권한을 설정(ADMIN인 유저만 권한 설정을 할 수 있음)")
    @PutMapping("/admin/role/{userId}")
    public ResponseEntity<UpdateRoleResponse> updateRole(
            @PathVariable("userId") Long userId, @RequestBody UpdateRoleRequest request) {
        UpdateRoleResponse response = userUseCase.updateRole(userId, request.role());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "비밀번호 찾기 메일 전송", description = "비밀번호 찾기 메일 전송")
    @PostMapping("/user/password/find")
    public ResponseEntity<FindPasswordResponse> sendMail(
            @RequestBody FindPasswordRequest findPasswordRequest) {
        return ResponseEntity.ok(credentialCodeUseCase.sendMail(findPasswordRequest));
    }

    @Operation(summary = "비밀번호 재설정", description = "비밀번호 재설정")
    @PostMapping("/user/update/password/{code}")
    public ResponseEntity<UpdatePasswordResponse> updatePassword(
            @PathVariable String code, @RequestBody UpdatePasswordRequest updatePasswordRequest) {

        return ResponseEntity.ok(updatePasswordUseCase.execute(code, updatePasswordRequest));
    }
}
