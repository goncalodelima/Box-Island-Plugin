package pt.gongas.box.model.level.adapter;

import org.bukkit.configuration.ConfigurationSection;
import pt.gongas.box.model.level.BoxLevel;
import pt.gongas.box.util.config.ConfigurationAdapter;

public class BoxLevelAdapter implements ConfigurationAdapter<BoxLevel> {

    @Override
    public BoxLevel adapt(ConfigurationSection section) {

        int level = section.getInt("level");
        int size = section.getInt("size");
        int money = section.getInt("money");

        return new BoxLevel(level, size, money);
    }

}
