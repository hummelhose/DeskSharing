package io.github.hummelhose.desksharing.application.service;

import io.github.hummelhose.desksharing.domain.model.AppRole;
import io.github.hummelhose.desksharing.domain.model.AppUser;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.AppUserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserSyncService {

    private final AppUserRepository appUserRepository;

    public UserSyncService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser syncUser(String entraOid, String email, String displayName, AppRole defaultRole) {
        return appUserRepository.findByEntraOid(entraOid)
                .map(existingUser -> updateExistingUser(existingUser, email, displayName))
                .orElseGet(() -> createNewUser(entraOid, email, displayName, defaultRole));
    }

    private AppUser updateExistingUser(AppUser existingUser, String email, String displayName) {
        existingUser.setEmail(email);
        existingUser.setDisplayName(displayName);
        existingUser.setLastLoginAt(LocalDateTime.now());

        return appUserRepository.save(existingUser);
    }

    private AppUser createNewUser(String entraOid, String email, String displayName, AppRole defaultRole) {
        AppUser newUser = new AppUser(
                entraOid,
                email,
                displayName,
                defaultRole,
                LocalDateTime.now()
        );

        newUser.setLastLoginAt(LocalDateTime.now());

        return appUserRepository.save(newUser);
    }
}