package com.ubiqedge.billing_software.script;

import com.password4j.Password;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@Component
public class AdminInitializer implements ApplicationRunner {

    public final UserRepository userRepository;

    public AdminInitializer(UserRepository userRepository){
        this.userRepository = userRepository;
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {

        if(!userRepository.existsByUsername(SEED_ADMIN_USERNAME)){
            String passwordHash = Password.hash(SEED_ADMIN_PASSWORD).withArgon2().getResult();
            User admin = new User();
            admin.setUsername(SEED_ADMIN_USERNAME);
            admin.setPasswordHash(passwordHash);
            admin.setRole(ROLE_ADMIN);
            Instant now = Instant.now();
            admin.setCreatedAt(now);
            admin.setUpdatedAt(now);
            userRepository.save(admin);
        }

    }
}
