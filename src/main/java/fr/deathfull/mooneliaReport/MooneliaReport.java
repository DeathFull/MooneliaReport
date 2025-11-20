package fr.deathfull.mooneliaReport;

import fr.deathfull.mooneliaReport.commands.ReportCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MooneliaReport extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getCommand("report").setExecutor(new ReportCommand(this));

        getLogger().info("MooneliaReport has been enabled!");
        getLogger().info("Configuration du webhook Discord chargée.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MooneliaReport has been disabled!");
    }
}
