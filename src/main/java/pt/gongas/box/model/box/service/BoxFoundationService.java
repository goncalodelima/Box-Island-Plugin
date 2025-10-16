package pt.gongas.box.model.box.service;

import org.bukkit.entity.Player;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.BoxData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface BoxFoundationService {

    void createBox(Player player, UUID playerUuid, String playerName, UUID boxUuid, UUID ownerUuid, boolean visitBetweenServers);

    Box get(UUID boxUuid);

    Box getBoxByOwnerUuid(UUID ownerUuid);

    boolean update(Map<Box, BoxData> boxes);

    void addPendingUpdates(Box box, BoxData boxData);

    void removePendingUpdates(Box box);

    Map<Box, BoxData> getPendingUpdates();

}
