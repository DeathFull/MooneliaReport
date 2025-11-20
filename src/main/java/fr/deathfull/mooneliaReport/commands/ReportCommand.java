package fr.deathfull.mooneliaReport.commands;

import fr.deathfull.mooneliaReport.DiscordWebhook;
import fr.deathfull.mooneliaReport.MooneliaReport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ReportCommand implements CommandExecutor {

    private final MooneliaReport plugin;

    public ReportCommand(MooneliaReport plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Cette commande ne peut être exécutée que par un joueur !").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.usage", "&cUsage: /report <message>")));
            return true;
        }

        StringBuilder reportMessage = new StringBuilder();
        for (String arg : args) {
            reportMessage.append(arg).append(" ");
        }

        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");

        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK")) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.webhook-not-configured",
                    "&cLe webhook Discord n'est pas configuré !")));
            plugin.getLogger().warning("Le webhook Discord n'est pas configuré dans config.yml !");
            return true;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DiscordWebhook webhook = new DiscordWebhook(webhookUrl);
                webhook.sendReport(player.getName(), reportMessage.toString().trim());

                player.sendMessage(colorize(plugin.getConfig().getString("messages.report-sent",
                        "&aVotre report a été envoyé avec succès !")));

                plugin.getLogger().info("Report envoyé par " + player.getName() + ": " + reportMessage.toString().trim());

            } catch (IOException e) {
                player.sendMessage(colorize(plugin.getConfig().getString("messages.report-error",
                        "&cUne erreur est survenue lors de l'envoi du report.")));

                plugin.getLogger().severe("Erreur lors de l'envoi du webhook Discord: " + e.getMessage());
                e.printStackTrace();
            }
        });

        return true;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
