package pt.gongas.box.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public class BukkitMainThreadExecutor implements Executor {

    private final BukkitScheduler scheduler;
    private final Plugin plugin;

    public BukkitMainThreadExecutor(Plugin plugin) {
        this.scheduler = Bukkit.getScheduler();
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull Runnable command) {
        scheduler.runTask(plugin, command);
    }

}