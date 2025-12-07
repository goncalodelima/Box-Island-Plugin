package pt.gongas.box.runnable;

import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import pt.gongas.box.BoxLoader;
import pt.gongas.box.BoxPlugin;

import java.util.UUID;
import java.util.logging.Level;

public class BoxRunnable extends BukkitRunnable {

    private final BoxPlugin plugin;

    public BoxRunnable(BoxPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {

        for (SlimeWorldInstance world : BoxPlugin.advancedSlimePaperAPI.getLoadedWorlds()) {

            try {

                UUID uuid = UUID.fromString(world.getName());
                World bukkitWorld = world.getBukkitWorld();

                System.out.println("fulltime: " + bukkitWorld.getFullTime());

                if (bukkitWorld.getFullTime() > BoxPlugin.UNLOAD_WORLD_COOLDOWN) {

                    if (Bukkit.getPlayer(uuid) == null) {
                        attemptWorldDelete(uuid, world.getBukkitWorld());
                    }

                }

            } catch (IllegalArgumentException ignored) {}

        }

    }

    private void attemptWorldDelete(UUID uuid, World world) {

        if (!Bukkit.isTickingWorlds()) {

            if (!Bukkit.unloadWorld(world, true)) {
                plugin.getLogger().log(Level.INFO, "The world was not unloaded successfully for some reason. BoxRunnable#tryDelete call failed");
            } else {
                BoxPlugin.plugin.getRedisExecutor().submit(() -> BoxLoader.removeBoxServer(uuid));
            }

        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> attemptWorldDeleteCallback(uuid, world), 20 * 5);
        }

    }

    private void attemptWorldDeleteCallback(UUID uuid, World world) {

        if (Bukkit.getPlayer(uuid) != null) {
            return;
        }

        if (!Bukkit.isTickingWorlds()) {

            if (!Bukkit.unloadWorld(world, true)) {
                plugin.getLogger().log(Level.INFO, "The world was not unloaded successfully for some reason. BoxRunnable#tryDelete call failed");
            } else {
                BoxPlugin.plugin.getRedisExecutor().submit(() -> BoxLoader.removeBoxServer(uuid));
            }

        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> attemptWorldDeleteCallback(uuid, world), 20 * 5);
        }

    }

}
