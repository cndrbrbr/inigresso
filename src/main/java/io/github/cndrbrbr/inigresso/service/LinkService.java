package io.github.cndrbrbr.inigresso.service;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.geo.Geometry;
import io.github.cndrbrbr.inigresso.geo.Segment;
import io.github.cndrbrbr.inigresso.geo.Triangle;
import io.github.cndrbrbr.inigresso.geo.Vec2;
import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.Field;
import io.github.cndrbrbr.inigresso.model.Link;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.model.Portal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Links und Felder (rules.md §6, §7). */
public final class LinkService {

    /** Wie nah man am Ausgangsportal stehen muss. */
    public static final double LINK_DISTANCE = 6;

    public record Rules(int maxRange, int maxOutgoing) {
    }

    public enum Problem {
        NO_FACTION, NOT_AT_PORTAL, SOURCE_NOT_OWN, SOURCE_NOT_FULL, SAME_PORTAL, TARGET_NOT_OWN,
        OTHER_WORLD, TOO_FAR, ALREADY_LINKED, TOO_MANY_OUTGOING, CROSSES, SOURCE_IN_ENEMY_FIELD, NO_KEY
    }

    /** Ergebnis: link + entstandene Felder, oder Problem (detail z. B. Reichweite oder gekreuzter Link). */
    public record Result(Link link, List<Field> fields, Problem problem, String detail) {
        public boolean ok() {
            return problem == null;
        }
    }

    private final Map<Integer, Link> links = new LinkedHashMap<>();
    private final Map<Integer, Field> fields = new LinkedHashMap<>();
    private final PortalService portals;
    private final PlayerProgressService progress;
    private final GameEventBus bus;
    private final Clock clock;
    private final Supplier<Rules> rules;
    private int nextLinkId;
    private int nextFieldId;

    public LinkService(PortalService portals, PlayerProgressService progress, GameEventBus bus, Clock clock,
                       Supplier<Rules> rules, Collection<Link> loadedLinks, Collection<Field> loadedFields) {
        this.portals = portals;
        this.progress = progress;
        this.bus = bus;
        this.clock = clock;
        this.rules = rules;
        for (Link l : loadedLinks) {
            links.put(l.id(), l);
            nextLinkId = Math.max(nextLinkId, l.id());
        }
        for (Field f : loadedFields) {
            fields.put(f.id(), f);
            nextFieldId = Math.max(nextFieldId, f.id());
        }
        bus.subscribe(GameEvent.PortalChanged.class, e -> {
            if (e.portal().owner() == Faction.NEUTRAL || e.moved()) {
                removeLinksOf(e.portal().id());
            }
        });
        bus.subscribe(GameEvent.PortalRemoved.class, e -> removeLinksOf(e.portal().id()));
    }

    /** Reichweite nach rules.md §6: 64 × Stufe², gekappt. */
    public int range(Portal p) {
        int level = p.level();
        return Math.min(64 * level * level, rules.get().maxRange());
    }

    /** Prüft alle Bedingungen außer dem Schlüssel. */
    public Optional<Result> check(PlayerProfile by, BlockPos playerPos, Portal from, Portal to) {
        Rules r = rules.get();
        if (!by.hasFaction()) {
            return fail(Problem.NO_FACTION, null);
        }
        if (from.core().horizontalDistance(playerPos) > LINK_DISTANCE) {
            return fail(Problem.NOT_AT_PORTAL, null);
        }
        if (from.owner() != by.faction()) {
            return fail(Problem.SOURCE_NOT_OWN, null);
        }
        if (from.resonatorCount() < Portal.SLOTS) {
            return fail(Problem.SOURCE_NOT_FULL, String.valueOf(from.resonatorCount()));
        }
        if (from.id() == to.id()) {
            return fail(Problem.SAME_PORTAL, null);
        }
        if (to.owner() != by.faction()) {
            return fail(Problem.TARGET_NOT_OWN, null);
        }
        if (!from.core().world().equals(to.core().world())) {
            return fail(Problem.OTHER_WORLD, null);
        }
        double dist = from.core().horizontalDistance(to.core());
        if (dist > range(from)) {
            return fail(Problem.TOO_FAR, (int) Math.ceil(dist) + "/" + range(from));
        }
        if (links.values().stream().anyMatch(l -> l.connects(from.id(), to.id()))) {
            return fail(Problem.ALREADY_LINKED, null);
        }
        if (links.values().stream().filter(l -> l.from() == from.id()).count() >= r.maxOutgoing()) {
            return fail(Problem.TOO_MANY_OUTGOING, String.valueOf(r.maxOutgoing()));
        }
        Segment s = new Segment(from.core().xz(), to.core().xz());
        String world = from.core().world();
        for (Link l : links.values()) {
            Optional<Segment> other = segment(l);
            if (other.isPresent() && world.equals(worldOf(l)) && Geometry.crosses(s, other.get())) {
                return fail(Problem.CROSSES, name(l.from()) + " – " + name(l.to()));
            }
        }
        // Ein Portal auf der Strecke, das nicht Endpunkt ist, würde der Link durchqueren.
        for (Portal p : portals.all()) {
            if (p.id() != from.id() && p.id() != to.id() && p.core().world().equals(world)
                    && Geometry.crosses(s, new Segment(p.core().xz(), p.core().xz()))) {
                return fail(Problem.CROSSES, p.name());
            }
        }
        for (Field f : fields.values()) {
            if (f.faction() != by.faction() && world.equals(worldOfField(f))) {
                Optional<Triangle> t = triangle(f);
                if (t.isPresent() && Geometry.strictlyContains(t.get(), from.core().xz())) {
                    return fail(Problem.SOURCE_IN_ENEMY_FIELD, null);
                }
            }
        }
        return Optional.empty();
    }

