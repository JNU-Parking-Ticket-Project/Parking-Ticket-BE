package com.jnu.ticketapi.controller;


import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@SecurityRequirement(name = "access-token")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class UserController {}
