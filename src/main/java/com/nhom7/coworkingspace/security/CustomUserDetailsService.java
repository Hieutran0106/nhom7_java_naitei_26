package com.nhom7.coworkingspace.security;

import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Adapter between the application's {@link User} entity and Spring Security's
 * {@link UserDetailsService} contract.
 *
 * <p>Roles are mapped with the {@code ROLE_} prefix (e.g. {@code "ADMIN"} → {@code "ROLE_ADMIN"}),
 * so {@code @PreAuthorize("hasRole('ADMIN')")} works without extra configuration.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[Auth] User not found for email: {}", email);
                    return new UsernameNotFoundException("Invalid credentials");
                });

        // Map Set<Role> → List<GrantedAuthority> with ROLE_ prefix
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .toList();

        boolean isEnabled = user.getStatus() != UserStatus.BLOCKED;
        boolean isAccountNonLocked = user.getStatus() != UserStatus.BLOCKED;

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                isEnabled,
                true,
                true,
                isAccountNonLocked,
                authorities
        );
    }
}
