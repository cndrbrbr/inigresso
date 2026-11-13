package io.github.cndrbrbr.inigresso.item;

import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.GameItem;
import io.github.cndrbrbr.inigresso.service.PortalService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Erzeugt und erkennt markierte Spielgegenstände (rules.md §3). */
public final class ItemFactory {

    /** Farben der Stufen 1–8 für Namen. */
    private static final ChatColor[] LEVEL_COLORS = {
        ChatColor.YELLOW, ChatColor.GOLD, ChatColor.RED, ChatColor.LIGHT_PURPLE,
        ChatColor.DARK_PURPLE, ChatColor.BLUE, ChatColor.AQUA, ChatColor.WHITE
    };

    private final NamespacedKey typeKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey portalKey;
    private final NamespacedKey factionKey;
    private final PortalService portals;

    public ItemFactory(Plugin plugin, PortalService portals) {
        this.typeKey = new NamespacedKey(plugin, "type");
        this.levelKey = new NamespacedKey(plugin, "level");
        this.portalKey = new NamespacedKey(plugin, "portal");
        this.factionKey = new NamespacedKey(plugin, "faction");
        this.portals = portals;
    }

    public static ChatColor levelColor(int level) {
        return LEVEL_COLORS[Math.clamp(level, 1, 8) - 1];
    }

    public ItemStack create(GameItem item, int amount) {
        Material m = switch (item.type()) {
            case RESONATOR -> Material.AMETHYST_SHARD;
            case BURST -> Material.FIRE_CHARGE;
            case KEY -> Material.TRIAL_KEY;
        };
        ItemStack stack = new ItemStack(m, amount);
        ItemMeta meta = stack.getItemMeta();
        List<String> lore = new ArrayList<>();
        switch (item.type()) {
            case RESONATOR -> {
                meta.setDisplayName(levelColor(item.level()) + "Resonator L" + item.level());
                lore.add(ChatColor.GRAY + "Rechtsklick auf einen Portalkern:");
                lore.add(ChatColor.GRAY + "setzen oder aufrüsten");
            }
            case BURST -> {
                meta.setDisplayName(levelColor(item.level()) + "Burst L" + item.level());
                lore.add(ChatColor.GRAY + "Rechtsklick: Schaden an gegnerischen");
                lore.add(ChatColor.GRAY + "Resonatoren im Umkreis");
            }
            case KEY -> {
                String name = portals.get(item.portalId()).map(p -> p.name()).orElse("#" + item.portalId());
                meta.setDisplayName(ChatColor.AQUA + "Schlüssel: " + name);
                lore.add(ChatColor.GRAY + "Portal #" + item.portalId());
                lore.add(ChatColor.GRAY + "Nötig für einen Link zu diesem Portal");
            }
        }
        lore.add(item.faction().color() + "Inigresso · " + item.faction().displayName());
        meta.setLore(lore);
        meta.setEnchantmentGlintOverride(item.type() != GameItem.Type.KEY);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(typeKey, PersistentDataType.STRING, item.type().name());
        pdc.set(levelKey, PersistentDataType.INTEGER, item.level());
        pdc.set(portalKey, PersistentDataType.INTEGER, item.portalId());
        pdc.set(factionKey, PersistentDataType.STRING, item.faction().name());
        stack.setItemMeta(meta);
        return stack;
    }

    public Optional<GameItem> read(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return Optional.empty();
        }
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        String type = pdc.get(typeKey, PersistentDataType.STRING);
        if (type == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new GameItem(GameItem.Type.valueOf(type),
                    pdc.getOrDefault(levelKey, PersistentDataType.INTEGER, 0),
                    pdc.getOrDefault(portalKey, PersistentDataType.INTEGER, 0),
                    Faction.valueOf(pdc.getOrDefault(factionKey, PersistentDataType.STRING, "NEUTRAL"))));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public boolean isGameItem(ItemStack stack) {
        return read(stack).isPresent();
    }
}
