package dev.ciel.jesutilities.autoboat;

import dev.ciel.jesutilities.PluginConfig;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class AutoboatCommand implements BasicCommand {

    private final AutoboatManager manager;
    private final PluginConfig config;

    public AutoboatCommand(AutoboatManager manager, PluginConfig config) {
        this.manager = manager;
        this.config = config;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        String arg = args.length > 0 ? args[0].toLowerCase() : "";

        if (arg.equals("reload") && sender.hasPermission("jesutilities.reload")) {
            config.reload();
            config.send(sender, "reloaded");
            return;
        }

        if (!(sender instanceof Player player)) {
            config.send(sender, "players-only");
            return;
        }

        switch (arg) {
            case "on" -> on(player);
            case "off" -> off(player);
            case "" -> {
                if (manager.isOn(player)) off(player);
                else on(player);
            }
            default -> config.send(player, "usage");
        }
    }

    private void on(Player player) {
        if (manager.isOn(player)) {
            config.send(player, "already-on");
        } else if (!manager.enable(player)) {
            config.send(player, "not-in-boat");
        }
    }

    private void off(Player player) {
        if (!manager.disable(player)) {
            config.send(player, "not-on");
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length > 1) return List.of();

        List<String> options = new ArrayList<>(List.of("on", "off"));
        if (source.getSender().hasPermission("jesutilities.reload")) options.add("reload");

        String typed = args.length > 0 ? args[0].toLowerCase() : "";
        return options.stream().filter(s -> s.startsWith(typed)).toList();
    }

    @Override
    public String permission() {
        return "jesutilities.autoboat";
    }
}
