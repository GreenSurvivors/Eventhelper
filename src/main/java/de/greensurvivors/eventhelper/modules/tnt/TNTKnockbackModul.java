package de.greensurvivors.eventhelper.modules.tnt;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.AModul;
import de.greensurvivors.eventhelper.modules.StateChangeEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class TNTKnockbackModul extends AModul<TNTKnockbackConfig> implements Listener {
    private static final @NotNull
    @KeyPattern.Namespace String MODUL_ID = "tnt_knockback";
    private final HashMap<UUID, TntAndTasks> interactionMap = new HashMap<>();
    private StateFlag tntFlag;

    /**
     * creates the tnt-knockback-flag if the server recognizes worldguard.
     * Since flag creation has to be done before worldguard is enabled,
     * we can't check if it was successfully loaded here.
     */
    public TNTKnockbackModul(final @NotNull EventHelper plugin) {
        super(plugin, new TNTKnockbackConfig(plugin, MODUL_ID));

        if (plugin.getDependencyManager().isWorldGuardInstanceSafe()) {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

            try {
                tntFlag = new StateFlag("tnt-knockback", false);
                registry.register(tntFlag);

            } catch (FlagConflictException e) {
                // some other plugin registered a flag by the same name already.
                // you can use the existing flag, but this may cause conflicts - be sure to check type
                Flag<?> existing = registry.get("tnt-knockback");
                if (existing instanceof StateFlag stateFlag) {
                    tntFlag = stateFlag;
                } else {
                    tntFlag = null;
                    // types don't match - this is bad news! some other plugin conflicts with you
                    // hopefully this never actually happens
                    this.plugin.getLogger().log(Level.WARNING, "couldn't enable Flag \"tnt-knockback\". Might conflict with other plugin.");
                }
            }
        }
    }

    /**
     * spawns a new Interaction entity and links it to the given entity,
     * also starts a new Timer task to update it every tick to match the tnt's location as close as possible
     *
     * @param probablyTnt The Entity to get knock-backed whenever the interaction entity was hit.
     *                    All assumptions where made for a primed tnt;
     *                    however, everything may work with other entities as well
     */
    private void spawnInteraction(final @NotNull Entity probablyTnt) {
        probablyTnt.getWorld().spawn(probablyTnt.getLocation(), Interaction.class, CreatureSpawnEvent.SpawnReason.COMMAND,
            interaction -> {
                interaction.setResponsive(true);

                // 0.03f is just a magic value to be slightly bigger than the original hitbox
                interaction.setInteractionHeight((float) probablyTnt.getBoundingBox().getHeight() + 0.03f);
                interaction.setInteractionWidth((float) probablyTnt.getBoundingBox().getWidthX() + 0.03f);

                interactionMap.put(interaction.getUniqueId(),
                    new TntAndTasks(probablyTnt.getUniqueId(),
                        Bukkit.getScheduler().runTaskTimer(plugin, () -> doTimerTask(interaction.getUniqueId()), 1, 1)));
            });
    }

    /**
     * every tnt ignited in a region with knockback flag has to tick,
     * to move.
     * Also, this function will try to clean after itself if a tnt explodes or dies
     *
     * @param interactionUUID every task created for this plugin does
     *                        call this function with the UUID of its
     *                        interaction entity as identifier
     */
    private void doTimerTask(final @NotNull UUID interactionUUID) {
        Interaction interaction = (Interaction) Bukkit.getEntity(interactionUUID);
        TntAndTasks tntAndTasks = interactionMap.get(interactionUUID);

        if (tntAndTasks != null) {
            Entity tntEntity = Bukkit.getEntity(tntAndTasks.tntUUID());

            if (interaction != null) { // try to get the interaction entity
                // if you really want to be safe, test here if tntEntity is instanceof TntPrimed.
                // However, I like to believe since this task get called every tick it's very unlikely
                // an entity gets replaced with another with the same UUID.
                // worst case we will have a ticking Interaction entity until next restart
                if (tntEntity != null) {
                    //always tp interaction, since it's always a tick behind and it does not move
                    interaction.teleport(tntEntity);
                } else { // tnt was not found, remove map entries, cancel task and kill interaction entity
                    interaction.remove();
                    tntAndTasks.task().cancel();

                    interactionMap.remove(interactionUUID);
                }

            } else { //interaction entity was not found, try to recover with a new interaction
                if (tntEntity != null) {
                    spawnInteraction(tntEntity);
                }

                tntAndTasks.task().cancel();
                interactionMap.remove(interactionUUID);
            }
        } else { // map entry is broken. clear up as much as we can
            if (interaction != null) {
                interaction.remove();
            }

            interactionMap.remove(interactionUUID);
        }
    }

    /**
     * every time a tnt is ignited in a region with the knockback flag,
     * an interaction entity will be spawned and a task to handle
     * the knockback will be created.
     *
     * @param event Called when an entity (tnt) is spawned into a world.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onTNTIgnite(EntitySpawnEvent event) {
        if (plugin.getDependencyManager().isWorldGuardEnabled() && tntFlag != null && event.getEntity() instanceof TNTPrimed tntPrimed) {
            Location entityLoc = tntPrimed.getLocation();

            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(entityLoc));

            if (set.testState(null, tntFlag)) {
                spawnInteraction(tntPrimed);
            }
        }
    }

    /**
     * every time an interaction entity in a region with a knockback flag,
     * gets hit with something with a knockback enchantment,
     * a knockback velocity vector will be calculated
     * <p>
     * These numbers and calculations are taken from
     * net.minecraft.world.entity.player.Player#attack (default Mojang mappings)
     * at 13.09.2023 paper 1.20.1, #Build 176
     *
     * @param event Called when an entity is damaged by an entity (Interaction by a Player)
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onTNTHit(final @NotNull EntityDamageByEntityEvent event) {
        //check region for flag
        if (plugin.getDependencyManager().isWorldGuardEnabled() && tntFlag != null) {
            if (event.getEntity() instanceof Interaction interactionEntity && event.getDamager() instanceof Player player) {
                Location interactionLoc = interactionEntity.getLocation();

                RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
                ApplicableRegionSet regionSet = query.getApplicableRegions(BukkitAdapter.adapt(interactionLoc));

                if (regionSet.testState(null, tntFlag)) {
                    boolean attackCooldownReady = player.getCooledAttackStrength(0.5f) > 0.9F;
                    int knockbackValue = player.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.KNOCKBACK);

                    // vanilla parity
                    if (player.isSprinting() && attackCooldownReady) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1, 1);
                        ++knockbackValue;
                    }

                    if (knockbackValue > 0) {
                        // Please note: since a primed tnt is in fact not a LivingEntity it's movements
                        // are slightly different. it moves faster horizontally but does fall slower.
                        // That's why we have to  halve the knock-back value and on top of that decrease
                        // the y by a magic number that is higher than the gravity acceleration of 0.04 per tick for tnt
                        double strength = (double) knockbackValue * 0.5D * 0.5D;

                        if (strength > 0.0D) {
                            //get tnt
                            UUID tntUUID = interactionMap.get(interactionEntity.getUniqueId()).tntUUID;
                            if (tntUUID != null) {
                                Entity tntEntity = Bukkit.getEntity(tntUUID);
                                if (tntEntity != null) {
                                    float yaw = player.getLocation().getYaw();
                                    double knockbackX = Math.sin(yaw * 0.017453292D);
                                    double knockbackZ = -Math.cos(yaw * 0.017453292D);

                                    Vector velocity = tntEntity.getVelocity();

                                    //calc for living entities
                                    Vector rawKnockback = (new Vector(knockbackX, 0.0D, knockbackZ)).normalize().multiply(strength);
                                    Vector calcKnockback = new Vector(velocity.getX() / 2.0D - rawKnockback.getX(), tntEntity.isOnGround() ? Math.min(0.4D, velocity.getY() / 2.0D + strength) : velocity.getY(), velocity.getZ() / 2.0D - rawKnockback.getZ());

                                    // to get closer to the movement of a LivingEntity it may help to do straight up skip the first calculation step
                                    // for primed tnt. I know a fact, that a livingEntity fist slows down its velocity by 0.98 than updates its position
                                    // while the tnt does it in reverse.
                                    // however even with the magic multiplication of 0.5 of above the curves don't match - the tnt flies to high.
                                    // A tnt accelerates 0.04 blocks per tnt down, that's less than a Living entity. That's why we give it a lower starting point.
                                    if (tntEntity.isOnGround()) {
                                        calcKnockback.setY(calcKnockback.getY() - 0.05);
                                    }
                                    calcKnockback.multiply(0.98);

                                    //todo maybe fire a EntityKnockbackByEntityEvent
                                    tntEntity.setVelocity(calcKnockback);

                                    // calc for non-living entities
                                    // while it maybe would be more accurate, at the same time it isn't nearly as fun as the curve of living entities
                                    // tntEntity.setVelocity( new Vector(-knockbackX * strength * 0.5D, 0.1D, -knockbackZ * strength * 0.5D));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public @NotNull @KeyPattern.Namespace String getName() {
        return MODUL_ID;
    }

    @Override
    @EventHandler(ignoreCancelled = true)
    protected void onConfigEnabledChange(@NotNull StateChangeEvent<?> event) {
        Key eventKey = event.getKey();

        if (eventKey.namespace().equals(getName()) && eventKey.value().equals(getName())) {
            if (event.getNewState() instanceof Boolean enabledState) {
                if (enabledState) {
                    if (plugin.getDependencyManager().isWorldGuardEnabled()) {
                        Bukkit.getPluginManager().registerEvents(this, plugin);
                    }
                } else { // clears all interact entities and internal data
                    HandlerList.unregisterAll(this);

                    for (Map.Entry<UUID, TntAndTasks> entry : interactionMap.entrySet()) {
                        entry.getValue().task.cancel();

                        Entity entity = Bukkit.getEntity(entry.getKey());
                        if (entity != null) {
                            entity.remove();
                        }
                    }

                    interactionMap.clear();
                }
            }
        }
    }

    /**
     * This is a technical record to map interaction entities to their tnt and task
     *
     * @param tntUUID
     * @param task
     */
    private record TntAndTasks(UUID tntUUID, BukkitTask task) {
    }
}
