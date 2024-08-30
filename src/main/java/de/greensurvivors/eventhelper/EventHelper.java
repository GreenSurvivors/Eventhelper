package de.greensurvivors.eventhelper;

import de.greensurvivors.eventhelper.command.MainCmd;
import de.greensurvivors.eventhelper.listeners.InventoryRegionListener;
import de.greensurvivors.eventhelper.listeners.TNTKnockbackListener;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

@SuppressWarnings("UnstableApiUsage") // brigadier api
public class EventHelper extends JavaPlugin {
	private static EventHelper instance;
	private TNTKnockbackListener tntKnockback;

	public EventHelper() {
		super();

		instance = this;
	}

	public static EventHelper getPlugin() {
		return instance;
	}

	@Override
	public void onEnable() {
		// listener
		PluginManager pm = Bukkit.getPluginManager();

		if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
			tntKnockback = new TNTKnockbackListener(this);
			pm.registerEvents(tntKnockback, this);
			pm.registerEvents(InventoryRegionListener.inst(), this);
		} else {
			this.getLogger().log(Level.WARNING, "WorldGuard was not installed and therefore some features where deactivated.");
		}

		// register commands
		LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
		manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
			final Commands commands = event.registrar();

			new MainCmd(this).register(commands,null, null);
		});
	}

	@Override
	public void onDisable() {
		if (tntKnockback != null){
			tntKnockback.cleanAll();
		}
	}
}
