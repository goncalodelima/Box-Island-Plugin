package pt.gongas.box.model.box.service;

import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.minecraftsolutions.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.redisson.api.RSet;
import org.redisson.api.RSetMultimap;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.BoxData;
import pt.gongas.box.model.level.BoxLevel;
import pt.gongas.box.model.box.repository.BoxFoundationRepository;
import pt.gongas.box.model.box.repository.BoxRepository;
import pt.gongas.box.util.BoxLocation;
import pt.gongas.box.util.Triple;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BoxService implements BoxFoundationService {

    private final BoxPlugin plugin;

    private final BoxFoundationRepository boxRepository;

    private final Map<UUID, Box> cache = new HashMap<>();

    private final Map<Box, BoxData> pendingUpdates = new ConcurrentHashMap<>();

    public BoxService(BoxPlugin plugin, Database database) {
        this.plugin = plugin;
        boxRepository = new BoxRepository(plugin, database);
        boxRepository.setup();
    }

    @Override
    public CompletableFuture<Box> createBox(UUID playerUuid, String playerName, UUID ownerUuid, boolean visitBetweenServers) {

        Box verify = get(ownerUuid);

        if (verify == null) {

            BoxLevel boxLevel = BoxPlugin.boxLevelService.get(0);

            return boxRepository.findOne(ownerUuid).thenComposeAsync(result -> {

                        boolean needUpdateDate;

                        if (result.success()) {

                            Box value = result.value();
                            Box box = Objects.requireNonNullElseGet(value, () -> {
                                String now = plugin.getDateFormat().format(new Date());
                                return new Box(ownerUuid, playerName, boxLevel, now, now);
                            });

                            if (value == null) {

                                boolean success = boxRepository.insertOrUpdateSync(box);

                                if (!success) {
                                    return CompletableFuture.failedFuture(new IllegalStateException("Failed to save box to Database"));
                                }

                                needUpdateDate = false;
                            } else {
                                needUpdateDate = true;
                            }

                            return CompletableFuture.supplyAsync(() -> loadOrCreateWorld(box, ownerUuid, needUpdateDate), plugin.getWorldExecutor());

                        } else {
                            return CompletableFuture.failedFuture(new IllegalStateException(result.errorMessage()));
                        }

                    }, plugin.getDatabaseExecutor()).thenApplyAsync(triple -> initializeWorld(ownerUuid, triple.first(), triple.second(), triple.first().getBoxLevel(), visitBetweenServers, playerUuid, triple.third()), Bukkit.getScheduler().getMainThreadExecutor(plugin))
                    .exceptionally(e -> null);

        } else {

            return CompletableFuture.supplyAsync(() -> loadOrCreateWorld(verify, ownerUuid, true), plugin.getWorldExecutor())
                    .thenApplyAsync(triple -> initializeWorld(ownerUuid, verify, triple.second(), triple.first().getBoxLevel(), visitBetweenServers, playerUuid, triple.third()), Bukkit.getScheduler().getMainThreadExecutor(plugin))
                    .exceptionally(e -> {
                        plugin.getLogger().log(Level.WARNING, "Failed to create world for player UUID: " + ownerUuid, e.getCause() != null ? e.getCause() : e);
                        return null;
                    });

        }

    }

    @Override
    public Box get(UUID ownerUuid) {
        return cache.get(ownerUuid);
    }

    @Override
    public boolean update(Map<Box, BoxData> boxes) {
        return boxRepository.update(boxes);
    }

    @Override
    public CompletableFuture<Boolean> addMember(Box box, UUID memberUuid, String memberName, int position) {
        return boxRepository.addMember(box, memberUuid, memberName, position);
    }

    @Override
    public CompletableFuture<Boolean> removeMember(Box box, UUID memberUuid) {
        return boxRepository.removeMember(box, memberUuid);
    }

    @Override
    public void addPendingUpdates(Box box, BoxData boxData) {
        pendingUpdates.put(box, boxData);
    }

    @Override
    public void removePendingUpdates(Box box) {
        pendingUpdates.remove(box);
    }

    @Override
    public Map<Box, BoxData> getPendingUpdates() {
        return pendingUpdates;
    }

    @Override
    public Map<UUID, Box> getAll() {
        return cache;
    }

    private Triple<Box, SlimeWorld, Boolean> loadOrCreateWorld(Box box, UUID ownerUuid, boolean needUpdateDate) {

        String uuidString = ownerUuid.toString();

        try {
            SlimeWorld slimeWorld = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld("template1").clone(uuidString, BoxPlugin.slimeLoader);
            return new Triple<>(box, slimeWorld, needUpdateDate);
        } catch (WorldAlreadyExistsException e) {

            SlimeWorld slimeWorld = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld(uuidString);

            if (slimeWorld != null) {
                return new Triple<>(box, slimeWorld, needUpdateDate);
            }

            try {
                slimeWorld = BoxPlugin.advancedSlimePaperAPI.readWorld(BoxPlugin.slimeLoader, uuidString, false, new SlimePropertyMap());
                return new Triple<>(box, slimeWorld, needUpdateDate);
            } catch (UnknownWorldException | IOException | CorruptedWorldException | NewerFormatException ex) {
                throw new CompletionException(ex);
            }

        } catch (IOException | IllegalArgumentException e) {
            throw new CompletionException(e);
        }

    }

    private Box initializeWorld(UUID ownerUuid, Box box, SlimeWorld slimeWorld, BoxLevel boxLevel, boolean visitBetweenServers, UUID playerUuid, boolean needUpdateDate) {

        System.out.println("initializeWorld 0");

        long current = System.currentTimeMillis();

        SlimeWorldInstance slimeWorldInstance = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld(ownerUuid.toString());

        if (slimeWorldInstance == null) {
            System.out.println("initializeWorld 1");
            slimeWorldInstance = BoxPlugin.advancedSlimePaperAPI.loadWorld(slimeWorld, true);
            System.out.println("difference 1: " + (System.currentTimeMillis() - current));
        }

        System.out.println("initializeWorld 2");

        World world = slimeWorldInstance.getBukkitWorld();
        Location location = world.getSpawnLocation();
        WorldBorder border = world.getWorldBorder();
        border.setCenter(location.clone().add(1, 0, 1));
        border.setSize(boxLevel.size());

        if (box.getCenterBoxLocation() == null) {
            System.out.println("set centerbox here 3");
            box.setCenterBoxLocation(new BoxLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()));
            box.setWorld(world);
        }

        cache.put(box.getOwnerUuid(), box);

        String now;

        if (needUpdateDate) {
            now = plugin.getDateFormat().format(new Date());
            box.setLastTime(now);
            pendingUpdates.merge(box, BoxData.withLastTime(now), (boxData, boxData2) -> boxData.merge(boxData2));
        } else {
            now = null;
        }

        BoxPlugin.plugin.getRedisExecutor().submit(() -> {

            BoxPlugin.boxServers.put(box.getOwnerUuid(), BoxPlugin.serverId);

            if (!visitBetweenServers) {
                System.out.println("remove 0");
                BoxPlugin.serverReservations.remove(ownerUuid);
            } else {
                System.out.println("remove 1");
                BoxPlugin.boxUuidByPlayerUuid.remove(playerUuid);
            }

            if (needUpdateDate) {
                updateBoxDateToFriends(playerUuid, box.getOwnerUuid(), now);
            }

        });

        System.out.println("difference 2: " + (System.currentTimeMillis() - current));

        return box;
    }

    private void updateBoxDateToFriends(UUID playerUuid, UUID ownerUuid, String now) {

        Set<UUID> uuids = findPlayersWithOwnerUuid(ownerUuid);

        uuids.add(playerUuid);

        for (UUID uuid : uuids) {

            RSet<Object[]> set = BoxPlugin.boxListByPlayerUuid.get(uuid);
            Object[] target = null;

            for (Object[] arr : set) {
                if (ownerUuid.equals(arr[0])) {
                    target = arr;
                    break;
                }
            }

            if (target != null) {
                set.remove(target);
                target[5] = now;
                set.add(target);
            }

        }

    }

    @SuppressWarnings("unchecked")
    private Set<UUID> findPlayersWithOwnerUuid(UUID ownerUuid) {

        Set<UUID> result = new HashSet<>();
        RSetMultimap<String, Object[]> map = (RSetMultimap<String, Object[]>) (Object) BoxPlugin.boxListByPlayerUuid;

        for (Map.Entry<String, Object[]> entry : map.entries()) {

            if (ownerUuid.equals(entry.getValue()[0])) {
                result.add(UUID.fromString(entry.getKey()));
            }

        }

        return result;
    }

}
