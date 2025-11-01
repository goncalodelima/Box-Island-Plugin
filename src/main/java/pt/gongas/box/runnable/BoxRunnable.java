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

                if (Bukkit.getPlayer(uuid) == null) {
                    attemptWorldDelete(uuid, world.getBukkitWorld());
                }

            } catch (IllegalArgumentException ignored) {}

        }

    }

    private void attemptWorldDelete(UUID uuid, World world) {

        if (!Bukkit.isTickingWorlds()) {

            if (!Bukkit.unloadWorld(world, true)) {
                plugin.getLogger().log(Level.INFO, "The world was not unloaded successfully for some reason. BoxRunnable#tryDelete call failed");
            } else {
                BoxLoader.removeBoxServer(uuid);
            }

        } else {
            Bukkit.getScheduler().runTask(plugin, () -> attemptWorldDelete(uuid, world));
        }

    }

}
