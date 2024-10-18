package de.greensurvivors.eventhelper.modules.inventory;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.AModul;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class InventoryRegionModul extends AModul<InventoryConfig> implements Listener {
    public static StringFlag inventory_identifier;
    private final HashMap<UUID, String> playerInventoryCache = new HashMap<>(); //todo use caffeein cache

    public InventoryRegionModul(final @NotNull EventHelper plugin) {
        super(plugin, new InventoryConfig(plugin));
        this.getConfig().setModul(this);

        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        try {
            // create a flag with the name "inventory-identifier", defaulting to "default"
            StringFlag flag = new StringFlag("inventory-identifier", config.getDefaultIdentifier());
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
                plugin.getLogger().log(Level.WARNING, "couldn't enable Flag \"cinventory-identifier\". Might conflict with other plugin.");
            }
        }
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        playerInventoryCache.put(event.getPlayer().getUniqueId(), config.loadIdentifier(event.getPlayer()));
    }

    private void updateInventory(final @NotNull Player buk_player, final @NotNull com.sk89q.worldedit.util.Location weLocation) {
        LocalPlayer we_Player = WorldGuardPlugin.inst().wrapPlayer(buk_player);

        final @NotNull RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        final @Nullable String identifier = query.getApplicableRegions(weLocation).queryValue(we_Player, inventory_identifier);

        String oldIndentifier = playerInventoryCache.get(buk_player.getUniqueId());
        playerInventoryCache.putIfAbsent(buk_player.getUniqueId(), identifier);

        if (!Objects.equals(oldIndentifier, identifier)) {
            if (oldIndentifier != null) {
                config.savePlayerData(buk_player, oldIndentifier);
            }
            config.loadPlayerData(buk_player, identifier);

            playerInventoryCache.put(buk_player.getUniqueId(), identifier);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerTeleport(final @NotNull PlayerTeleportEvent event) {
        if (inventory_identifier == null) {
            return;
        }

        updateInventory(event.getPlayer(), BukkitAdapter.adapt(event.getTo()));
    }

    @EventHandler
    private void onPlayerMove(final @NotNull PlayerMoveEvent event) {
        if (!event.isCancelled()) {
            if (inventory_identifier == null) {
                return;
            }

            updateInventory(event.getPlayer(), BukkitAdapter.adapt(event.getTo()));
        }
    }

    @Override
    public @NotNull String getName() {
        return "inventoryRegions";
    }

    @Override
    public void onEnable() {
        if (plugin.getDependencyManager().isWorldGuardEnabled()) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }
}
