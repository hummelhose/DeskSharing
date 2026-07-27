package io.github.hummelhose.desksharing.infrastructure.security;


public class CurrentUserResolutionException extends RuntimeException {

    public CurrentUserResolutionException(String message) {
        super(message);
    }
}