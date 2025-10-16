package pt.gongas.box.model.level.service;

import pt.gongas.box.model.level.BoxLevel;

public interface BoxLevelFoundationService {

    void put(BoxLevel boxLevel);

    void remove(int level);

    BoxLevel get(int level);

}
