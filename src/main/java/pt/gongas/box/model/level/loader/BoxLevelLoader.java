package pt.gongas.box.model.level.loader;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import pt.gongas.box.model.level.BoxLevel;
import pt.gongas.box.model.level.adapter.BoxLevelAdapter;

import java.util.ArrayList;
import java.util.List;

public class BoxLevelLoader {

    private final Configuration config;

    private final BoxLevelAdapter adapter = new BoxLevelAdapter();

    public static int MAX_LEVEL;

    public BoxLevelLoader(Configuration config) {
        this.config = config;
    }

    public List<BoxLevel> setup() {

        ConfigurationSection section = config.getConfigurationSection("");

        if (section == null)
            return List.of();

        List<BoxLevel> levels = new ArrayList<>();

        for (String key : section.getKeys(false)) {

            BoxLevel level = this.adapter.adapt(section.getConfigurationSection(key));

            if (level != null) {

                levels.add(level);

                int verify = level.level();

                if (verify > MAX_LEVEL) {
                    MAX_LEVEL = verify;
                }

            }

        }

        return levels;
    }


}
