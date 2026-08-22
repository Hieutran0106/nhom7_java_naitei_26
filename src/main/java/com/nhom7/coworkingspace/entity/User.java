package com.nhom7.coworkingspace.entity;

import com.nhom7.coworkingspace.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private UserStatus status = UserStatus.INACTIVE;

    @Column(name = "is_identity_verified")
    private Boolean isIdentityVerified; // CCCD verified

    @Column(name = "is_business_verified")
    private Boolean isBusinessVerified; // Business license verified

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "cccd_url", length = 500)
    private String cccdUrl;

    @Column(name = "business_license_url", length = 500)
    private String businessLicenseUrl;

    // SHA-256 hex digest of the currently stored business license file's bytes. Lets a resubmission
    // of the exact same file (e.g. a client retrying the become-host call with the same attachment
    // still selected) be recognized as a no-op instead of wiping out an already-approved verification.
    @Column(name = "business_license_hash", length = 64)
    private String businessLicenseHash;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
