package com.smelend.smelendbackend.security;

import com.smelend.smelendbackend.entity.AppUser;
import com.smelend.smelendbackend.entity.enums.StatusFlag;
import com.smelend.smelendbackend.repository.AppUserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepo;

    public CustomUserDetailsService(AppUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepo.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getStatus() != StatusFlag.ACTIVE) {
            throw new DisabledException("User account is inactive");
        }

        String role = "ROLE_" + user.getRole().getRoleName().name();

        return new User(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}