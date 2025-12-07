package pt.gongas.box.model.box.service;

import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.BoxData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface BoxFoundationService {

    CompletableFuture<Box> createBox(UUID playerUuid, String playerName, UUID ownerUuid, boolean visitBetweenServers);

    Box get(UUID ownerUuid);

    boolean update(Map<Box, BoxData> boxes);

    CompletableFuture<Boolean> addMember(Box box, UUID memberUuid, String memberName, int position);

    CompletableFuture<Boolean> removeMember(Box box, UUID memberUuid);

    void addPendingUpdates(Box box, BoxData boxData);

    void removePendingUpdates(Box box);

    Map<Box, BoxData> getPendingUpdates();

    Map<UUID, Box> getAll();

}
