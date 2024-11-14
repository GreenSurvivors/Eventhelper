package de.greensurvivors.eventhelper.modules.inventory;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.AModulConfig;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class InventoryConfig extends AModulConfig<InventoryRegionModul> {
    private final static String
        INVENTORY = "inventory",
        ENDERCHEST = "enderchest",
        STATS = "stats",
        EXP = "xp",
        LEVEL = "level",
        HEALTH = "health",
        HUNGER = "hunger",
        ATTRIBUTES = "attributes",
        ATTRIBUTE_BASE_VALUE = "base",
        ATTRIBUTE_MODIFIERS = "modifiers",
        ATTRIBUTE_TYPE = "type",
        ACTIVE_INVENTORY = "activeInventory";

    private final String defaultIdentifier = "default";

    public InventoryConfig(final @NotNull EventHelper plugin) {
        super(plugin);
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> reload() {
        final CompletableFuture<Boolean> runAfter = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                if (getModul() != null) {
                    if (!Files.isRegularFile(configPath)) {
                        plugin.saveResource(getModul().getName() + "/" + configPath.getFileName().toString(), false);
                    }

                    try (BufferedReader bufferedReader = Files.newBufferedReader(configPath)) {
                        @NotNull YamlConfiguration config = YamlConfiguration.loadConfiguration(bufferedReader);

                        @Nullable String dataVersionStr = config.getString(VERSION_PATH);
                        if (dataVersionStr != null) {
                            ComparableVersion lastVersion = new ComparableVersion(dataVersionStr);

                            if (dataVersion.compareTo(lastVersion) < 0) {
                                plugin.getComponentLogger().warn("Found modul config for \"{}\" was saved in a newer data " +
                                    "version ({}), expected: {}. Trying to load anyway but some this most definitely " +
                                    "will be broken!", getModul().getName(), lastVersion, dataVersion);
                            }
                        } else {
                            plugin.getComponentLogger().warn("The data version for modul config for \"{}\" was missing." +
                                " Proceed with care!", getModul().getName());
                        }

                        isEnabled.setValue(config.getBoolean(isEnabled.getPath()));
                        Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(isEnabled.getValueOrFallback())); // back to main thread
                    } catch (IOException e) {
                        plugin.getComponentLogger().error("Could not load modul config for {} from file!", getModul().getName(), e);

                        isEnabled.setValue(Boolean.FALSE);
                        Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.FALSE)); // back to main thread
                    }
                } else {
                    plugin.getComponentLogger().error("Could not load modul config, since the module of {} was not set!", this.getClass().getName());

                    isEnabled.setValue(Boolean.FALSE);
                    Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.FALSE)); // back to main thread
                }
            }
        });

        return runAfter;
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> save() {
        final CompletableFuture<@NotNull Boolean> runAfter = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                if (getModul() != null) {
                    if (!Files.isRegularFile(configPath)) {
                        plugin.saveResource(getModul().getName() + "/" + configPath.getFileName().toString(), false);
                    }

                    try (BufferedReader bufferedReader = Files.newBufferedReader(configPath)) {
                        @NotNull YamlConfiguration config = YamlConfiguration.loadConfiguration(bufferedReader);

                        config.set(VERSION_PATH, dataVersion.toString());
                        config.set(isEnabled.getPath(), isEnabled.getValueOrFallback());

                        config.options().parseComments(true);
                        config.save(configPath.toFile());

                        Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.TRUE)); // back to main thread
                    } catch (IOException e) {
                        plugin.getComponentLogger().error("Could not load modul config for {} from file!", getModul().getName(), e);

                        isEnabled.setValue(Boolean.FALSE);
                        Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.TRUE)); // back to main thread
                    }
                } else {
                    plugin.getComponentLogger().error("Could not save modul config, since the module of {} was not set!", this.getClass().getName());

                    isEnabled.setValue(Boolean.FALSE);
                    Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.TRUE)); // back to main thread
                }
            }
        });

        return runAfter;
    }

    public String getDefaultIdentifier() {
        return defaultIdentifier;
    }

    /**
     * Saves the inventory of a player
     *
     * @param player     player whose inventory is about to be saved
     * @param identifier the identifier what inventory should be saved.
     */
    public void savePlayerData(final @NotNull Player player, final @NotNull String identifier) {
        File file = new File(plugin.getDataFolder(), "inventory_regions" + File.separator + player.getUniqueId() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        //saves the inventory, enderchest and states of a player under the identifier
        cfg.set(buildKey(identifier, INVENTORY), player.getInventory().getContents());
        cfg.set(buildKey(identifier, ENDERCHEST), player.getEnderChest().getContents());
        cfg.set(buildKey(identifier, STATS, EXP), player.getExp());
        cfg.set(buildKey(identifier, STATS, LEVEL), player.getLevel());
        cfg.set(buildKey(identifier, STATS, HEALTH), player.getHealth());
        cfg.set(buildKey(identifier, STATS, HUNGER), player.getFoodLevel());

        // save modified configuration
        cfg.options().parseComments(true);
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getName() + " inventory file.", e);
        }
    }

    private String buildKey(String... args) {
        return String.join(".", args);
    }

    /**
     * loads the inventory and stats of a player, depending on the identifier
     *
     * @param player     player whose inventory is about to be loaded
     * @param identifier the identifier what inventory should be loaded.
     */
    public void loadPlayerData(final @NotNull Player player, final @NotNull String identifier) {
        File file = new File(plugin.getDataFolder(), "inventory_regions" + File.separator + player.getUniqueId() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        //set active
        cfg.set(ACTIVE_INVENTORY, identifier);

        // save modified configuration
        cfg.options().parseComments(true);
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getName() + " inventory file.", e);
        }

        List<?> inventoryListLoaded = cfg.getList(buildKey(identifier, INVENTORY));
        List<?> enderListLoaded = cfg.getList(buildKey(identifier, ENDERCHEST));

        if (inventoryListLoaded == null) {
            player.getInventory().clear();
        } else {
            List<ItemStack> inventoryList = new ArrayList<>();

            for (Object obj : inventoryListLoaded) {
                if (obj instanceof ItemStack || obj == null) {
                    inventoryList.add((ItemStack) obj);
                }
            }

            player.getInventory().setContents(inventoryList.toArray(new ItemStack[0]));
        }

        if (enderListLoaded == null) {
            player.getEnderChest().clear();
        } else {
            List<ItemStack> enderList = new ArrayList<>();

            for (Object obj : enderListLoaded) {
                if (obj instanceof ItemStack || obj == null) {
                    enderList.add((ItemStack) obj);
                }
            }

            player.getEnderChest().setContents(enderList.toArray(new ItemStack[0]));
        }

        player.updateInventory();

        player.setExp(Math.max(0, (float) cfg.getDouble(buildKey(identifier, STATS, EXP), 0.0)));
        player.setLevel(Math.max(0, cfg.getInt(buildKey(identifier, STATS, LEVEL), 0)));
        final double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        player.setHealth(Math.max(0, Math.min(maxHealth, cfg.getDouble(buildKey(identifier, STATS, HEALTH), maxHealth))));
        player.setFoodLevel(Math.max(0, Math.min(20, cfg.getInt(buildKey(identifier, STATS, HUNGER), 20))));
    }

    /**
     * loads the inventory identifier a player
     *
     * @param player player whose inventory identifier is about to be loaded
     */
    public @NotNull String loadIdentifier(final @NotNull Player player) {
        File file = new File(plugin.getDataFolder(), "inventory_regions" + File.separator + player.getUniqueId() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        //get active inventory identifier
        return cfg.getString(ACTIVE_INVENTORY, defaultIdentifier);
    }
}
