package dev.ciel.jesutilities;

import dev.ciel.jesutilities.autoboat.AutoboatCommand;
import dev.ciel.jesutilities.autoboat.AutoboatManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class JESUtilities extends JavaPlugin {

    private AutoboatManager autoboatManager;

    @Override
    public void onEnable() {
        PluginConfig config = new PluginConfig(this);

        autoboatManager = new AutoboatManager(this, config);
        autoboatManager.start();

        registerCommand("autoboat", "Toggle automatic boat rowing", List.of(),
                new AutoboatCommand(autoboatManager, config));
    }

    @Override
    public void onDisable() {
        if (autoboatManager != null) {
            autoboatManager.stop();
            autoboatManager = null;
        }
    }
}
