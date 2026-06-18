package io.github.hummelhose.desksharing.application.service;

import io.github.hummelhose.desksharing.domain.model.AppRole;
import io.github.hummelhose.desksharing.domain.model.AppUser;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.AppUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;

    public AppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public List<AppUser> getAllUsers() {
        return appUserRepository.findAll();
    }

    public Optional<AppUser> getUserById(Long id) {
        return appUserRepository.findById(id);
    }

    public Optional<AppUser> updateUserRole(Long userId, AppRole role) {
        return appUserRepository.findById(userId)
                .map(existingUser -> {
                    existingUser.setRole(role);
                    return appUserRepository.save(existingUser);
                });
    }
}