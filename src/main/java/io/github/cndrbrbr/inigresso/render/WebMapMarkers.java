package io.github.cndrbrbr.inigresso.render;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.LineMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Line;
import de.bluecolored.bluemap.api.math.Shape;
import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.Field;
import io.github.cndrbrbr.inigresso.model.Link;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.service.LinkService;
import io.github.cndrbrbr.inigresso.service.PortalService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

/**
 * Portale, Links und Felder als Marker auf der BlueMap-Webkarte. Wird nur angelegt, wenn
 * BlueMap installiert ist (sonst fehlen die API-Klassen). Der Markersatz wird bei
 * Spieländerungen und spätestens alle {@link #FULL_SECONDS} s komplett neu gebaut und
 * atomar ersetzt; bei ein paar hundert Objekten ist das billig.
 */
public final class WebMapMarkers {

    public static final String SET_ID = "inigresso";
    static final int FULL_SECONDS = 60;

    private final Plugin plugin;
    private final PortalService portals;
    private final LinkService links;
    private final DoubleSupplier height;
    private boolean dirty = true;
    private int sinceFull;

    public WebMapMarkers(Plugin plugin, PortalService portals, LinkService links, DoubleSupplier height) {
        this.plugin = plugin;
        this.portals = portals;
        this.links = links;
        this.height = height;
    }

    public void start(GameEventBus bus) {
        bus.subscribe(GameEvent.class, e -> dirty = true);
        BlueMapAPI.onEnable(api -> Bukkit.getScheduler().runTask(plugin, () -> {
            dirty = true;
            refresh();
        }));
        Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 100L, 100L);
    }

    /** Alle 5 s: neu bauen, wenn sich etwas geändert hat oder die volle Minute um ist. */
    private void refresh() {
        sinceFull += 5;
        if (!dirty && sinceFull < FULL_SECONDS) {
            return;
        }
        Optional<BlueMapAPI> api = BlueMapAPI.getInstance();
        if (api.isEmpty()) {
            return;
        }
        dirty = false;
        sinceFull = 0;
        try {
            publish(api.get());
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Webkarte: Marker konnten nicht aktualisiert werden.", ex);
        }
    }

    private void publish(BlueMapAPI api) {
        double h = height.getAsDouble();
        Map<String, MarkerSet> sets = new HashMap<>();
        for (Portal p : portals.all()) {
            set(sets, p).put("portal-" + p.id(), POIMarker.builder()
                    .label(WebMapContent.label(p))
                    .detail(WebMapContent.detail(p))
                    .position(Vector3d.from(p.core().x() + 0.5, p.core().y() + 1, p.core().z() + 0.5))
                    .icon(WebMapContent.icon(p), Vector2i.from(11, 11))
                    .build());
        }
        for (Link l : links.links()) {
            Optional<Portal> a = portals.get(l.from());
            Optional<Portal> b = portals.get(l.to());
            if (a.isEmpty() || b.isEmpty()) {
                continue;
            }
            set(sets, a.get()).put("link-" + l.id(), LineMarker.builder()
                    .label("Link " + a.get().name() + " → " + b.get().name())
                    .line(new Line(top(a.get(), h), top(b.get(), h)))
                    .lineColor(color(l.faction(), 1f))
                    .lineWidth(3)
                    .depthTestEnabled(false)
                    .build());
        }
        for (Field f : links.fields()) {
            Optional<Portal> a = portals.get(f.a());
            Optional<Portal> b = portals.get(f.b());
            Optional<Portal> c = portals.get(f.c());
            if (a.isEmpty() || b.isEmpty() || c.isEmpty()) {
                continue;
            }
            double y = (a.get().core().y() + b.get().core().y() + c.get().core().y()) / 3.0 + h;
            set(sets, a.get()).put("field-" + f.id(), ShapeMarker.builder()
                    .label("Feld " + f.faction().displayName() + " (Einfluss " + f.influence() + ")")
                    .shape(new Shape(xz(a.get()), xz(b.get()), xz(c.get())), (float) y)
                    .fillColor(color(f.faction(), 0.25f))
                    .lineColor(color(f.faction(), 0.6f))
                    .lineWidth(2)
                    .depthTestEnabled(false)
                    .build());
        }
        // Für jede bekannte Welt ersetzen – auch leere, damit gelöschte Portale verschwinden.
        for (World w : Bukkit.getWorlds()) {
            MarkerSet set = sets.getOrDefault(w.getName(), newSet());
            api.getWorld(w).ifPresent(bw -> {
                for (BlueMapMap map : bw.getMaps()) {
                    map.getMarkerSets().put(SET_ID, set);
                }
            });
        }
    }

    private static MarkerSet newSet() {
        return MarkerSet.builder().label("Inigresso").toggleable(true).defaultHidden(false).build();
    }

    private static Map<String, de.bluecolored.bluemap.api.markers.Marker> set(Map<String, MarkerSet> sets, Portal p) {
        return sets.computeIfAbsent(p.core().world(), w -> newSet()).getMarkers();
    }

    private static Vector3d top(Portal p, double h) {
        return Vector3d.from(p.core().x() + 0.5, p.core().y() + h, p.core().z() + 0.5);
    }

    private static Vector2d xz(Portal p) {
        return Vector2d.from(p.core().x() + 0.5, p.core().z() + 0.5);
    }

    private static Color color(Faction f, float alpha) {
        return new Color(WebMapContent.rgb(f), alpha);
    }
}
