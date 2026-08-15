package com.alex.pokedex.pokemon.repository;

import com.alex.pokedex.common.ApiException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "pokemon")
public class Pokemon {

    public static final int MAX_INTERNAL_TAGS = 20;

    public record Snapshot(
            int externalId,
            String name,
            String spriteUrl,
            String category,
            int mass,
            List<String> abilities) {}

    public record LocalEdits(
            Optional<String> localName,
            Optional<String> region,
            Optional<List<String>> internalTags) {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private int externalId;

    @Column(nullable = false)
    private String name;

    @Column(name = "sprite_url")
    private String spriteUrl;

    private String category;

    @Column(nullable = false)
    private int mass;

    @Column(name = "local_name", nullable = false)
    private String localName;

    private String region;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection
    @CollectionTable(name = "pokemon_ability", joinColumns = @JoinColumn(name = "pokemon_id"))
    @OrderColumn(name = "position")
    @Column(name = "ability", nullable = false)
    private List<String> abilities = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "pokemon_internal_tag", joinColumns = @JoinColumn(name = "pokemon_id"))
    @OrderColumn(name = "position")
    @Column(name = "tag", nullable = false)
    private List<String> internalTags = new ArrayList<>();

    protected Pokemon() {}

    public static Pokemon fromSnapshot(Snapshot snapshot) {
        Pokemon pokemon = new Pokemon();
        pokemon.externalId = snapshot.externalId();
        pokemon.name = snapshot.name();
        pokemon.spriteUrl = snapshot.spriteUrl();
        pokemon.category = snapshot.category();
        pokemon.mass = snapshot.mass();
        pokemon.abilities = new ArrayList<>(snapshot.abilities());
        pokemon.localName = snapshot.name();
        pokemon.region = null;
        pokemon.internalTags = new ArrayList<>();
        return pokemon;
    }

    public void refreshSnapshot(Snapshot snapshot) {
        if (externalId != snapshot.externalId()) {
            throw ApiException.badRequest(
                    "Snapshot external id must match the synced Pokemon external id", "id");
        }
        this.name = snapshot.name();
        this.spriteUrl = snapshot.spriteUrl();
        this.category = snapshot.category();
        this.mass = snapshot.mass();
        this.abilities = new ArrayList<>(snapshot.abilities());
    }

    public void applyLocalEdits(LocalEdits edits) {
        boolean hasLocalName = edits.localName() != null;
        boolean hasRegion = edits.region() != null;
        boolean hasTags = edits.internalTags() != null;

        if (!hasLocalName && !hasRegion && !hasTags) {
            throw ApiException.badRequest("At least one editable field must be provided", null);
        }

        if (hasLocalName) {
            Optional<String> localName = edits.localName();
            if (localName.isEmpty()) {
                throw ApiException.badRequest("Local name must not be blank", "localName");
            }
            validateLocalName(localName.get());
            this.localName = localName.get().trim();
        }
        if (hasRegion) {
            this.region = edits.region().orElse(null);
        }
        if (hasTags) {
            this.internalTags = validateTags(edits.internalTags().orElse(null));
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    private static void validateLocalName(String localName) {
        if (localName == null || localName.isBlank()) {
            throw ApiException.badRequest("Local name must not be blank", "localName");
        }
    }

    private static List<String> validateTags(List<String> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }
        if (tags.size() > MAX_INTERNAL_TAGS) {
            throw ApiException.badRequest(
                    "Internal tags must not exceed " + MAX_INTERNAL_TAGS, "internalTags");
        }
        List<String> validated = new ArrayList<>();
        for (String tag : tags) {
            if (tag == null) {
                throw ApiException.badRequest(
                        "Internal tags must not contain null values", "internalTags");
            }
            validated.add(tag);
        }
        return validated;
    }

    public Long getId() {
        return id;
    }

    public int getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public String getSpriteUrl() {
        return spriteUrl;
    }

    public String getCategory() {
        return category;
    }

    public int getMass() {
        return mass;
    }

    public List<String> getAbilities() {
        return abilities;
    }

    public String getLocalName() {
        return localName;
    }

    public String getRegion() {
        return region;
    }

    public List<String> getInternalTags() {
        return internalTags;
    }
}
