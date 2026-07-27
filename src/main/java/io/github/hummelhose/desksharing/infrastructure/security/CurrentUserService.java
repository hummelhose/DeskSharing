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

    private static final String NO_AUTHENTICATED_USER_MESSAGE =
            "Kein angemeldeter Benutzer gefunden.";

    private static final String INVALID_MICROSOFT_IDENTITY_MESSAGE =
            "Der angemeldete Microsoft-Benutzer enthält keine verwertbare Benutzerkennung.";

    private static final String DEFAULT_MICROSOFT_DISPLAY_NAME =
            "Microsoft Benutzer";

    private final UserSyncService userSyncService;

    public CurrentUserService(UserSyncService userSyncService) {
        this.userSyncService = userSyncService;
    }

    public AppUser getOrCreateCurrentUser() {
        Authentication authentication = getRequiredAuthentication();

        if (authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken) {
            return getOrCreateMicrosoftUser(oauth2AuthenticationToken);
        }

        return getOrCreateLocalUser(authentication);
    }

    private Authentication getRequiredAuthentication() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new CurrentUserResolutionException(
                    NO_AUTHENTICATED_USER_MESSAGE
            );
        }

        return authentication;
    }

    private AppUser getOrCreateMicrosoftUser(
            OAuth2AuthenticationToken authentication
    ) {
        OAuth2User principal = authentication.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();

        String entraOid = getFirstNonBlankAttribute(
                attributes,
                "oid",
                "sub"
        );

        String email = getFirstNonBlankAttribute(
                attributes,
                "email",
                "preferred_username",
                "upn"
        );

        /*
         * Mindestens eine stabile Kennung muss vorhanden sein.
         * Andernfalls könnte kein Benutzer zuverlässig synchronisiert werden.
         */
        if (isBlank(entraOid) && isBlank(email)) {
            throw new CurrentUserResolutionException(
                    INVALID_MICROSOFT_IDENTITY_MESSAGE
            );
        }

        if (isBlank(entraOid)) {
            entraOid = "microsoft-" + email;
        }

        if (isBlank(email)) {
            email = entraOid + "@microsoft.local";
        }

        String displayName = getFirstNonBlankAttribute(
                attributes,
                "name"
        );

        if (isBlank(displayName) && principal instanceof OidcUser oidcUser) {
            displayName = oidcUser.getFullName();
        }

        if (isBlank(displayName)) {
            displayName = email;
        }

        if (isBlank(displayName)) {
            displayName = DEFAULT_MICROSOFT_DISPLAY_NAME;
        }

        return userSyncService.syncUser(
                entraOid,
                email,
                displayName,
                AppRole.USER
        );
    }

    private AppUser getOrCreateLocalUser(Authentication authentication) {
        String username = authentication.getName();

        AppRole role = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority.getAuthority())
                )
                ? AppRole.ADMIN
                : AppRole.USER;

        String entraOid = "local-" + username;
        String email = username + "@local.dev";
        String displayName = username;

        return userSyncService.syncUser(
                entraOid,
                email,
                displayName,
                role
        );
    }

    private String getFirstNonBlankAttribute(
            Map<String, Object> attributes,
            String... keys
    ) {
        for (String key : keys) {
            Object value = attributes.get(key);

            if (value == null) {
                continue;
            }

            String stringValue = value.toString();

            if (!stringValue.isBlank()) {
                return stringValue;
            }
        }

        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}