    /** Legt den Link an (Schlüssel muss der Aufrufer geprüft und verbraucht haben). */
    public Result link(PlayerProfile by, BlockPos playerPos, Portal from, Portal to) {
        Optional<Result> problem = check(by, playerPos, from, to);
        if (problem.isPresent()) {
            return problem.get();
        }
        Link link = new Link(++nextLinkId, from.id(), to.id(), by.faction(), by.uuid(), clock.millis());
        links.put(link.id(), link);
        bus.publish(new GameEvent.LinkCreated(link));
        progress.award(by, PlayerProgressService.Action.CREATE_LINK);
        List<Field> created = createFields(by, link);
        return new Result(link, created, null, null);
    }

    /**
     * Neue Felder am Link A–B: alle C, die mit A und B verlinkt sind, nach Seite trennen und
     * je Seite das größte Dreieck nehmen (rules.md §7).
     */
    private List<Field> createFields(PlayerProfile by, Link link) {
        Portal a = portals.get(link.from()).orElseThrow();
        Portal b = portals.get(link.to()).orElseThrow();
        Set<Integer> nA = neighbours(a.id(), link.faction());
        Set<Integer> nB = neighbours(b.id(), link.faction());
        Portal bestLeft = null;
        Portal bestRight = null;
        long areaLeft = 0;
        long areaRight = 0;
        for (int cId : nA) {
            if (!nB.contains(cId)) {
                continue;
            }
            Portal c = portals.get(cId).orElse(null);
            if (c == null) {
                continue;
            }
            Triangle t = new Triangle(a.core().xz(), b.core().xz(), c.core().xz());
            long doubled = t.doubleSignedArea();
            if (doubled > 0 && doubled > areaLeft) {
                areaLeft = doubled;
                bestLeft = c;
            } else if (doubled < 0 && -doubled > areaRight) {
                areaRight = -doubled;
                bestRight = c;
            }
        }
        List<Field> out = new ArrayList<>();
        for (Portal c : new Portal[] {bestLeft, bestRight}) {
            if (c == null) {
                continue;
            }
            Triangle t = new Triangle(a.core().xz(), b.core().xz(), c.core().xz());
            Field f = new Field(++nextFieldId, a.id(), b.id(), c.id(), link.faction(), Geometry.influence(t),
                    by.uuid(), clock.millis());
            fields.put(f.id(), f);
            out.add(f);
            bus.publish(new GameEvent.FieldCreated(f));
            progress.award(by, PlayerProgressService.Action.CREATE_FIELD);
        }
        return out;
    }

    private Set<Integer> neighbours(int portalId, Faction f) {
        return links.values().stream().filter(l -> l.faction() == f && l.touches(portalId))
                .map(l -> l.other(portalId)).collect(Collectors.toSet());
    }

    /** Entfernt alle Links eines Portals und alle Felder daran. */
    public void removeLinksOf(int portalId) {
        List<Link> gone = links.values().stream().filter(l -> l.touches(portalId)).toList();
        for (Link l : gone) {
            removeLink(l);
        }
    }

    public void removeLink(Link l) {
        if (links.remove(l.id()) == null) {
            return;
        }
        List<Field> gone = fields.values().stream().filter(f -> f.hasEdge(l.from(), l.to())).toList();
        for (Field f : gone) {
            fields.remove(f.id());
            bus.publish(new GameEvent.FieldRemoved(f));
        }
        bus.publish(new GameEvent.LinkRemoved(l));
    }

    public Collection<Link> links() {
        return Collections.unmodifiableCollection(links.values());
    }

    public Collection<Field> fields() {
        return Collections.unmodifiableCollection(fields.values());
    }

    public List<Link> linksOf(int portalId) {
        return links.values().stream().filter(l -> l.touches(portalId)).toList();
    }

    public Optional<Segment> segment(Link l) {
        Optional<Portal> a = portals.get(l.from());
        Optional<Portal> b = portals.get(l.to());
        if (a.isEmpty() || b.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Segment(a.get().core().xz(), b.get().core().xz()));
    }

    public Optional<Triangle> triangle(Field f) {
        Optional<Portal> a = portals.get(f.a());
        Optional<Portal> b = portals.get(f.b());
        Optional<Portal> c = portals.get(f.c());
        if (a.isEmpty() || b.isEmpty() || c.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Triangle(a.get().core().xz(), b.get().core().xz(), c.get().core().xz()));
    }

    /** Felder, unter denen der Punkt liegt (Rand zählt). */
    public List<Field> fieldsAt(String world, Vec2 p) {
        List<Field> out = new ArrayList<>();
        for (Field f : fields.values()) {
            if (world.equals(worldOfField(f))) {
                triangle(f).filter(t -> Geometry.contains(t, p)).ifPresent(t -> out.add(f));
            }
        }
        return out;
    }

    /** Summe des Einflusses aller Felder einer Fraktion (rules.md §8). */
    public long influence(Faction f) {
        return fields.values().stream().filter(x -> x.faction() == f).mapToLong(Field::influence).sum();
    }

    private String worldOf(Link l) {
        return portals.get(l.from()).map(p -> p.core().world()).orElse("");
    }

    private String worldOfField(Field f) {
        return portals.get(f.a()).map(p -> p.core().world()).orElse("");
    }

    private String name(int portalId) {
        return portals.get(portalId).map(Portal::name).orElse("#" + portalId);
    }

    private static Optional<Result> fail(Problem p, String detail) {
        return Optional.of(new Result(null, List.of(), p, detail));
    }
}
