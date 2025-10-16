package pt.gongas.box.model.level.service;

import pt.gongas.box.model.level.BoxLevel;

import java.util.HashMap;
import java.util.Map;

public class BoxLevelService implements BoxLevelFoundationService {

    private final Map<Integer, BoxLevel> cache = new HashMap<>();

    @Override
    public void put(BoxLevel boxLevel) {
        cache.put(boxLevel.level(), boxLevel);
    }

    @Override
    public void remove(int level) {
        cache.remove(level);
    }

    @Override
    public BoxLevel get(int level) {
        return cache.get(level);
    }

}
