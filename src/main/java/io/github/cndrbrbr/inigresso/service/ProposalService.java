package io.github.cndrbrbr.inigresso.service;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.model.Proposal;
import java.time.Clock;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Portalvorschläge prüfen und entscheiden (rules.md §2.1). */
public final class ProposalService {

    public static final int MIN_NAME = 3;
    public static final int MAX_NAME = 32;

    /** Einstellungen, die sich per reload ändern können. */
    public record Rules(List<String> worlds, int minDistance, int minLevel, int maxOpen) {
    }

    public enum Problem {
        NO_FACTION, LEVEL_TOO_LOW, TOO_MANY_OPEN, WORLD_NOT_ALLOWED, NAME_INVALID, NAME_TAKEN,
        TOO_CLOSE_TO_PORTAL, TOO_CLOSE_TO_PROPOSAL
    }

    /** Ergebnis von propose: entweder ein Vorschlag oder ein Problem (+ betroffener Name). */
    public record Result(Proposal proposal, Problem problem, String detail) {
        public boolean ok() {
            return proposal != null;
        }
    }

    private final Map<Integer, Proposal> proposals = new HashMap<>();
    private final GameEventBus bus;
    private final Clock clock;
    private final Supplier<Rules> rules;
    private final PortalService portals;
    private final PlayerProgressService progress;
    private final ProfileService profiles;
    private int nextId;

    public ProposalService(GameEventBus bus, Clock clock, Supplier<Rules> rules, PortalService portals,
                           PlayerProgressService progress, ProfileService profiles, Collection<Proposal> loaded) {
        this.bus = bus;
        this.clock = clock;
        this.rules = rules;
        this.portals = portals;
        this.progress = progress;
        this.profiles = profiles;
        for (Proposal p : loaded) {
            proposals.put(p.id(), p);
            nextId = Math.max(nextId, p.id());
        }
    }

    public Result propose(PlayerProfile by, String rawName, BlockPos pos) {
        Rules r = rules.get();
        String name = rawName.strip().replaceAll("\\s+", " ");
        if (!by.hasFaction()) {
            return fail(Problem.NO_FACTION, null);
        }
        if (PlayerProgressService.levelFor(by.ap()) < r.minLevel()) {
            return fail(Problem.LEVEL_TOO_LOW, String.valueOf(r.minLevel()));
        }
        if (!r.worlds().contains(pos.world())) {
            return fail(Problem.WORLD_NOT_ALLOWED, pos.world());
        }
        if (open().stream().filter(p -> p.proposer().equals(by.uuid())).count() >= r.maxOpen()) {
            return fail(Problem.TOO_MANY_OPEN, String.valueOf(r.maxOpen()));
        }
        if (name.length() < MIN_NAME || name.length() > MAX_NAME || name.startsWith("#")
                || name.chars().allMatch(Character::isDigit) || name.chars().anyMatch(c -> c == '§')) {
            return fail(Problem.NAME_INVALID, null);
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (portals.byName(name).isPresent()
                || open().stream().anyMatch(p -> p.name().toLowerCase(Locale.ROOT).equals(lower))) {
            return fail(Problem.NAME_TAKEN, name);
        }
        Optional<Portal> nearPortal = portals.nearest(pos, r.minDistance() - 0.0001);
        if (nearPortal.isPresent()) {
            return fail(Problem.TOO_CLOSE_TO_PORTAL, nearPortal.get().name());
        }
        Optional<Proposal> nearProposal = open().stream()
                .filter(p -> p.pos().horizontalDistance(pos) < r.minDistance()).findFirst();
        if (nearProposal.isPresent()) {
            return fail(Problem.TOO_CLOSE_TO_PROPOSAL, nearProposal.get().name());
        }
        Proposal p = new Proposal(++nextId, name, pos, by.uuid(), clock.millis(), Proposal.Status.OFFEN,
                null, 0, null);
        proposals.put(p.id(), p);
        bus.publish(new GameEvent.ProposalChanged(p));
        return new Result(p, null, null);
    }

    private static Result fail(Problem problem, String detail) {
        return new Result(null, problem, detail);
    }

    /**
     * Nimmt an und legt das Portal an. Abstand und Name werden erneut geprüft, weil seit dem
     * Vorschlag ein anderer angenommen worden sein kann.
     */
    public Result accept(Proposal p, String admin) {
        requireOpen(p);
        Optional<Portal> near = portals.nearest(p.pos(), rules.get().minDistance() - 0.0001);
        if (near.isPresent()) {
            return fail(Problem.TOO_CLOSE_TO_PORTAL, near.get().name());
        }
        if (portals.byName(p.name()).isPresent()) {
            return fail(Problem.NAME_TAKEN, p.name());
        }
        p.decide(Proposal.Status.ANGENOMMEN, admin, clock.millis(), null);
        bus.publish(new GameEvent.ProposalChanged(p));
        portals.create(p.name(), p.pos(), p.proposer());
        profiles.get(p.proposer()).ifPresent(pr -> progress.award(pr, PlayerProgressService.Action.PROPOSAL_ACCEPTED));
        bus.publish(new GameEvent.Notify(p.proposer(), "§aDein Portalvorschlag §f" + p.name()
                + "§a wurde angenommen! §7(+" + PlayerProgressService.Action.PROPOSAL_ACCEPTED.ap() + " AP)"));
        return new Result(p, null, null);
    }

    public void reject(Proposal p, String admin, String reason) {
        requireOpen(p);
        p.decide(Proposal.Status.ABGELEHNT, admin, clock.millis(), reason);
        bus.publish(new GameEvent.ProposalChanged(p));
        bus.publish(new GameEvent.Notify(p.proposer(), "§cDein Portalvorschlag §f" + p.name()
                + "§c wurde abgelehnt: §f" + reason));
    }

    private static void requireOpen(Proposal p) {
        if (!p.isOpen()) {
            throw new IllegalStateException("Vorschlag " + p.id() + " ist schon entschieden");
        }
    }

    public Optional<Proposal> get(int id) {
        return Optional.ofNullable(proposals.get(id));
    }

    /** Offene Vorschläge, älteste zuerst. */
    public List<Proposal> open() {
        return proposals.values().stream().filter(Proposal::isOpen)
                .sorted(Comparator.comparingInt(Proposal::id)).toList();
    }
}
