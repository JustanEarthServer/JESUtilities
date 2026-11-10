package dev.ciel.jESUtilities.autoboat;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class AutoboatCommand implements BasicCommand {

    private static final List<String> OPTIONS = List.of("on", "off");

    private final AutoboatManager manager;

    public AutoboatCommand(AutoboatManager manager) {
        this.manager = manager;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return;
        }

        String option = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (option) {
            case "on" -> turnOn(player);
            case "off" -> turnOff(player);
            case "" -> {
                if (manager.isActive(player)) {
                    turnOff(player);
                } else {
                    turnOn(player);
                }
            }
            default -> player.sendMessage(Component.text("Usage: /autoboat [on|off]", NamedTextColor.RED));
        }
    }

    private void turnOn(Player player) {
        if (manager.isActive(player)) {
            player.sendMessage(Component.text("Autoboat is already on.", NamedTextColor.YELLOW));
        } else if (!manager.enable(player)) {
            player.sendMessage(Component.text("You need to be steering a boat to use autoboat.", NamedTextColor.RED));
        }
    }

    private void turnOff(Player player) {
        if (!manager.disable(player)) {
            player.sendMessage(Component.text("Autoboat is not on.", NamedTextColor.YELLOW));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length > 1) {
            return List.of();
        }
        String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        return OPTIONS.stream().filter(option -> option.startsWith(prefix)).toList();
    }

    @Override
    public String permission() {
        return "jesutilities.autoboat";
    }
}
