package io.github.hummelhose.desksharing.infrastructure.security;

import io.github.hummelhose.desksharing.domain.model.AppRole;
import io.github.hummelhose.desksharing.domain.model.AppUser;
import org.springframework.stereotype.Service;

@Service
public class AdminAccessService {

    private final CurrentUserService currentUserService;

    public AdminAccessService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public boolean isCurrentUserAdmin() {
        try {
            AppUser currentUser =
                    currentUserService.getOrCreateCurrentUser();

            return currentUser.getRole() == AppRole.ADMIN;
        } catch (CurrentUserResolutionException exception) {
            return false;
        }
    }
}