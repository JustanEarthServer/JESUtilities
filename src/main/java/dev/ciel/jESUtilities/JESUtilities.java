package dev.ciel.jESUtilities;

import dev.ciel.jESUtilities.autoboat.AutoboatCommand;
import dev.ciel.jESUtilities.autoboat.AutoboatManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class JESUtilities extends JavaPlugin {

    private AutoboatManager autoboatManager;

    @Override
    public void onEnable() {
        autoboatManager = new AutoboatManager(this);
        autoboatManager.start();

        registerCommand("autoboat", "Toggle automatic boat rowing", List.of(), new AutoboatCommand(autoboatManager));
    }

    @Override
    public void onDisable() {
        if (autoboatManager != null) {
            autoboatManager.stop();
            autoboatManager = null;
        }
    }
}
