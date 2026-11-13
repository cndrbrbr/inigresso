package io.github.cndrbrbr.inigresso.service;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Alle Spielerprofile im Speicher, über UUID (rules.md §1). */
public final class ProfileService {

    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();
    private final GameEventBus bus;

    public ProfileService(GameEventBus bus, Collection<PlayerProfile> loaded) {
        this.bus = bus;
        for (PlayerProfile p : loaded) {
            profiles.put(p.uuid(), p);
        }
    }

    /** Beim Join: Profil holen oder anlegen, Namensänderung übernehmen. */
    public PlayerProfile join(UUID uuid, String name) {
        PlayerProfile p = profiles.get(uuid);
        if (p == null) {
            p = new PlayerProfile(uuid, name, null, 0);
            profiles.put(uuid, p);
            bus.publish(new GameEvent.ProfileChanged(p));
        } else if (!p.lastName().equals(name)) {
            p.setLastName(name);
            bus.publish(new GameEvent.ProfileChanged(p));
        }
        return p;
    }

    public Optional<PlayerProfile> get(UUID uuid) {
        return Optional.ofNullable(profiles.get(uuid));
    }

    /** Suche über den zuletzt bekannten Namen, ohne Groß-/Kleinschreibung. */
    public Optional<PlayerProfile> byName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return profiles.values().stream()
                .filter(p -> p.lastName().toLowerCase(Locale.ROOT).equals(n))
                .findFirst();
    }

    public Collection<PlayerProfile> all() {
        return Collections.unmodifiableCollection(profiles.values());
    }

    public long count(Faction f) {
        return profiles.values().stream().filter(p -> p.faction() == f).count();
    }
}
