package com.ubiqedge.billing_software.service;

import com.password4j.Password;
import com.ubiqedge.billing_software.dto.LoginRequest;
import com.ubiqedge.billing_software.dto.LoginResponse;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.exception.ApiException;
import com.ubiqedge.billing_software.repository.SessionRepository;
import com.ubiqedge.billing_software.repository.UserRepository;
import com.ubiqedge.billing_software.util.TokenUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    public AuthService(UserRepository userRepository,SessionRepository sessionRepository){
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }


    public LoginResponse login(LoginRequest loginRequest)  {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(loginRequest.username()).orElseThrow(() ->
                new ApiException(INVALID_USERNAME_PASSWORD, HttpStatus.BAD_REQUEST));

        boolean validPassword = Password.check(loginRequest.password(),user.getPasswordHash()).withArgon2();

        if(!validPassword){
            throw new ApiException(INVALID_USERNAME_PASSWORD,HttpStatus.BAD_REQUEST);
        }

        SecureRandom secureRandom = new SecureRandom();
        byte [] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String sessionToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now();
        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setToken(TokenUtil.hashToken(sessionToken));
        session.setCreatedAt(now);
        session.setExpiresAt(now.plus(6, ChronoUnit.HOURS));
        session.setRevokedAt(null);
        sessionRepository.save(session);

        LoginResponse loginResponse = new LoginResponse(sessionToken);

        return loginResponse;
    }

    public void revokeSession(String authorization){
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ApiException(INVALID_SESSION, HttpStatus.BAD_REQUEST);
        }

        authorization = authorization.substring(7);
      int updatedRows = sessionRepository.revokeSession(TokenUtil.hashToken(authorization));
      if(updatedRows == 0){
          throw new ApiException(INVALID_SESSION,HttpStatus.BAD_REQUEST);
      }
      if(updatedRows > 1 ){
          throw new ApiException(GENRIC_ERROR_MESSAGE,HttpStatus.INTERNAL_SERVER_ERROR);
      }


    }


    public UserSession validateSession(String token) {

        String tokenHash = TokenUtil.hashToken(token);

        UserSession session = sessionRepository
                .findActiveSession(tokenHash)
                .orElseThrow(() -> new ApiException(
                        INVALID_SESSION,
                        HttpStatus.BAD_REQUEST
                ));

        if (!Instant.now().isBefore(session.getExpiresAt())) {

            sessionRepository.revokeSession(tokenHash);

            throw new ApiException(
                    SESSION_EXPIRED,
                    HttpStatus.UNAUTHORIZED
            );
        }

        return session;
    }

}
