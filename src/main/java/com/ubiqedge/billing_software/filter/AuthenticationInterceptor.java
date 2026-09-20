package com.ubiqedge.billing_software.filter;

import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.exception.ApiException;
import com.ubiqedge.billing_software.repository.UserRepository;
import com.ubiqedge.billing_software.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import static com.ubiqedge.billing_software.constant.AppConstant.*;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final AuthService authenticationService;
    private final UserRepository userRepository;

    public AuthenticationInterceptor(
            AuthService authenticationService,UserRepository userRepository) {
        this.authenticationService = authenticationService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        String authorization = request.getHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            throw new ApiException(INVALID_SESSION,HttpStatus.BAD_REQUEST);
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            throw new ApiException(INVALID_SESSION,HttpStatus.BAD_REQUEST);
        }

        UserSession session =
                authenticationService.validateSession(token);

        User user = userRepository.findActiveUserById(session.getUserId())
                .orElseThrow(() -> new ApiException(
                        INVALID_SESSION,
                        HttpStatus.BAD_REQUEST
                ));

        request.setAttribute("userSession", session);
        request.setAttribute("user", user);

        return true;
    }
}