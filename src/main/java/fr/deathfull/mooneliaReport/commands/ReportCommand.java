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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReportCommand implements CommandExecutor {

    private final MooneliaReport plugin;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

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
            player.sendMessage(colorize(plugin.getConfig().getString("messages.usage-header", "&6&m                    &r &e&lREPORT &6&m                    &r")));
            player.sendMessage("");
            player.sendMessage(colorize(plugin.getConfig().getString("messages.usage-simple", "&7▸ &e/report &f<message> &8- &7Report simple")));
            player.sendMessage(colorize(plugin.getConfig().getString("messages.usage-target", "&7▸ &e/report &b<joueur> &f<message> &8- &7Cibler un joueur &8(optionnel)")));
            player.sendMessage("");
            player.sendMessage(colorize(plugin.getConfig().getString("messages.usage-examples", "&6Exemples:")));
            player.sendMessage(colorize(plugin.getConfig().getString("messages.usage-example1", "&7  • &f/report Il y a un bug au spawn")));
            player.sendMessage(colorize(plugin.getConfig().getString("messages.usage-example2", "&7  • &f/report Steve Il utilise des cheats")));
            player.sendMessage("");
            player.sendMessage(colorize(plugin.getConfig().getString("messages.usage-footer", "&6&m                                              &r")));
            return true;
        }

        int cooldownSeconds = plugin.getConfig().getInt("discord.cooldown-seconds", 60);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(uuid);
        if (lastUse != null) {
            long elapsed = now - lastUse;
            long remainingMs = cooldownSeconds * 1000L - elapsed;
            if (remainingMs > 0) {
                long remainingSec = (remainingMs + 999) / 1000;
                String cooldownMsg = plugin.getConfig().getString("messages.cooldown",
                        "&cVous devez attendre {seconds} secondes avant de refaire un report.");
                cooldownMsg = cooldownMsg.replace("{seconds}", String.valueOf(remainingSec));
                player.sendMessage(colorize(cooldownMsg));
                return true;
            }
        }

        String targetPlayer = null;
        int messageStartIndex = 0;

        Player firstArgPlayer = plugin.getServer().getPlayer(args[0]);
        if (firstArgPlayer != null && firstArgPlayer.isOnline()) {
            targetPlayer = firstArgPlayer.getName();
            messageStartIndex = 1;

            if (args.length == 1) {
                player.sendMessage(colorize("&c✗ Vous devez ajouter un message après le nom du joueur !"));
                player.sendMessage("");
                player.sendMessage(colorize("&7Exemple: &f/report &b" + targetPlayer + " &fIl triche"));
                return true;
            }
        }

        StringBuilder reportMessage = new StringBuilder();
        for (int i = messageStartIndex; i < args.length; i++) {
            reportMessage.append(args[i]).append(" ");
        }

        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");

        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK")) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.webhook-not-configured",
                    "&cLe webhook Discord n'est pas configuré !")));
            plugin.getLogger().warning("Le webhook Discord n'est pas configuré dans config.yml !");
            return true;
        }

        cooldowns.put(uuid, System.currentTimeMillis());
        int ticks = Math.max(0, cooldownSeconds) * 20;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> cooldowns.remove(uuid), ticks);

        String finalTargetPlayer = targetPlayer;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DiscordWebhook webhook = new DiscordWebhook(webhookUrl);
                webhook.sendReport(player.getName(), reportMessage.toString().trim(), finalTargetPlayer);

                // Message de confirmation différent selon si un joueur est ciblé
                if (finalTargetPlayer != null) {
                    String confirmMsg = plugin.getConfig().getString("messages.report-sent-with-target",
                            "&a✓ Report envoyé concernant &e{target}&a !");
                    confirmMsg = confirmMsg.replace("{target}", finalTargetPlayer);
                    player.sendMessage(colorize(confirmMsg));
                } else {
                    player.sendMessage(colorize(plugin.getConfig().getString("messages.report-sent-simple",
                            "&a✓ Report envoyé !")));
                    // Petit rappel subtil de la possibilité de cibler un joueur
                    player.sendMessage(colorize("&8💡 Astuce: Vous pouvez cibler un joueur avec &7/report <joueur> <message>"));
                }

                String logMessage = "Report envoyé par " + player.getName();
                if (finalTargetPlayer != null) {
                    logMessage += " concernant " + finalTargetPlayer;
                }
                logMessage += ": " + reportMessage.toString().trim();
                plugin.getLogger().info(logMessage);

            } catch (IOException e) {
                player.sendMessage(colorize(plugin.getConfig().getString("messages.report-error",
                        "&cUne erreur est survenue lors de l'envoi du report.")));

                plugin.getLogger().severe("Erreur lors de l'envoi du webhook Discord: " + e.getMessage());
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                plugin.getLogger().severe(sw.toString());
            }
        });

        return true;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
