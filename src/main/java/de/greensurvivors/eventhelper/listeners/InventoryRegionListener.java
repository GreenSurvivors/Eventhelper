package de.greensurvivors.eventhelper.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import de.greensurvivors.eventhelper.Eventhelper;
import de.greensurvivors.eventhelper.config.InventoryConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class InventoryRegionListener implements Listener {
    private final HashMap<UUID, String> playerInventoryCache = new HashMap<>();

    private static InventoryRegionListener instance;
    public static StringFlag inventory_identifier;

    public static InventoryRegionListener inst() {
        if (instance == null)
            instance = new InventoryRegionListener();
        return instance;
    }

    public InventoryRegionListener() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        try {
            // create a flag with the name "inventory-identifier", defaulting to "default"
            StringFlag flag = new StringFlag("inventory-identifier", InventoryConfig.inst().getDefaultIdentifier());
            registry.register(flag);
            inventory_identifier = flag; // only set our field if there was no error
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            // you can use the existing flag, but this may cause conflicts - be sure to check type
            Flag<?> existing = registry.get("inventory-identifier");
            if (existing instanceof StringFlag) {
                inventory_identifier = (StringFlag) existing;
            } else {
                inventory_identifier = null;
                // types don't match - this is bad news! some other plugin conflicts with you
                // hopefully this never actually happens
                Eventhelper.getPlugin().getLogger().log(Level.WARNING, "couldn't enable Flag \"cinventory-identifier\". Might conflict with other plugin.");
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerInventoryCache.put(event.getPlayer().getUniqueId(), InventoryConfig.inst().loadIdentifier(event.getPlayer()));
    }

    private void updateInventory(Player buk_player, com.sk89q.worldedit.util.Location we_location) {
        LocalPlayer we_Player = WorldGuardPlugin.inst().wrapPlayer(buk_player);

        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        String identifier = query.getApplicableRegions(we_location).queryValue(we_Player, inventory_identifier);

        String oldIndentifier = playerInventoryCache.get(buk_player.getUniqueId());
        playerInventoryCache.putIfAbsent(buk_player.getUniqueId(), identifier);

        if (!Objects.equals(oldIndentifier, identifier)) {
            if (oldIndentifier != null) {
                InventoryConfig.inst().savePlayerData(buk_player, oldIndentifier);
            }
            InventoryConfig.inst().loadPlayerData(buk_player, identifier);

            playerInventoryCache.put(buk_player.getUniqueId(), identifier);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.isCancelled()) {
            if (inventory_identifier == null) {
                return;
            }

            updateInventory(event.getPlayer(), BukkitAdapter.adapt(event.getTo()));
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.isCancelled()) {
            if (inventory_identifier == null) {
                return;
            }

            updateInventory(event.getPlayer(), BukkitAdapter.adapt(event.getTo()));
        }
    }
}
