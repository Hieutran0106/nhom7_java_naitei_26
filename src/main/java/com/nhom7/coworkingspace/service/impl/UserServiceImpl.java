package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.response.UserRoleResponse;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.repository.RoleRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    
    private final UserRepository userRepository;

   
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public UserRoleResponse addRole(Long userId, String roleName) {

        
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: " + userId
                        )
                );

       
        String normalizedRoleName =
                roleName.trim().toUpperCase();

        
        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Role not found: " + normalizedRoleName
                        )
                );

        
        user.getRoles().add(role);

        
        User updatedUser = userRepository.save(user);

        
        Set<String> roleNames = updatedUser.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        
        return UserRoleResponse.builder()
                .id(updatedUser.getId())
                .name(updatedUser.getName())
                .email(updatedUser.getEmail())
                .roles(roleNames)
                .build();
    }
}