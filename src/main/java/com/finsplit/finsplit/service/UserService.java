package com.finsplit.finsplit.service;

import com.finsplit.finsplit.dto.AuthDTOs.UserProfileResponse;
import com.finsplit.finsplit.exception.ResourceNotFoundException;
import com.finsplit.finsplit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getUserProfile(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return new UserProfileResponse(
                user.getId().toString(), user.getName(),
                user.getEmail(), user.getCreatedAt().toString()
        );
    }
}
