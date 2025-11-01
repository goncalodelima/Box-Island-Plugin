package pt.gongas.box.model.box;

import java.util.UUID;

public class BoxInfo {

    private final UUID boxUuid;
    private final UUID boxOwnerUuid;
    private final String boxOwner;
    private final int level;
    private final int members;
    private final String firstTime;
    private final String lastTime;

    public BoxInfo(UUID boxUuid, UUID boxOwnerUuid, String boxOwner, int level, int members, String firstTime, String lastTime) {
        this.boxUuid = boxUuid;
        this.boxOwnerUuid = boxOwnerUuid;
        this.boxOwner = boxOwner;
        this.level = level;
        this.members = members;
        this.firstTime = firstTime;
        this.lastTime = lastTime;
    }

    public UUID getBoxUuid() {
        return boxUuid;
    }

    public UUID getBoxOwnerUuid() {
        return boxOwnerUuid;
    }

    public String getBoxOwner() {
        return boxOwner;
    }

    public int getLevel() {
        return level;
    }

    public int getMembers() {
        return members;
    }

    public String getFirstTime() {
        return firstTime;
    }

    public String getLastTime() {
        return lastTime;
    }

}
