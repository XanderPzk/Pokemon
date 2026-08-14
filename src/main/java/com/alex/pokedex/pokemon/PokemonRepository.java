package com.alex.pokedex.pokemon;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PokemonRepository extends JpaRepository<Pokemon, Long> {

    Optional<Pokemon> findByExternalId(int externalId);
}
