package com.alex.pokedex.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/exceptions")
class ExceptionTestController {

    @GetMapping("/unexpected")
    void unexpected() {
        throw new RuntimeException("Sensitive internal detail");
    }

    @GetMapping("/access-denied")
    void accessDenied() {
        throw new AccessDeniedException("Denied");
    }

    @GetMapping("/authentication")
    void authentication() {
        throw new BadCredentialsException("Bad credentials");
    }

    @GetMapping("/method-not-supported")
    void methodNotSupported() {}

    @PatchMapping(value = "/unsupported-media-type", consumes = MediaType.APPLICATION_JSON_VALUE)
    void unsupportedMediaType() {}

    @GetMapping("/missing-parameter")
    void missingParameter(@RequestParam String required) {}

    @GetMapping("/data-integrity")
    void dataIntegrity() {
        throw new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"users_email_key\"");
    }
}
