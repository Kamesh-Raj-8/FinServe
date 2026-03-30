package com.smelend.smelendbackend.service.common;

import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.exception.ApiException;
import com.smelend.smelendbackend.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final AppUserRepository userRepo;

    public CurrentUserService(AppUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public AppUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        String email = auth.getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found for token"));
    }

    public boolean isAdmin(AppUser user) {
        return user.getRole() != null
                && user.getRole().getRoleName() != null
                && user.getRole().getRoleName().name().equals("ADMIN");
    }
}