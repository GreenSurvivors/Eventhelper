package de.greensurvivors.eventhelper.modules.inventory;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.AModulConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final ItemStack[] defaultInventory = new ItemStack[InventoryType.PLAYER.getDefaultSize()];
    private final ItemStack[] defaultEnderInv = new ItemStack[InventoryType.ENDER_CHEST.getDefaultSize()];
    private float defaultExp = 0.0f;
    private int defaultLevel = 0;
    private float defaultHealth = 20.0f;
    private int defaultFood = 20;
    private String defaultIdentifier = "default";

    public InventoryConfig(final @NotNull EventHelper plugin) {
        super(plugin);
    }

    @Override
    public CompletableFuture<Boolean> reload() {
        final CompletableFuture<Boolean> runAfter = new CompletableFuture<>();

        //todo
        runAfter.complete(isEnabled.getValueOrFallback());

        return runAfter;
    }

    public void setDefaults(ItemStack inventoryContent, float exp, int level, float health, int food, String identifier) {
        Arrays.fill(defaultInventory, inventoryContent);
        Arrays.fill(defaultEnderInv, inventoryContent);

        this.defaultExp = exp;
        this.defaultLevel = level;

        this.defaultHealth = health;
        this.defaultFood = food;
        this.defaultIdentifier = identifier;
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
    public void savePlayerData(Player player, String identifier) {
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
    public void loadPlayerData(Player player, String identifier) {
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

//		ItemStack[] content = ((List<ItemStack>) c.get(mode + ".inventory.armor")).toArray(new ItemStack[0]);
//		player.getInventory().setArmorContents(content);

        List<?> inventoryListLoaded = cfg.getList(buildKey(identifier, INVENTORY));
        List<?> enderListLoaded = cfg.getList(buildKey(identifier, ENDERCHEST));

        if (inventoryListLoaded == null) {
            player.getInventory().setContents(defaultInventory);
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
            player.getEnderChest().setContents(defaultEnderInv);
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

        player.setExp(Math.max(0, (float) cfg.getDouble(buildKey(identifier, STATS, EXP), defaultExp)));
        player.setLevel(Math.max(0, cfg.getInt(buildKey(identifier, STATS, LEVEL), defaultLevel)));
        player.setHealth(Math.max(0, Math.min(defaultHealth, cfg.getDouble(buildKey(identifier, STATS, HEALTH), defaultHealth))));
        player.setFoodLevel(Math.max(0, Math.min(defaultFood, cfg.getInt(buildKey(identifier, STATS, HUNGER), defaultFood))));
    }

    /**
     * loads the inventory identifier a player
     *
     * @param player player whose inventory identifier is about to be loaded
     */
    public String loadIdentifier(Player player) {
        File file = new File(plugin.getDataFolder(), "inventory_regions" + File.separator + player.getUniqueId() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        //get active inventory identifier
        return cfg.getString(ACTIVE_INVENTORY, defaultIdentifier);
    }
}
