package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.response.UserRoleResponse;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.repository.RoleRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.impl.UserServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private Role userRole;
    private Role moderatorRole;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
                .id(1L)
                .name("USER")
                .build();

        moderatorRole = Role.builder()
                .id(3L)
                .name("MODERATOR")
                .build();

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        user = User.builder()
                .id(3L)
                .name("Test User")
                .email("user@test.com")
                .roles(roles)
                .build();
    }

    @Test
    void addRole_shouldAddModeratorAndKeepExistingUserRole() {
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("MODERATOR"))
                .thenReturn(Optional.of(moderatorRole));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserRoleResponse response =
                userService.addRole(3L, "MODERATOR");

        assertTrue(response.getRoles().contains("USER"));
        assertTrue(response.getRoles().contains("MODERATOR"));
        assertEquals(2, response.getRoles().size());

        verify(userRepository, times(1)).save(user);
    }

    @Test
    void addRole_shouldThrowException_whenUserNotFound() {
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> userService.addRole(999L, "MODERATOR")
                );

        assertTrue(exception.getMessage().contains("User not found"));

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void addRole_shouldThrowException_whenRoleNotFound() {
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("ABC"))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> userService.addRole(3L, "ABC")
                );

        assertTrue(exception.getMessage().contains("Role not found"));

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void addRole_shouldNormalizeRoleNameToUpperCase() {
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("MODERATOR"))
                .thenReturn(Optional.of(moderatorRole));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.addRole(3L, "moderator");

        verify(roleRepository)
                .findByName("MODERATOR");
    }
}