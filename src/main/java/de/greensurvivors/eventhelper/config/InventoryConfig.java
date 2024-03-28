package de.greensurvivors.eventhelper.config;

import de.greensurvivors.eventhelper.Eventhelper;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class InventoryConfig {
    final static String Digits = "(\\p{Digit}+)";
    final static String HexDigits = "(\\p{XDigit}+)";
    // an exponent is 'e' or 'E' followed by an optionally
    // signed decimal integer.
    final static String Exp = "[eE][+-]?" + Digits;
    final static String fpRegex =
            ("[\\x00-\\x20]*" + // Optional leading "whitespace"
                    "[+-]?(" +         // Optional sign character
                    //"NaN|" +           // "NaN" string
                    //"Infinity|" +      // "Infinity" string

                    // A decimal floating-point string representing a finite positive
                    // number without a leading sign has at most five basic pieces:
                    // Digits . Digits ExponentPart FloatTypeSuffix
                    //
                    // Since this method allows integer-only strings as input
                    // in addition to strings of floating-point literals, the
                    // two sub-patterns below are simplifications of the grammar
                    // productions from the Java Language Specification, 2nd
                    // edition, section 3.10.2.

                    // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
                    "(((" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|" +

                    // . Digits ExponentPart_opt FloatTypeSuffix_opt
                    "(\\.(" + Digits + ")(" + Exp + ")?)|" +

                    // Hexadecimal strings
                    "((" +
                    // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
                    "(0[xX]" + HexDigits + "(\\.)?)|" +

                    // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
                    "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

                    ")[pP][+-]?" + Digits + "))" +
                    "[fFdD]?))" +
                    "[\\x00-\\x20]*");// Optional trailing "whitespace"
    private static final Pattern FLOAT_PATTERN = Pattern.compile(fpRegex);
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

    private static InventoryConfig instance;

    private final ItemStack[] defaultInventory = new ItemStack[InventoryType.PLAYER.getDefaultSize()];
    private final ItemStack[] defaultEnderInv = new ItemStack[InventoryType.ENDER_CHEST.getDefaultSize()];
    private float defaultExp = 0.0f;
    private int defaultLevel = 0;
    private float defaultHealth = 20.0f;
    private int defaultFood = 20;
    private String defaultIdentifier = "default";

    public static InventoryConfig inst() {
        if (instance == null)
            instance = new InventoryConfig();
        return instance;
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
        File file = new File(Eventhelper.getPlugin().getDataFolder(), "inventory_regions" + File.separator + player.getUniqueId() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        //saves the inventory, enderchest and states of a player under the identifier
        cfg.set(buildKey(identifier, INVENTORY), player.getInventory().getContents());
        cfg.set(buildKey(identifier, ENDERCHEST), player.getEnderChest().getContents());
        cfg.set(buildKey(identifier, STATS, EXP), player.getExp());
        cfg.set(buildKey(identifier, STATS, LEVEL), player.getLevel());
        cfg.set(buildKey(identifier, STATS, HEALTH), player.getHealth());
        cfg.set(buildKey(identifier, STATS, HUNGER), player.getFoodLevel());

        //save attributes
        cfg.set(buildKey(identifier, ATTRIBUTES), Arrays.stream(Attribute.values()).map(attribute -> {
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null) {
                Map<String, Object> attributeMap = new HashMap<>();
                attributeMap.put(ATTRIBUTE_TYPE, attribute.name());
                attributeMap.put(ATTRIBUTE_BASE_VALUE, attributeInstance.getBaseValue());
                attributeMap.put(ATTRIBUTE_MODIFIERS, attributeInstance.getModifiers().stream().map(AttributeModifier::serialize).toList());

                return attributeMap;
            } else {
                return new HashMap<>();
            }
        }).filter(s -> !s.isEmpty()).toList());

        // save modified configuration
        cfg.options().parseComments(true);
        try {
            cfg.save(file);
        } catch (IOException e) {
            Eventhelper.getPlugin().getLogger().log(Level.SEVERE, "Could not save " + file.getName() + " inventory file.", e);
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
        File file = new File(Eventhelper.getPlugin().getDataFolder(), "inventory_regions" + File.separator + player.getUniqueId() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        //set active
        cfg.set(ACTIVE_INVENTORY, identifier);

        // save modified configuration
        cfg.options().parseComments(true);
        try {
            cfg.save(file);
        } catch (IOException e) {
            Eventhelper.getPlugin().getLogger().log(Level.SEVERE, "Could not save " + file.getName() + " inventory file.", e);
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

        //todo

        if (cfg.contains(buildKey(identifier, ATTRIBUTES))) {
            List<?> attributeObjs = cfg.getList(buildKey(identifier, ATTRIBUTES));

            if (attributeObjs != null) {

                //reset attributes
                for (Attribute attribute : Attribute.values()) {
                    AttributeInstance attributeInstance = player.getAttribute(attribute);
                    if (attributeInstance == null) {
                        continue;
                    }

                    attributeInstance.setBaseValue(attributeInstance.getDefaultValue());
                    attributeInstance.getModifiers().clear();
                }


                //load attributes
                for (Object mapObj : attributeObjs) {
                    if (mapObj instanceof Map<?, ?> attributeMap) {
                        String type = null;
                        Double baseValue = null;
                        List<AttributeModifier> modifiers = new ArrayList<>();

                        for (Object strObj : attributeMap.keySet()) {
                            if (strObj instanceof String id) {
                                if (id.equalsIgnoreCase(ATTRIBUTE_TYPE)) {
                                    Object attributeNameObj = attributeMap.get(strObj);

                                    if (attributeNameObj instanceof String name) {
                                        type = name;
                                    }
                                } else if (id.equalsIgnoreCase(ATTRIBUTE_BASE_VALUE)) {
                                    Object baseObj = attributeMap.get(strObj);

                                    if (baseObj instanceof Double base) {
                                        baseValue = base;
                                    } else if (baseObj instanceof Float base) {
                                        baseValue = Double.valueOf(base);
                                    } else if (baseObj instanceof String baseStr) {
                                        if (isDouble(baseStr)) {
                                            baseValue = Double.parseDouble(baseStr);
                                        }
                                    }
                                } else if (id.equalsIgnoreCase(ATTRIBUTE_MODIFIERS)) {
                                    Object modifierListObjMapObjs = attributeMap.get(strObj);

                                    if (modifierListObjMapObjs instanceof List<?> modifierListMapObjs) {
                                        for (Object modifierMapObj : modifierListMapObjs) {
                                            if (modifierMapObj instanceof Map<?, ?> modifierMap) {
                                                Map<String, Object> checkedModifierMap = new HashMap<>();

                                                for (Object modifierMapKeyObj : modifierMap.keySet()) {
                                                    if (modifierMapKeyObj instanceof String modifierMapKey) {
                                                        checkedModifierMap.put(modifierMapKey, modifierMap.get(modifierMapKeyObj));
                                                    }
                                                }

                                                modifiers.add(AttributeModifier.deserialize(checkedModifierMap));
                                            }
                                        }
                                    }

                                } else {
                                    Eventhelper.getPlugin().getLogger().log(Level.WARNING, "unknown playerdata-attribut id: \"" + id + "\".");
                                }
                            }
                        }

                        if (type != null && baseValue != null) {
                            Attribute attribute = Attribute.valueOf(type);
                            player.registerAttribute(attribute);

                            player.getAttribute(attribute).setBaseValue(baseValue);
                            for (AttributeModifier modifier : modifiers) {
                                player.getAttribute(attribute).getModifiers().add(modifier);
                            }
                        }
                    }
                }
            }
        }

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
        File file = new File(Eventhelper.getPlugin().getDataFolder(), "inventory_regions" + File.separator + player.getUniqueId() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        //get active inventory identifier
        return cfg.getString(ACTIVE_INVENTORY, defaultIdentifier);
    }

    /**
     * Test if a String can safely convert into a double
     *
     * @param toTest String input
     */
    private static boolean isDouble(String toTest) {
        if (toTest == null) {
            return false;
        }

        if (toTest.isEmpty()) { //empty
            return false;
        }

        return FLOAT_PATTERN.matcher(toTest).find();
    }
}
