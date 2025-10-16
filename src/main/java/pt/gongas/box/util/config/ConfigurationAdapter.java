package pt.gongas.box.util.config;

import org.bukkit.configuration.ConfigurationSection;

public interface ConfigurationAdapter<T> {

    T adapt(ConfigurationSection section);

}
