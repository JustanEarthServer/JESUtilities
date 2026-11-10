package dev.ciel.jESUtilities.autoboat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

/**
 * Keeps a boat rowing forward on its own once the driver has held the forward key
 * for a while and actually covered some distance.
 */
public final class AutoboatManager implements Listener {

    /** Ticks forward has to be held before autoboat turns on (5 seconds). */
    private static final int ACTIVATION_TICKS = 5 * 20;

    /** Minimum horizontal distance covered while holding forward for autoboat to turn on. */
    private static final double ACTIVATION_MIN_DISTANCE = 8.0;

    /** How often the boat's progress is checked while autoboat is on. */
    private static final int STUCK_CHECK_TICKS = 2 * 20;

    /** Minimum horizontal distance the boat must travel between two progress checks. */
    private static final double STUCK_MIN_DISTANCE = 2.0;

    /** Horizontal speed in blocks per tick, matching the top speed of a normally rowed boat on water. */
    private static final double BOAT_SPEED = 0.4;

    private static final Component ENABLED_MESSAGE = Component.text(
            "Autoboat is now on. To disable, type /autoboat off", NamedTextColor.GREEN);

    private final JavaPlugin plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private BukkitTask task;

    public AutoboatManager(JavaPlugin plugin) {
        this.plugin = plugin;
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
        sessions.clear();
    }

    public boolean isActive(Player player) {
        Session session = sessions.get(player.getUniqueId());
        return session != null && session.active;
    }

    /**
     * Turns autoboat on for a player who is currently driving a boat.
     *
     * @return {@code false} if the player is not driving a boat
     */
    public boolean enable(Player player) {
        Boat boat = drivenBoat(player);
        if (boat == null) {
            return false;
        }
        Session session = sessions.computeIfAbsent(player.getUniqueId(), id -> new Session());
        session.bindTo(boat);
        activate(player, session, boat.getLocation());
        return true;
    }

    /**
     * Turns autoboat off for a player.
     *
     * @return {@code false} if autoboat was not on
     */
    public boolean disable(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null || !session.active) {
            return false;
        }
        deactivate(player, session, null);
        return true;
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            Boat boat = drivenBoat(player);

            if (boat == null) {
                Session session = sessions.remove(id);
                if (session != null && session.active) {
                    sendDisabled(player, "you left the boat");
                }
                continue;
            }

            Session session = sessions.computeIfAbsent(id, key -> new Session());
            if (!boat.getUniqueId().equals(session.boatId)) {
                if (session.active) {
                    deactivate(player, session, "you switched boats");
                }
                session.bindTo(boat);
            }

            Input input = player.getCurrentInput();
            Location location = boat.getLocation();

            if (session.active) {
                tickActive(player, session, boat, input, location);
            } else {
                tickInactive(player, session, input, location);
            }
        }
    }

    private void tickActive(Player player, Session session, Boat boat, Input input, Location location) {
        if (input.isBackward()) {
            deactivate(player, session, "you pressed back");
            return;
        }

        if (++session.ticksSinceCheck >= STUCK_CHECK_TICKS) {
            if (horizontalDistanceSquared(location, session.checkX, session.checkZ)
                    < STUCK_MIN_DISTANCE * STUCK_MIN_DISTANCE) {
                deactivate(player, session, "your boat stopped moving");
                return;
            }
            session.checkpoint(location);
        }

        if (boat.getStatus() == Boat.Status.IN_WATER) {
            double yaw = Math.toRadians(location.getYaw());
            boat.setVelocity(new Vector(-Math.sin(yaw) * BOAT_SPEED, 0.0, Math.cos(yaw) * BOAT_SPEED));
        }
    }

    private void tickInactive(Player player, Session session, Input input, Location location) {
        if (!input.isForward() || input.isBackward()) {
            session.forwardTicks = 0;
            return;
        }

        if (session.forwardTicks == 0) {
            session.startX = location.getX();
            session.startZ = location.getZ();
        }

        if (++session.forwardTicks < ACTIVATION_TICKS) {
            return;
        }

        if (horizontalDistanceSquared(location, session.startX, session.startZ)
                >= ACTIVATION_MIN_DISTANCE * ACTIVATION_MIN_DISTANCE) {
            activate(player, session, location);
        } else {
            // Rowing in place (against a wall, on land, etc). Start a fresh window.
            session.forwardTicks = 0;
        }
    }

    private void activate(Player player, Session session, Location location) {
        boolean wasActive = session.active;
        session.active = true;
        session.forwardTicks = 0;
        session.checkpoint(location);
        if (!wasActive) {
            player.sendMessage(ENABLED_MESSAGE);
        }
    }

    private void deactivate(Player player, Session session, String reason) {
        session.active = false;
        session.forwardTicks = 0;
        sendDisabled(player, reason);
    }

    private static void sendDisabled(Player player, String reason) {
        Component message = Component.text("Autoboat is now off.", NamedTextColor.YELLOW);
        if (reason != null) {
            message = message.append(Component.text(" (" + reason + ")", NamedTextColor.GRAY));
        }
        player.sendMessage(message);
    }

    /** Returns the boat the player is steering, or {@code null} if they are not the driver of a boat. */
    static Boat drivenBoat(Player player) {
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof Boat boat) || !boat.isValid()) {
            return null;
        }
        List<Entity> passengers = boat.getPassengers();
        if (passengers.isEmpty() || !passengers.getFirst().getUniqueId().equals(player.getUniqueId())) {
            return null;
        }
        return boat;
    }

    private static double horizontalDistanceSquared(Location location, double x, double z) {
        double dx = location.getX() - x;
        double dz = location.getZ() - z;
        return dx * dx + dz * dz;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private static final class Session {
        UUID boatId;
        boolean active;
        int forwardTicks;
        double startX;
        double startZ;
        int ticksSinceCheck;
        double checkX;
        double checkZ;

        void bindTo(Boat boat) {
            boatId = boat.getUniqueId();
            active = false;
            forwardTicks = 0;
        }

        void checkpoint(Location location) {
            ticksSinceCheck = 0;
            checkX = location.getX();
            checkZ = location.getZ();
        }
    }
}
