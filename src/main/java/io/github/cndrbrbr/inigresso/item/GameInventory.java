package io.github.cndrbrbr.inigresso.item;

import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.GameItem;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.IntSupplier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Spielgegenstände im normalen Spielerinventar: Zählen, Geben mit Obergrenze, Entfernen.
 * Gegenstände, die in Truhen liegen, zählen nicht mit.
 */
public final class GameInventory {

    private final ItemFactory items;
    private final IntSupplier maxItems;

    public GameInventory(ItemFactory items, IntSupplier maxItems) {
        this.items = items;
        this.maxItems = maxItems;
    }

    public int count(Player p) {
        int n = 0;
        for (ItemStack s : p.getInventory().getContents()) {
            if (items.isGameItem(s)) {
                n += s.getAmount();
            }
        }
        return n;
    }

    public int max() {
        return maxItems.getAsInt();
    }

    /**
     * Gibt Gegenstände bis zur Obergrenze. Was nicht ins Inventar passt, wird nicht gedroppt,
     * sondern verfällt. Rückgabe: Anzahl tatsächlich erhaltener Gegenstände.
     */
    public int give(Player p, List<GameItem> loot) {
        int room = max() - count(p);
        int given = 0;
        for (GameItem g : loot) {
            if (room <= 0) {
                break;
            }
            Map<Integer, ItemStack> rest = p.getInventory().addItem(items.create(g, 1));
            if (!rest.isEmpty()) {
                break;
            }
            room--;
            given++;
        }
        return given;
    }

    public int give(Player p, GameItem g, int amount) {
        return give(p, java.util.Collections.nCopies(amount, g));
    }

    /** Ein Stück aus der Haupthand verbrauchen. */
    public void consumeMainHand(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getAmount() <= 1) {
            p.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
    }

    /** Ein Stück eines bestimmten Gegenstands irgendwo im Inventar verbrauchen. */
    public boolean consume(Player p, GameItem wanted) {
        PlayerInventory inv = p.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            Optional<GameItem> g = items.read(contents[i]);
            if (g.isPresent() && g.get().equals(wanted)) {
                if (contents[i].getAmount() <= 1) {
                    inv.setItem(i, null);
                } else {
                    contents[i].setAmount(contents[i].getAmount() - 1);
                }
                return true;
            }
        }
        return false;
    }

    /** Entfernt alle Spielgegenstände einer anderen Fraktion. Rückgabe: Anzahl entfernter Stücke. */
    public int purgeForeign(Player p, Faction keep) {
        int removed = 0;
        PlayerInventory inv = p.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            Optional<GameItem> g = items.read(contents[i]);
            if (g.isPresent() && g.get().faction() != keep) {
                removed += contents[i].getAmount();
                inv.setItem(i, null);
            }
        }
        return removed;
    }

    /** Übersicht: Typ/Stufe bzw. Schlüssel → Anzahl, sortiert. */
    public Map<GameItem, Integer> summary(Player p) {
        Map<GameItem, Integer> out = new TreeMap<>((a, b) -> {
            int c = a.type().compareTo(b.type());
            if (c != 0) {
                return c;
            }
            c = Integer.compare(a.level(), b.level());
            return c != 0 ? c : Integer.compare(a.portalId(), b.portalId());
        });
        for (ItemStack s : p.getInventory().getContents()) {
            items.read(s).ifPresent(g -> out.merge(new GameItem(g.type(), g.level(), g.portalId(), Faction.NEUTRAL),
                    s.getAmount(), Integer::sum));
        }
        return out;
    }
}
