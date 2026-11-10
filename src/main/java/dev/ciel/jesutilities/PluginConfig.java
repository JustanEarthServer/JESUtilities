package dev.ciel.jesutilities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginConfig {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    private int activationTicks;
    private double activationDistance;
    private int stuckCheckTicks;
    private double stuckDistance;
    private double speed;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        activationTicks = ticks(getDouble("autoboat.activation-seconds", false));
        activationDistance = getDouble("autoboat.activation-min-distance", true);
        stuckCheckTicks = ticks(getDouble("autoboat.stuck-check-seconds", false));
        stuckDistance = getDouble("autoboat.stuck-min-distance", true);
        speed = getDouble("autoboat.speed", false);
    }

    public int activationTicks() {
        return activationTicks;
    }

    public double activationDistance() {
        return activationDistance;
    }

    public int stuckCheckTicks() {
        return stuckCheckTicks;
    }

    public double stuckDistance() {
        return stuckDistance;
    }

    public double speed() {
        return speed;
    }

    // null if the message is empty
    public Component message(String key, TagResolver... resolvers) {
        String raw = config.getString("messages." + key, "");
        if (raw == null || raw.isEmpty()) return null;
        return MiniMessage.miniMessage().deserialize(raw, resolvers);
    }

    public void send(CommandSender to, String key, TagResolver... resolvers) {
        Component msg = message(key, resolvers);
        if (msg != null) to.sendMessage(msg);
    }

    private double getDouble(String path, boolean allowZero) {
        double value = config.getDouble(path);
        if (Double.isFinite(value) && (value > 0 || (allowZero && value == 0))) {
            return value;
        }
        double def = config.getDefaults() != null ? config.getDefaults().getDouble(path) : 0;
        plugin.getLogger().warning("Invalid value for " + path + ", using " + def);
        return def;
    }

    private static int ticks(double seconds) {
        return (int) Math.max(1, Math.round(seconds * 20));
    }
}
