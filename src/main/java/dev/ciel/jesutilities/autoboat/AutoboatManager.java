package dev.ciel.jesutilities.autoboat;

import dev.ciel.jesutilities.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AutoboatManager implements Listener {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final Map<UUID, BoatData> data = new HashMap<>();
    private BukkitTask task;

    public AutoboatManager(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        HandlerList.unregisterAll(this);
        data.clear();
    }

    public boolean isOn(Player player) {
        BoatData d = data.get(player.getUniqueId());
        return d != null && d.on;
    }

    // returns false if the player isn't driving a boat
    public boolean enable(Player player) {
        Boat boat = getBoat(player);
        if (boat == null) return false;

        BoatData d = data.computeIfAbsent(player.getUniqueId(), k -> new BoatData());
        d.reset(boat);
        turnOn(player, d, boat.getLocation());
        return true;
    }

    // returns false if it wasn't on
    public boolean disable(Player player) {
        BoatData d = data.get(player.getUniqueId());
        if (d == null || !d.on) return false;

        turnOff(player, d, null);
        return true;
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Boat boat = getBoat(player);

            if (boat == null) {
                BoatData old = data.remove(player.getUniqueId());
                if (old != null && old.on) {
                    sendOff(player, "reason-left-boat");
                }
                continue;
            }

            BoatData d = data.computeIfAbsent(player.getUniqueId(), k -> new BoatData());
            if (!boat.getUniqueId().equals(d.boat)) {
                if (d.on) turnOff(player, d, "reason-switched-boat");
                d.reset(boat);
            }

            Input input = player.getCurrentInput();
            Location loc = boat.getLocation();

            if (d.on) {
                tickOn(player, d, boat, input, loc);
            } else {
                tickOff(player, d, input, loc);
            }
        }
    }

    private void tickOn(Player player, BoatData d, Boat boat, Input input, Location loc) {
        if (input.isBackward()) {
            turnOff(player, d, "reason-pressed-back");
            return;
        }

        // turn off if the boat hasn't really moved (hit land, stuck on something...)
        d.ticksSinceCheck++;
        if (d.ticksSinceCheck >= config.stuckCheckTicks()) {
            if (distSq(loc, d.checkX, d.checkZ) < square(config.stuckDistance())) {
                turnOff(player, d, "reason-stopped-moving");
                return;
            }
            d.checkpoint(loc);
        }

        if (boat.getStatus() == Boat.Status.IN_WATER) {
            double yaw = Math.toRadians(loc.getYaw());
            double speed = config.speed();
            boat.setVelocity(new Vector(-Math.sin(yaw) * speed, 0, Math.cos(yaw) * speed));
        }
    }

    private void tickOff(Player player, BoatData d, Input input, Location loc) {
        if (!input.isForward() || input.isBackward()) {
            d.forwardTicks = 0;
            return;
        }

        if (d.forwardTicks == 0) {
            d.startX = loc.getX();
            d.startZ = loc.getZ();
        }

        d.forwardTicks++;
        if (d.forwardTicks < config.activationTicks()) return;

        if (distSq(loc, d.startX, d.startZ) >= square(config.activationDistance())) {
            turnOn(player, d, loc);
        } else {
            // rowing but not going anywhere, start counting again
            d.forwardTicks = 0;
        }
    }

    private void turnOn(Player player, BoatData d, Location loc) {
        boolean wasOn = d.on;
        d.on = true;
        d.forwardTicks = 0;
        d.checkpoint(loc);
        if (!wasOn) config.send(player, "enabled");
    }

    private void turnOff(Player player, BoatData d, String reason) {
        d.on = false;
        d.forwardTicks = 0;
        sendOff(player, reason);
    }

    private void sendOff(Player player, String reason) {
        if (reason == null) {
            config.send(player, "disabled-by-command");
            return;
        }
        Component reasonMsg = config.message(reason);
        config.send(player, "disabled", Placeholder.component("reason", reasonMsg != null ? reasonMsg : Component.empty()));
    }

    // the boat the player is steering, null if they're not the driver
    static Boat getBoat(Player player) {
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof Boat boat) || !boat.isValid()) return null;

        List<Entity> passengers = boat.getPassengers();
        if (passengers.isEmpty() || !passengers.getFirst().getUniqueId().equals(player.getUniqueId())) {
            return null;
        }
        return boat;
    }

    private static double distSq(Location loc, double x, double z) {
        double dx = loc.getX() - x;
        double dz = loc.getZ() - z;
        return dx * dx + dz * dz;
    }

    private static double square(double d) {
        return d * d;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        data.remove(event.getPlayer().getUniqueId());
    }

    private static class BoatData {
        UUID boat;
        boolean on;
        int forwardTicks;
        double startX, startZ;
        int ticksSinceCheck;
        double checkX, checkZ;

        void reset(Boat b) {
            boat = b.getUniqueId();
            on = false;
            forwardTicks = 0;
        }

        void checkpoint(Location loc) {
            ticksSinceCheck = 0;
            checkX = loc.getX();
            checkZ = loc.getZ();
        }
    }
}
