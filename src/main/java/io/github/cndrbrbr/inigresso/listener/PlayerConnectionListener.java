package io.github.cndrbrbr.inigresso.listener;

import io.github.cndrbrbr.inigresso.event.GameEvent;
import io.github.cndrbrbr.inigresso.event.GameEventBus;
import io.github.cndrbrbr.inigresso.model.PlayerProfile;
import io.github.cndrbrbr.inigresso.render.FactionTeams;
import io.github.cndrbrbr.inigresso.service.ProfileService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerConnectionListener implements Listener {

    private final ProfileService profiles;
    private final FactionTeams teams;

    public PlayerConnectionListener(ProfileService profiles, FactionTeams teams, GameEventBus bus) {
        this.profiles = profiles;
        this.teams = teams;
        bus.subscribe(GameEvent.LevelUp.class, e -> {
            Player p = Bukkit.getPlayer(e.profile().uuid());
            if (p != null) {
                p.sendTitle(ChatColor.GOLD + "Stufe " + e.newLevel(), ChatColor.YELLOW + "Inigresso", 10, 50, 20);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerProfile p = profiles.join(player.getUniqueId(), player.getName());
        teams.apply(p);
        if (!p.hasFaction()) {
            player.sendMessage(ChatColor.GOLD + "Willkommen bei Inigresso! " + ChatColor.YELLOW
                    + "Wähle deine Fraktion: " + ChatColor.GREEN + "/ig fraktion lumen" + ChatColor.YELLOW
                    + " oder " + ChatColor.BLUE + "/ig fraktion waechter" + ChatColor.GRAY + " (gilt dauerhaft)");
        }
    }
}
