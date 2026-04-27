package com.smelend.smelendbackend.config;

import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.Role;
import com.smelend.smelendbackend.entity.enums.RoleName;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import com.smelend.smelendbackend.repository.AppUserRepository;
import com.smelend.smelendbackend.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.EnumMap;
import java.util.Map;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedSystemData(
            RoleRepository roleRepo,
            AppUserRepository userRepo,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.full-name}") String adminFullName,
            @Value("${app.bootstrap.admin.email}") String adminEmail,
            @Value("${app.bootstrap.admin.password}") String adminPassword,
            @Value("${app.bootstrap.admin.phone}") String adminPhone,

            @Value("${app.bootstrap.agent.full-name}") String agentFullName,
            @Value("${app.bootstrap.agent.email}") String agentEmail,
            @Value("${app.bootstrap.agent.password}") String agentPassword,
            @Value("${app.bootstrap.agent.phone}") String agentPhone,

            @Value("${app.bootstrap.underwriter.full-name}") String underwriterFullName,
            @Value("${app.bootstrap.underwriter.email}") String underwriterEmail,
            @Value("${app.bootstrap.underwriter.password}") String underwriterPassword,
            @Value("${app.bootstrap.underwriter.phone}") String underwriterPhone,

            @Value("${app.bootstrap.operations.full-name}") String operationsFullName,
            @Value("${app.bootstrap.operations.email}") String operationsEmail,
            @Value("${app.bootstrap.operations.password}") String operationsPassword,
            @Value("${app.bootstrap.operations.phone}") String operationsPhone,

            @Value("${app.bootstrap.servicing.full-name}") String servicingFullName,
            @Value("${app.bootstrap.servicing.email}") String servicingEmail,
            @Value("${app.bootstrap.servicing.password}") String servicingPassword,
            @Value("${app.bootstrap.servicing.phone}") String servicingPhone,

            @Value("${app.bootstrap.collections.full-name}") String collectionsFullName,
            @Value("${app.bootstrap.collections.email}") String collectionsEmail,
            @Value("${app.bootstrap.collections.password}") String collectionsPassword,
            @Value("${app.bootstrap.collections.phone}") String collectionsPhone,

            @Value("${app.bootstrap.risk.full-name}") String riskFullName,
            @Value("${app.bootstrap.risk.email}") String riskEmail,
            @Value("${app.bootstrap.risk.password}") String riskPassword,
            @Value("${app.bootstrap.risk.phone}") String riskPhone,

            @Value("${app.bootstrap.compliance.full-name}") String complianceFullName,
            @Value("${app.bootstrap.compliance.email}") String complianceEmail,
            @Value("${app.bootstrap.compliance.password}") String compliancePassword,
            @Value("${app.bootstrap.compliance.phone}") String compliancePhone
    ) {
        return args -> {

            Map<RoleName, Role> roleMap = new EnumMap<>(RoleName.class);

            for (RoleName roleName : RoleName.values()) {
                Role role = roleRepo.findByRoleName(roleName)
                        .orElseGet(() -> roleRepo.save(
                                Role.builder()
                                        .roleName(roleName)
                                        .status(StatusFlag.ACTIVE)
                                        .build()
                        ));
                roleMap.put(roleName, role);
            }

            seedUserIfMissing(userRepo, passwordEncoder,
                    adminFullName, adminEmail, adminPassword, adminPhone, roleMap.get(RoleName.ADMIN));

            seedUserIfMissing(userRepo, passwordEncoder,
                    agentFullName, agentEmail, agentPassword, agentPhone, roleMap.get(RoleName.AGENT));

            seedUserIfMissing(userRepo, passwordEncoder,
                    underwriterFullName, underwriterEmail, underwriterPassword, underwriterPhone, roleMap.get(RoleName.UNDERWRITER));

            seedUserIfMissing(userRepo, passwordEncoder,
                    operationsFullName, operationsEmail, operationsPassword, operationsPhone, roleMap.get(RoleName.OPERATIONS));

            seedUserIfMissing(userRepo, passwordEncoder,
                    servicingFullName, servicingEmail, servicingPassword, servicingPhone, roleMap.get(RoleName.SERVICING));

            seedUserIfMissing(userRepo, passwordEncoder,
                    collectionsFullName, collectionsEmail, collectionsPassword, collectionsPhone, roleMap.get(RoleName.COLLECTIONS));

            seedUserIfMissing(userRepo, passwordEncoder,
                    riskFullName, riskEmail, riskPassword, riskPhone, roleMap.get(RoleName.RISK));

            seedUserIfMissing(userRepo, passwordEncoder,
                    complianceFullName, complianceEmail, compliancePassword, compliancePhone, roleMap.get(RoleName.COMPLIANCE));
        };
    }

    private void seedUserIfMissing(AppUserRepository userRepo,
                                   PasswordEncoder passwordEncoder,
                                   String fullName,
                                   String email,
                                   String rawPassword,
                                   String phone,
                                   Role role) {

        userRepo.findByEmail(email).orElseGet(() ->
                userRepo.save(
                        AppUser.builder()
                                .fullName(fullName)
                                .email(email)
                                .passwordHash(passwordEncoder.encode(rawPassword))
                                .phone(phone)
                                .role(role)
                                .status(StatusFlag.ACTIVE)
                                .build()
                )
        );
    }
}