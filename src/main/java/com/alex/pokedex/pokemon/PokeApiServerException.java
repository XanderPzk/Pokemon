package com.alex.pokedex.pokemon;

/**
 * Raised for PokeAPI responses that are worth retrying or that indicate an upstream defect, as
 * opposed to a genuinely absent Pokemon.
 *
 * <p>This class name is referenced by fully-qualified name in the {@code resilience4j.retry} and
 * {@code resilience4j.circuitbreaker} sections of {@code application.yml}, so renaming or moving it
 * silently disables retry and circuit breaking. Update the configuration alongside any move.
 */
public class PokeApiServerException extends RuntimeException {

    public PokeApiServerException(String message) {
        super(message);
    }
}
