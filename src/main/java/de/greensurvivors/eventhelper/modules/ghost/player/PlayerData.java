package de.greensurvivors.eventhelper.modules.ghost.player;

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

/// temporary store all important data about a player to restore it later
public class PlayerData { // todo share this class with InventoryRegions
    private final @NotNull EventHelper plugin;
    private final @NotNull UUID uuid;
    private final @NotNull GameMode prevGameMode;
    private final boolean wasInvulnerable;
    private final boolean wasSilent;
    private final @NotNull Scoreboard prevScoreBoard;
    private final ItemStack @NotNull [] prevInventory;
    private final ItemStack @NotNull [] prevEnderChest;
    private final @NotNull Collection<PotionEffect> prevEffects;
    private final boolean wasGlowing;
    private final double prevMaxHealthBase;
    private final double prevHealth;
    private final int prevFoodLevel;
    private final float prevSaturation;
    private final float prevExhaustion;
    private final float prevExperience;
    private final int prevExperienceLevel;
    private final @Nullable WeatherType prevWeatherType;
    private final long prevPlayerTimeOffset;
    private final boolean prevPlayerTimeIsRelative;
    private final boolean prevCouldFly;
    private final boolean hadGravity;

    public PlayerData(final @NotNull EventHelper plugin, final @NotNull Player player) {
        this.plugin = plugin;
        this.uuid = player.getUniqueId();

        this.prevGameMode = player.getGameMode();
        this.wasInvulnerable = player.isInvulnerable();
        player.setInvulnerable(false);
        player.setGravity(true);
        this.wasSilent = player.isSilent();
        this.prevScoreBoard = player.getScoreboard();

        this.prevInventory = player.getInventory().getContents();
        player.getInventory().clear();

        this.prevEnderChest = player.getEnderChest().getContents();
        player.getEnderChest().clear();

        this.prevEffects = player.getActivePotionEffects();
        for (@NotNull PotionEffect prevEffect : prevEffects) {
            player.removePotionEffect(prevEffect.getType());
        }
        this.wasGlowing = player.isGlowing();

        final AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        this.prevMaxHealthBase = maxHealthAttribute.getValue();
        maxHealthAttribute.setBaseValue(maxHealthAttribute.getDefaultValue());
        this.prevHealth = player.getHealth();
        player.setHealth(maxHealthAttribute.getDefaultValue());
        this.prevFoodLevel = player.getFoodLevel();
        player.setFoodLevel(20);
        this.prevSaturation = player.getSaturation();
        player.setSaturation(15);
        this.prevExhaustion = player.getExhaustion();
        player.setExhaustion(0.0f);

        this.prevExperience = player.getExp();
        player.setExp(0.0f);
        this.prevExperienceLevel = player.getLevel();
        player.setLevel(0);

        this.prevWeatherType = player.getPlayerWeather();
        this.prevPlayerTimeOffset = player.getPlayerTimeOffset();
        this.prevPlayerTimeIsRelative = player.isPlayerTimeRelative();

        this.prevCouldFly = player.getAllowFlight();
        this.hadGravity = player.hasGravity();

        player.updateInventory();
    }

    public void restorePlayer() {
        final @Nullable Player player = plugin.getServer().getPlayer(uuid);

        if (player == null) {
            plugin.getComponentLogger().error("Could not restore player data for player with uuid {}. Their data might get lost!", uuid);
        } else {
            player.setGameMode(prevGameMode);
            player.setInvulnerable(wasInvulnerable);
            player.setSilent(wasSilent);
            player.setScoreboard(prevScoreBoard);

            player.getInventory().clear();
            player.getInventory().setContents(prevInventory);
            player.getEnderChest().setContents(prevEnderChest);

            for (@NotNull PotionEffect prevEffect : player.getActivePotionEffects()) {
                player.removePotionEffect(prevEffect.getType());
            }
            for (@NotNull PotionEffect prevEffect : prevEffects) {
                player.addPotionEffect(prevEffect);
            }
            player.setGlowing(wasGlowing);

            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(prevMaxHealthBase);
            player.setHealth(prevHealth);
            player.setFoodLevel(prevFoodLevel);
            player.setSaturation(prevSaturation);
            player.setExhaustion(prevExhaustion);

            player.setExp(prevExperience);
            player.setLevel(prevExperienceLevel);

            if (prevWeatherType == null) {
                player.resetPlayerWeather();
            } else {
                player.setPlayerWeather(prevWeatherType);
            }
            player.setPlayerTime(prevPlayerTimeOffset, prevPlayerTimeIsRelative);

            player.setAllowFlight(prevCouldFly);
            player.setGravity(hadGravity);

            player.updateInventory();
        }
    }
}
