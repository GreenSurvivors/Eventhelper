package de.greensurvivors.eventhelper.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import net.minecraft.util.Mth;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class TNTKnockback implements Listener {
    private final Plugin eventHelper;
    private final Plugin worldGuard;
    private final HashMap<UUID, TntAndTasks> interactionMap = new HashMap<>();
    private final HashMap<UUID, InteractionAndSpeed> tntMap = new HashMap<>();
    private StateFlag tntFlag;

    /**
     * creates the tnt-knockback-flag if the server recognizes worldguard.
     * Since flag creation has to be done before worldguard is enabled,
     * we can't check if it was successfully loaded here.
     */
    public TNTKnockback(Plugin eventHelper) {
        this.eventHelper = eventHelper;
        this.worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");

        if (worldGuard != null) {
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
                    this.eventHelper.getLogger().log(Level.WARNING, "couldn't enable Flag \"tnt-knockback\". Might conflict with other plugin.");
                }
            }
        }
    }

    private boolean isWorldguardEnabled() {
        return worldGuard != null && worldGuard.isEnabled();
    }

    /**
     * clears all interact entities and internal data
     */
    public void cleanAll() {
        for (Map.Entry<UUID, TntAndTasks> entry : interactionMap.entrySet()) {
            entry.getValue().task.cancel();

            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity != null) {
                entity.remove();
            }
        }

        interactionMap.clear();
        tntMap.clear();
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
    private void doTimerTask(UUID interactionUUID) {
        Interaction interaction = (Interaction) Bukkit.getEntity(interactionUUID);

        if (interaction != null) { // try to get the interaction entity
            TntAndTasks tntAndTasks = interactionMap.get(interactionUUID);

            if (tntAndTasks != null) {
                Entity tntEntity = Bukkit.getEntity(tntAndTasks.tntUUID());

                if (tntEntity != null) {
                    InteractionAndSpeed interactionAndSpeed = tntMap.get(tntAndTasks.tntUUID());
                    // add together with velocity of tnt, so we don't fling it to a seemingly random direction
                    Vector newVelocity = tntEntity.getVelocity().add(interactionAndSpeed.getKnockbackVec());

                    //only move if the tnt would not hit anything
                    if (!tntEntity.wouldCollideUsing(tntEntity.getBoundingBox().shift(newVelocity))) {

                        //move interact and tnt
                        tntEntity.teleport(tntEntity.getLocation().add(newVelocity));

                        //calc next step
                        interactionAndSpeed.getKnockbackVec().setY((interactionAndSpeed.getKnockbackVec().getY() - 0.04) * 0.98);
                    }

                    //allways tp interaction, since its allways a tick behind
                    interaction.setVelocity(tntEntity.getVelocity());
                    interaction.teleport(tntEntity);
                } else { // tnt was not found, remove map entries, cancel task and interaction entity
                    interaction.remove();
                    tntAndTasks.task.cancel();

                    interactionMap.remove(interactionUUID);
                    tntMap.remove(tntAndTasks.tntUUID());
                }
            } else { // map entry is broken. clear up as much as we can
                interaction.remove();

                interactionMap.remove(interactionUUID);
            }
        } else { //interaction entity was not found, remove map entries and try to cancel task
            interactionMap.remove(interactionUUID);
            TntAndTasks tntAndTasks = interactionMap.get(interactionUUID);

            if (tntAndTasks != null) {
                tntMap.remove(tntAndTasks.tntUUID());
            }
        }
    }

    /**
     * every time a tnt is ignited in a region with the knockback flag,
     * an interaction entity will be spawned and a task to handle
     * the knockback will be created.
     *
     * @param event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onTNTIgnite(EntitySpawnEvent event) {
        if (isWorldguardEnabled() && tntFlag != null && event.getEntity() instanceof TNTPrimed tntPrimed) {
            Location entityLoc = tntPrimed.getLocation();

            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(entityLoc));

            if (set.testState(null, tntFlag)) {
                tntPrimed.getLocation().getWorld().spawn(tntPrimed.getLocation(), Interaction.class, CreatureSpawnEvent.SpawnReason.COMMAND,
                        interaction -> {
                            interaction.setResponsive(true);

                            interaction.setInteractionHeight((float) tntPrimed.getBoundingBox().getHeight() + 0.03f);
                            interaction.setInteractionWidth((float) tntPrimed.getBoundingBox().getWidthX() + 0.03f);

                            interactionMap.put(interaction.getUniqueId(),
                                    new TntAndTasks(tntPrimed.getUniqueId(),
                                            Bukkit.getScheduler().runTaskTimer(eventHelper, () -> doTimerTask(interaction.getUniqueId()), 1, 1)));

                            tntMap.put(tntPrimed.getUniqueId(), new InteractionAndSpeed(interaction.getUniqueId(), new Vector()));
                        });
            }
        }
    }

    /**
     * every time an interaction entity in a region with a knockback flag,
     * gets hit with something with a knockback enchantment,
     * a knockback velocity vector will be calculated
     * @param event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onTNTHit(EntityDamageByEntityEvent event) {
        //check region for flag
        if (isWorldguardEnabled() && tntFlag != null) {
            if (event.getEntity() instanceof Interaction interactionEntity && event.getDamager() instanceof Player player) {
                Location interactionLoc = interactionEntity.getLocation();

                RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
                ApplicableRegionSet regionSet = query.getApplicableRegions(BukkitAdapter.adapt(interactionLoc));

                if (regionSet.testState(null, tntFlag)) {
                    //get knockback
                    Integer knockbackValue = player.getInventory().getItemInMainHand().getEnchantments().get(Enchantment.KNOCKBACK);

                    if (knockbackValue != null && knockbackValue > 0) {
                        //get tnt
                        UUID tntUUID = interactionMap.get(interactionEntity.getUniqueId()).tntUUID;

                        if (tntUUID != null) {
                            Entity tntEntity = Bukkit.getEntity(tntUUID);

                            // vanilla parity
                            if (tntEntity != null) {
                                if (player.isSprinting()) {
                                    player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1, 1);
                                    ++knockbackValue;
                                }

                                // calc knockback velocity vector
                                float yaw = player.getLocation().getYaw();

                                Vector delta = new Vector(-Mth.sin(yaw * 0.017453292F) * knockbackValue * 0.5F, 0.1D, (Mth.cos(yaw * 0.017453292F) * knockbackValue * 0.5F));

                                if (new EntityPushedByEntityAttackEvent(tntEntity, player, delta).callEvent()) {

                                    // set vector
                                    tntMap.get(tntUUID).setKnockbackVec(delta);
                                }
                            }
                        }
                    }
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

    /**
     * this is a record like class to map tnt entities to their interaction and knockback vector
     * However, every hit this knockback velocity needs to get updated, so it can't be a record afterall
     */
    private static final class InteractionAndSpeed {
        private final UUID interactionUUID;
        private Vector knockbackVec;

        private InteractionAndSpeed(UUID interactionUUID, Vector knockbackVec) {
            this.interactionUUID = interactionUUID;
            this.knockbackVec = knockbackVec;
        }

        public UUID interactionUUID() {
            return interactionUUID;
        }

        public Vector getKnockbackVec() {
            return knockbackVec;
        }

        public void setKnockbackVec(Vector knockbackVec) {
            this.knockbackVec = knockbackVec;
        }
    }
}
