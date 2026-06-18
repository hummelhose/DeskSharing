package io.github.hummelhose.desksharing.infrastructure.security;

import io.github.hummelhose.desksharing.application.service.UserSyncService;
import io.github.hummelhose.desksharing.domain.model.AppRole;
import io.github.hummelhose.desksharing.domain.model.AppUser;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CurrentUserService {

    private final UserSyncService userSyncService;

    public CurrentUserService(UserSyncService userSyncService) {
        this.userSyncService = userSyncService;
    }

    public AppUser getOrCreateCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Kein angemeldeter Benutzer gefunden.");
        }

        if (authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken) {
            return getOrCreateMicrosoftUser(oauth2AuthenticationToken);
        }

        return getOrCreateLocalUser(authentication);
    }

    private AppUser getOrCreateMicrosoftUser(OAuth2AuthenticationToken authentication) {
        OAuth2User principal = authentication.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();

        String entraOid = getAttributeAsString(attributes, "oid");

        if (isBlank(entraOid)) {
            entraOid = getAttributeAsString(attributes, "sub");
        }

        String email = getAttributeAsString(attributes, "email");

        if (isBlank(email)) {
            email = getAttributeAsString(attributes, "preferred_username");
        }

        if (isBlank(email)) {
            email = getAttributeAsString(attributes, "upn");
        }

        String displayName = getAttributeAsString(attributes, "name");

        if (isBlank(displayName) && principal instanceof OidcUser oidcUser) {
            displayName = oidcUser.getFullName();
        }

        if (isBlank(displayName)) {
            displayName = email;
        }

        if (isBlank(displayName)) {
            displayName = "Microsoft Benutzer";
        }

        if (isBlank(email)) {
            email = entraOid + "@microsoft.local";
        }

        if (isBlank(entraOid)) {
            entraOid = "microsoft-" + email;
        }

        AppRole role = AppRole.USER;

        return userSyncService.syncUser(entraOid, email, displayName, role);
    }

    private AppUser getOrCreateLocalUser(Authentication authentication) {
        String username = authentication.getName();

        AppRole role = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))
                ? AppRole.ADMIN
                : AppRole.USER;

        String entraOid = "local-" + username;
        String email = username + "@local.dev";
        String displayName = username;

        return userSyncService.syncUser(entraOid, email, displayName, role);
    }

    private String getAttributeAsString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);

        if (value == null) {
            return null;
        }

        return value.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}