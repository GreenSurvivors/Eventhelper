package de.greensurvivors.eventhelper.modules.ghost.payer;

import de.greensurvivors.eventhelper.EventHelper;
import org.bukkit.GameMode;
import org.bukkit.WeatherType;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

public class PlayerData {
    private final @NotNull EventHelper plugin;
    private final @NotNull UUID uuid;
    private final @NotNull GameMode prevGameMode;
    private final @NotNull Scoreboard prevScoreBoard;
    private final ItemStack @NotNull [] prevInventory;
    private final ItemStack @NotNull [] prevArmor;
    private final ItemStack @NotNull [] prevExtraItems;
    private final ItemStack @NotNull [] prevStorage;
    private final @NotNull Collection<PotionEffect> prevEffects;
    private final double prevMaxHealthBase;
    private final double prevHealth;
    private final int prevFoodLevel;
    private final float prevSaturation;
    private final float prevExperience;
    private final int prevExperienceLevel;
    private final @Nullable WeatherType prevWeatherType;
    private final long prevPlayerTimeOffset;
    private final boolean prevPlayerTimeIsRelative;
    private final boolean prevCouldFly;

    public PlayerData(final @NotNull EventHelper plugin, final @NotNull Player player) {
        this.plugin = plugin;
        this.uuid = player.getUniqueId();

        this.prevGameMode = player.getGameMode();
        this.prevScoreBoard = player.getScoreboard();

        this.prevInventory = player.getInventory().getContents();
        this.prevArmor = player.getInventory().getArmorContents();
        this.prevExtraItems = player.getInventory().getExtraContents();
        this.prevStorage = player.getInventory().getExtraContents();
        player.getInventory().clear();

        this.prevEffects = player.getActivePotionEffects();
        for (@NotNull PotionEffect prevEffect : prevEffects) {
            player.removePotionEffect(prevEffect.getType());
        }

        final AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        this.prevMaxHealthBase = maxHealthAttribute.getValue();
        maxHealthAttribute.setBaseValue(maxHealthAttribute.getDefaultValue());
        this.prevHealth = player.getHealth();
        player.setHealth(maxHealthAttribute.getDefaultValue());
        this.prevFoodLevel = player.getFoodLevel();
        player.setFoodLevel(20);
        this.prevSaturation = player.getSaturation();
        player.setSaturation(15);

        this.prevExperience = player.getExp();
        player.setExp(0.0f);
        this.prevExperienceLevel = player.getLevel();
        player.setLevel(0);

        this.prevWeatherType = player.getPlayerWeather();
        this.prevPlayerTimeOffset = player.getPlayerTimeOffset();
        this.prevPlayerTimeIsRelative = player.isPlayerTimeRelative();

        this.prevCouldFly = player.getAllowFlight();

        player.updateInventory();
    }

    public void restorePlayer() {
        final @Nullable Player player = plugin.getServer().getPlayer(uuid);

        if (player == null) {
            plugin.getComponentLogger().error("Could not restore player data for player with uuid {}. Their data might get lost!", uuid);
        } else {
            player.setGameMode(prevGameMode);
            player.setScoreboard(prevScoreBoard);

            player.getInventory().clear();
            player.getInventory().setContents(prevInventory);
            player.getInventory().setArmorContents(prevArmor);
            player.getInventory().setExtraContents(prevExtraItems);
            player.getInventory().setExtraContents(prevStorage);

            for (@NotNull PotionEffect prevEffect : player.getActivePotionEffects()) {
                player.removePotionEffect(prevEffect.getType());
            }
            for (@NotNull PotionEffect prevEffect : prevEffects) {
                player.addPotionEffect(prevEffect);
            }

            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(prevMaxHealthBase);
            player.setHealth(prevHealth);
            player.setFoodLevel(prevFoodLevel);
            player.setSaturation(prevSaturation);

            player.setExp(prevExperience);
            player.setLevel(prevExperienceLevel);

            if (prevWeatherType == null) {
                player.resetPlayerWeather();
            } else {
                player.setPlayerWeather(prevWeatherType);
            }
            player.setPlayerTime(prevPlayerTimeOffset, prevPlayerTimeIsRelative);

            player.setAllowFlight(prevCouldFly);

            player.updateInventory();
        }
    }
}
