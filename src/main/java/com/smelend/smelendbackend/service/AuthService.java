package com.smelend.smelendbackend.service;

import com.smelend.smelendbackend.dto.auth.LoginRequest;
import com.smelend.smelendbackend.dto.auth.LoginResponse;
import com.smelend.smelendbackend.dto.auth.RegisterRequest;
import com.smelend.smelendbackend.dto.auth.RegisterResponse;
import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.Role;
import com.smelend.smelendbackend.entity.enums.RoleName;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.AppUserRepository;
import com.smelend.smelendbackend.repository.RoleRepository;
import com.smelend.smelendbackend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepo,
                       RoleRepository roleRepo,
                       PasswordEncoder encoder,
                       AuthenticationManager authManager,
                       JwtService jwtService) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    public RegisterResponse register(RegisterRequest req) {
        userRepo.findByEmail(req.getEmail()).ifPresent(u -> {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        });

        if (req.getRole() != RoleName.APPLICANT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only APPLICANT can self-register");
        }

        Role role = roleRepo.findByRoleName(RoleName.APPLICANT)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "APPLICANT role not found in system"
                ));

        if (role.getStatus() != StatusFlag.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "APPLICANT role is inactive");
        }

        AppUser user = AppUser.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .passwordHash(encoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(role)
                .bankAccountNo(req.getBankAccountNo())
                .ifsc(req.getIfsc())
                .status(StatusFlag.ACTIVE)
                .build();

        AppUser saved = userRepo.save(user);

        String token = jwtService.generateToken(saved.getEmail(), Map.of(
                "role", saved.getRole().getRoleName().name(),
                "userId", saved.getUserId()
        ));

        return RegisterResponse.builder()
                .userId(saved.getUserId())
                .email(saved.getEmail())
                .role(saved.getRole().getRoleName())
                .token(token)
                .build();
    }

    public LoginResponse login(LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        AppUser user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (user.getStatus() != StatusFlag.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "User account is inactive");
        }

        String token = jwtService.generateToken(user.getEmail(), Map.of(
                "role", user.getRole().getRoleName().name(),
                "userId", user.getUserId()
        ));

        return LoginResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getRole().getRoleName())
                .token(token)
                .build();
    }
}