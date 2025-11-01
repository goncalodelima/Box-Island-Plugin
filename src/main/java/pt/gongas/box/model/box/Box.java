package pt.gongas.box.model.box;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.model.level.BoxLevel;
import pt.gongas.box.util.BoxLocation;

import java.util.*;

public class Box {

    private final UUID boxUuid;
    private final UUID ownerUuid;
    private String boxName;
    private String ownerName;
    private BoxLocation centerBoxLocation;
    private BoxLevel boxLevel;
    private final String firstTime;
    private String lastTime;
    private final Map<UUID, String> playerNameByUuid = new HashMap<>();
    private final Map<String, UUID> uuidByPlayerName = new HashMap<>();
    private final Map<String, String> playerNameByLowercaseName = new HashMap<>();
    private final Map<UUID, Integer> positionByPlayerUuid = new HashMap<>();
    private final Map<Integer, UUID> playerUuidByPosition = new HashMap<>();
    private final Deque<Integer> freePositions = new ArrayDeque<>();
    private int nextPosition = 0;
    private World world;

    public Box(UUID ownerUuid, String ownerName, BoxLevel boxLevel, String firstTime, String lastTime) {
        this.boxUuid = UUID.randomUUID();
        this.ownerUuid = ownerUuid;
        this.boxName = ownerName;
        this.ownerName = ownerName;
        this.boxLevel = boxLevel;
        this.firstTime = firstTime;
        this.lastTime = lastTime;
    }

    public Box(UUID boxUuid, UUID ownerUuid, String ownerName, BoxLevel boxLevel, String firstTime, String lastTime) {
        this.boxUuid = boxUuid;
        this.ownerUuid = ownerUuid;
        this.boxName = ownerName;
        this.ownerName = ownerName;
        this.boxLevel = boxLevel;
        this.firstTime = firstTime;
        this.lastTime = lastTime;
    }

    public UUID getBoxUuid() {
        return boxUuid;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getBoxName() {
        return boxName;
    }

    public void setBoxName(String boxName) {
        this.boxName = boxName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public BoxLocation getCenterBoxLocation() {
        return centerBoxLocation;
    }

    public void setCenterBoxLocation(BoxLocation centerBoxLocation) {
        this.centerBoxLocation = centerBoxLocation;
    }

    public BoxLevel getBoxLevel() {
        return boxLevel;
    }

    public void setBoxLevel(BoxLevel boxLevel) {
        this.boxLevel = boxLevel;
    }

    public Map<UUID, String> getPlayerNameByUuid() {
        return playerNameByUuid;
    }

    public Map<String, UUID> getUuidByPlayerName() {
        return uuidByPlayerName;
    }

    public Map<String, String> getPlayerNameByLowercaseName() {
        return playerNameByLowercaseName;
    }

    public Map<UUID, Integer> getPositionByPlayerUuid() {
        return positionByPlayerUuid;
    }

    public Map<Integer, UUID> getPlayerUuidByPosition() {
        return playerUuidByPosition;
    }

    public int getNextPosition() {

        if (!freePositions.isEmpty()) {
            return freePositions.pop();
        }

        return nextPosition++;
    }

    public String getFirstTime() {
        return firstTime;
    }

    public String getLastTime() {
        return lastTime;
    }

    public void setLastTime(String lastTime) {
        this.lastTime = lastTime;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public void addPlayer(UUID playerUuid, String name, int pos) {
        positionByPlayerUuid.put(playerUuid, pos);
        playerUuidByPosition.put(pos, playerUuid);
        playerNameByLowercaseName.put(name.toLowerCase(), name);
        playerNameByUuid.put(playerUuid, name);
        uuidByPlayerName.put(name, playerUuid);
        Bukkit.getAsyncScheduler().runNow(BoxPlugin.plugin, (task) -> BoxPlugin.boxListByPlayerUuid.put(playerUuid, new Object[]{boxUuid, ownerUuid, ownerName, boxLevel.level(), playerNameByUuid.size() + 1, firstTime, lastTime}));
    }

    public void addPlayer(UUID playerUuid, String name) {
        int pos = getNextPosition();
        positionByPlayerUuid.put(playerUuid, pos);
        playerUuidByPosition.put(pos, playerUuid);
        playerNameByLowercaseName.put(name.toLowerCase(), name);
        playerNameByUuid.put(playerUuid, name);
        uuidByPlayerName.put(name, playerUuid);
        Bukkit.getAsyncScheduler().runNow(BoxPlugin.plugin, (task) -> BoxPlugin.boxListByPlayerUuid.put(playerUuid, new Object[]{boxUuid, ownerUuid, ownerName, boxLevel.level(), playerNameByUuid.size() + 1, firstTime, lastTime}));
    }

    public void removePlayer(UUID playerUuid) {

        Integer pos = positionByPlayerUuid.remove(playerUuid);

        if (pos != null) {
            freePositions.push(pos);
            String realName = playerNameByUuid.remove(playerUuid);
            uuidByPlayerName.remove(realName);
            playerNameByLowercaseName.remove(realName.toLowerCase());
            playerUuidByPosition.remove(pos);
            Bukkit.getAsyncScheduler().runNow(BoxPlugin.plugin, task -> BoxPlugin.boxListByPlayerUuid.remove(playerUuid, boxUuid));
        }

    }

    public UUID removePlayer(String realName) {

        UUID uuid = uuidByPlayerName.remove(realName);
        Integer pos = positionByPlayerUuid.remove(uuid);

        freePositions.push(pos);
        playerNameByUuid.remove(uuid);
        playerNameByLowercaseName.remove(realName.toLowerCase());
        playerUuidByPosition.remove(pos);

        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Box box)) return false;
        return Objects.equals(boxUuid, box.boxUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(boxUuid);
    }

}
