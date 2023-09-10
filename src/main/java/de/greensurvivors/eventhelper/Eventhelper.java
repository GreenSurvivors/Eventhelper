package de.greensurvivors.eventhelper;

import de.greensurvivors.eventhelper.listeners.TNTKnockback;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class Eventhelper extends JavaPlugin {
	private TNTKnockback tntKnockback;

	@Override
	public void onEnable() {
		// listener
		PluginManager pm = Bukkit.getPluginManager();

		if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
			tntKnockback = new TNTKnockback(this);
			pm.registerEvents(tntKnockback, this);
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
