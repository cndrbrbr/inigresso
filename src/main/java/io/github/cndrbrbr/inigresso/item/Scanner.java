package io.github.cndrbrbr.inigresso.item;

import io.github.cndrbrbr.inigresso.render.ScannerRenderer;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Das Scanner-Item: eine gefüllte Karte, deren MapView nur unseren ScannerRenderer hat.
 * Alle Scanner teilen sich eine Karten-ID (Renderer ist kontextbezogen). Die ID steht in
 * der state-Tabelle; Scanner mit veralteter ID werden beim Login umgestellt.
 */
public final class Scanner {

    private final NamespacedKey key;
    private final ScannerRenderer renderer;
    private MapView view;

    public Scanner(Plugin plugin, ScannerRenderer renderer) {
        this.key = new NamespacedKey(plugin, "scanner");
        this.renderer = renderer;
    }

    /**
     * Karte mit gespeicherter ID übernehmen oder neu anlegen.
     *
     * @return die verwendete Karten-ID (zum Speichern, falls neu)
     */
    @SuppressWarnings("deprecation") // Bukkit.getMap(int) ist der einzige Weg zu einer ID
    public int setup(long storedId) {
        MapView v = storedId >= 0 ? Bukkit.getMap((int) storedId) : null;
        if (v == null) {
            World w = Bukkit.getWorlds().get(0);
            v = Bukkit.createMap(w);
        }
        for (MapRenderer r : List.copyOf(v.getRenderers())) {
            v.removeRenderer(r);
        }
        v.addRenderer(renderer);
        v.setTrackingPosition(false);
        v.setUnlimitedTracking(false);
        v.setLocked(true);
        view = v;
        return v.getId();
    }

    public ItemStack create() {
        ItemStack stack = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) stack.getItemMeta();
        meta.setMapView(view);
        meta.setDisplayName(ChatColor.GOLD + "Scanner");
        meta.setLore(List.of(
                ChatColor.GRAY + "Zeigt Portale, Links und Felder",
                ChatColor.GRAY + "In die Nebenhand nehmen zum Laufen",
                ChatColor.GRAY + "Schleichen + Rechtsklick: Zoom",
                ChatColor.GOLD + "Inigresso"));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isScanner(ItemStack stack) {
        return stack != null && stack.getType() == Material.FILLED_MAP && stack.hasItemMeta()
                && stack.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public boolean has(Player p) {
        for (ItemStack s : p.getInventory().getContents()) {
            if (isScanner(s)) {
                return true;
            }
        }
        return false;
    }

    /** Gibt einen Scanner; false, wenn das Inventar voll war (dann liegt er auf dem Boden). */
    public boolean give(Player p) {
        var rest = p.getInventory().addItem(create());
        rest.values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        return rest.isEmpty();
    }

    /** Scanner mit fremder Karten-ID (z. B. nach Verlust der Weltdaten) auf die aktuelle umstellen. */
    public int fix(Player p) {
        int fixed = 0;
        ItemStack[] contents = p.getInventory().getContents();
        for (ItemStack s : contents) {
            if (!isScanner(s)) {
                continue;
            }
            MapMeta meta = (MapMeta) s.getItemMeta();
            if (!meta.hasMapView() || meta.getMapView() == null || meta.getMapView().getId() != view.getId()) {
                meta.setMapView(view);
                s.setItemMeta(meta);
                fixed++;
            }
        }
        return fixed;
    }
}
