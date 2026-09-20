package com.ubiqedge.billing_software.controller;

import com.ubiqedge.billing_software.dto.ApiResponse;
import com.ubiqedge.billing_software.dto.LoginRequest;
import com.ubiqedge.billing_software.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@RestController
@RequestMapping("api/auth")
public class SessionController {

    private final AuthService authService;

    public SessionController(AuthService authService){
        this.authService = authService;
    }


     @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest loginRequest){
         return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse(Boolean.TRUE,LOGIN_SUCCESSFUL,HttpStatus.OK,authService.login(loginRequest)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestHeader("Authorization") String authorization){
           authService.revokeSession(authorization);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse(Boolean.TRUE,LOGOUT_SUCCESSFUL,HttpStatus.OK,null));
    }


}
