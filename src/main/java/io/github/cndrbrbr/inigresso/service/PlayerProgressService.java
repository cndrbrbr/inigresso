package io.github.cndrbrbr.inigresso.service;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;

/** Aktionspunkte und Spielerstufe nach rules.md §9. */
public final class PlayerProgressService {

    /** AP-Schwelle für Stufe i+1 (Index 0 = Stufe 1). */
    static final long[] THRESHOLDS = {0, 2_500, 20_000, 70_000, 150_000, 300_000, 600_000, 1_200_000};

    public static final int MAX_LEVEL = THRESHOLDS.length;

    /** AP-Werte aus rules.md §9. */
    public enum Action {
        DEPLOY_RESONATOR(125),
        CAPTURE_PORTAL(500),
        DESTROY_RESONATOR(75),
        CREATE_LINK(313),
        CREATE_FIELD(1250),
        HACK_OWN(100),
        HACK_ENEMY(150),
        RECHARGE(10),
        PROPOSAL_ACCEPTED(1000);

        private final int ap;

        Action(int ap) {
            this.ap = ap;
        }

        public int ap() {
            return ap;
        }
    }

    private final GameEventBus bus;

    public PlayerProgressService(GameEventBus bus) {
        this.bus = bus;
    }

    public static int levelFor(long ap) {
        int level = 1;
        for (int i = 1; i < THRESHOLDS.length; i++) {
            if (ap >= THRESHOLDS[i]) {
                level = i + 1;
            }
        }
        return level;
    }

    /** AP bis zur nächsten Stufe, -1 auf Höchststufe. */
    public static long apToNextLevel(long ap) {
        int level = levelFor(ap);
        return level >= MAX_LEVEL ? -1 : THRESHOLDS[level] - ap;
    }

    public void award(PlayerProfile p, Action action) {
        add(p, action.ap());
    }

    /** Fügt AP hinzu (negativ nur für Admin-Korrekturen) und meldet Stufenaufstieg. */
    public void add(PlayerProfile p, long amount) {
        int before = levelFor(p.ap());
        p.setAp(p.ap() + amount);
        int after = levelFor(p.ap());
        bus.publish(new GameEvent.ProfileChanged(p));
        if (after > before) {
            bus.publish(new GameEvent.LevelUp(p, after));
        }
    }
}
