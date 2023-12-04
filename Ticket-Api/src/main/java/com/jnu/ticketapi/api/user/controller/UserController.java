package com.jnu.ticketapi.api.user.controller;


import com.jnu.ticketapi.api.user.model.request.UpdateRoleRequest;
import com.jnu.ticketapi.api.user.model.response.UpdateRoleResponse;
import com.jnu.ticketapi.api.user.service.UserUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "access-token")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase userUseCase;

    @PutMapping("/admin/role/{userId}")
    public ResponseEntity<UpdateRoleResponse> updateRole(@PathVariable ("userId") Long userId, @RequestBody UpdateRoleRequest request) {
        UpdateRoleResponse response = userUseCase.updateRole(userId, request.role());
        return ResponseEntity.ok(response);
    }
}

