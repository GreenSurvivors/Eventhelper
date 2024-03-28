package de.greensurvivors.eventhelper;

import de.greensurvivors.eventhelper.listeners.InventoryRegionListener;
import de.greensurvivors.eventhelper.listeners.TNTKnockbackListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class Eventhelper extends JavaPlugin {
	private static Eventhelper instance;
	private TNTKnockbackListener tntKnockback;

	public Eventhelper() {
		super();

		instance = this;
	}

	public static Eventhelper getPlugin() {
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
	}

	@Override
	public void onDisable() {
		if (tntKnockback != null){
			tntKnockback.cleanAll();
		}
	}
}
