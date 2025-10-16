package pt.gongas.box.model.box.adapter;

import com.minecraftsolutions.database.adapter.DatabaseAdapter;
import com.minecraftsolutions.database.executor.DatabaseQuery;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.level.BoxLevel;
import pt.gongas.box.util.BoxLocation;
import pt.gongas.box.util.UUIDConverter;

import java.util.UUID;

public class BoxAdapter implements DatabaseAdapter<Box> {

    @Override
    public Box adapt(DatabaseQuery query) {

        UUID boxUuid = UUIDConverter.convert((byte[]) query.get("boxUuid"));
        UUID ownerUuid = UUIDConverter.convert((byte[]) query.get("ownerUuid"));
        String boxName = (String) query.get("boxName");
        String ownerName = (String) query.get("ownerName");
        int level = (int) query.get("level");
        String firstTime = (String) query.get("firstTime");
        String lastTime = (String) query.get("lastTime");

        BoxLocation centerLocation;

        try {
            centerLocation = BoxLocation.deserialize((String) query.get("centerLocation"));
        } catch (IllegalArgumentException e) {
            centerLocation = null;
        }

        BoxLevel boxLevel = BoxPlugin.boxLevelService.get(level);

        Box box = new Box(boxUuid, ownerUuid, ownerName, boxLevel, firstTime, lastTime);
        box.setBoxName(boxName);

        if (centerLocation != null) {
            box.setCenterBoxLocation(centerLocation);
        }

        return box;
    }

}
