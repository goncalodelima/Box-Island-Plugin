package pt.gongas.box.util.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Configuration extends YamlConfiguration {

    private final File file;
    private final JavaPlugin plugin;
    private final String name;
    private final String directory;

    public Configuration(JavaPlugin plugin, String directory, String name) {
        this.directory = directory;
        file = new File((this.plugin = plugin).getDataFolder() + File.separator + this.directory, this.name = name);
    }

    public void reloadConfig() {
        try {
            load(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        try {
            save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveDefaultConfig() {
        plugin.saveResource(this.directory + File.separator + name, false);
        reloadConfig();
    }

}
