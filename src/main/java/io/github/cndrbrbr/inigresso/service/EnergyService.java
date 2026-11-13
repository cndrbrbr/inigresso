package io.github.cndrbrbr.inigresso.service;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.model.Resonator;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

/** Täglicher Verfall und Aufladen (rules.md §5). */
public final class EnergyService {

    /** Höchstens so viel Energie pro Aufladevorgang (verhindert, dass ein Klick alle XP frisst). */
    public static final int MAX_PER_RECHARGE = 1000;
    /** Energie pro XP-Punkt. */
    public static final int ENERGY_PER_XP = 10;

    public record DecayRules(int percentPerDay, LocalTime time) {
    }

    private final PortalService portals;
    private final PlayerProgressService progress;
    private final GameEventBus bus;
    private final Clock clock;
    private final Supplier<DecayRules> rules;
    private final LongConsumer saveLastDecay;
    private long lastDecay;

    /**
     * @param lastDecay Zeitpunkt des letzten Verfalls (ms), 0 = noch nie; dann zählt der
     *                  Start als letzter Verfall, damit nichts rückwirkend verfällt.
     */
    public EnergyService(PortalService portals, PlayerProgressService progress, GameEventBus bus, Clock clock,
                         Supplier<DecayRules> rules, long lastDecay, LongConsumer saveLastDecay) {
        this.portals = portals;
        this.progress = progress;
        this.bus = bus;
        this.clock = clock;
        this.rules = rules;
        this.saveLastDecay = saveLastDecay;
        this.lastDecay = lastDecay > 0 ? lastDecay : clock.millis();
        if (lastDecay <= 0) {
            saveLastDecay.accept(this.lastDecay);
        }
    }

    /**
     * Wendet alle seit dem letzten Mal fälligen Verfallszeitpunkte an (auch nachgeholt,
     * wenn der Server aus war). Regelmäßig aufrufen. Rückgabe: Anzahl angewendeter Tage.
     */
    public int tick() {
        DecayRules r = rules.get();
        ZonedDateTime last = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(lastDecay), clock.getZone());
        ZonedDateTime next = last.with(r.time());
        if (!next.isAfter(last)) {
            next = next.plusDays(1);
        }
        long now = clock.millis();
        int days = 0;
        while (next.toInstant().toEpochMilli() <= now) {
            days++;
            lastDecay = next.toInstant().toEpochMilli();
            next = next.plusDays(1);
        }
        if (days > 0) {
            decay(days * r.percentPerDay());
            saveLastDecay.accept(lastDecay);
        }
        return days;
    }

    /** Jeder Resonator verliert percent % seiner maximalen Energie; leere werden zerstört. */
    void decay(int percent) {
        for (Portal p : java.util.List.copyOf(portals.all())) {
            if (p.resonatorCount() == 0) {
                continue;
            }
            for (int i = 0; i < Portal.SLOTS; i++) {
                Resonator res = p.resonator(i);
                if (res != null) {
                    res.setEnergy(res.energy() - (int) Math.ceil(res.maxEnergy() * percent / 100.0));
                    if (res.energy() == 0) {
                        p.setResonator(i, null);
                    }
                }
            }
            if (p.resonatorCount() == 0) {
                portals.neutralize(p);
            } else {
                bus.publish(new GameEvent.PortalChanged(p, p.core(), false));
            }
        }
    }

    /** Fehlende Energie aller Resonatoren. */
    public static int deficit(Portal p) {
        int d = 0;
        for (int i = 0; i < Portal.SLOTS; i++) {
            Resonator r = p.resonator(i);
            if (r != null) {
                d += r.maxEnergy() - r.energy();
            }
        }
        return d;
    }

    /** Energie in Prozent (0–100) über alle Resonatoren. */
    public static int percent(Portal p) {
        int max = 0;
        int cur = 0;
        for (int i = 0; i < Portal.SLOTS; i++) {
            Resonator r = p.resonator(i);
            if (r != null) {
                max += r.maxEnergy();
                cur += r.energy();
            }
        }
        return max == 0 ? 0 : (int) Math.round(100.0 * cur / max);
    }

    /**
     * Lädt ein eigenes Portal mit höchstens budget Energie auf, verteilt auf die schwächsten
     * Resonatoren zuerst. Rückgabe: tatsächlich verbrauchte Energie (0 = nichts zu tun/fremd).
     */
    public int recharge(PlayerProfile by, Portal p, int budget) {
        if (!by.hasFaction() || p.owner() != by.faction() || budget <= 0) {
            return 0;
        }
        int left = Math.min(budget, MAX_PER_RECHARGE);
        int used = 0;
        while (left > 0) {
            Resonator weakest = null;
            for (int i = 0; i < Portal.SLOTS; i++) {
                Resonator r = p.resonator(i);
                if (r != null && r.energy() < r.maxEnergy()
                        && (weakest == null || (double) r.energy() / r.maxEnergy()
                        < (double) weakest.energy() / weakest.maxEnergy())) {
                    weakest = r;
                }
            }
            if (weakest == null) {
                break;
            }
            int step = Math.min(left, Math.min(100, weakest.maxEnergy() - weakest.energy()));
            weakest.setEnergy(weakest.energy() + step);
            left -= step;
            used += step;
        }
        if (used > 0) {
            progress.award(by, PlayerProgressService.Action.RECHARGE);
            bus.publish(new GameEvent.PortalChanged(p, p.core(), false));
        }
        return used;
    }

    public long lastDecay() {
        return lastDecay;
    }
}
