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
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.BoxData;
import pt.gongas.box.model.level.BoxLevel;
import pt.gongas.box.model.box.repository.BoxFoundationRepository;
import pt.gongas.box.model.box.repository.BoxRepository;
import pt.gongas.box.util.BoxLocation;
import pt.gongas.box.util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BoxService implements BoxFoundationService {

    private final BoxPlugin plugin;

    private final BoxFoundationRepository boxRepository;

    private final Database database;

    private final Map<UUID, Box> cache = new HashMap<>();

    private final Map<Box, BoxData> pendingUpdates = new ConcurrentHashMap<>();

    private final Map<UUID, Box> boxByOwnerUuid = new ConcurrentHashMap<>();

    public BoxService(BoxPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        boxRepository = new BoxRepository(plugin, database);
        boxRepository.setup();
    }

    @Override
    public void createBox(Player player, UUID playerUuid, String playerName, UUID boxUuid, UUID ownerUuid, boolean visitBetweenServers) {

        BoxLevel boxLevel = BoxPlugin.boxLevelService.get(0);
        Box verify = get(boxUuid);

        if (verify == null) {

            boxRepository.findOne(boxUuid).thenComposeAsync(result -> {

                        if (result.success()) {


                            Box value = result.value();
                            Box box = Objects.requireNonNullElseGet(value, () -> {
                                String now = plugin.getDateFormat().format(new Date());
                                return new Box(boxUuid, ownerUuid, playerName, boxLevel, now, now);
                            });

                            if (value == null) {

                                boolean success = boxRepository.insertOrUpdateSync(box);

                                if (!success) {
                                    return CompletableFuture.failedFuture(new IllegalStateException("Failed to save box to Database"));
                                }

                            }

                            return CompletableFuture.supplyAsync(() -> loadOrCreateWorld(box, boxUuid), plugin.getWorldExecutor());

                        } else {
                            Bukkit.getScheduler().runTaskLater(plugin, () -> createBox(player, playerUuid, playerName, boxUuid, ownerUuid, visitBetweenServers), 20 * 5);
                            return CompletableFuture.failedFuture(new IllegalStateException(result.errorMessage()));
                        }

                    }, plugin.getDatabaseExecutor()).thenAcceptAsync(pair -> initializeWorld(boxUuid, pair.key(), pair.value(), player, pair.key().getBoxLevel(), visitBetweenServers, playerUuid), plugin.getBukkitMainThreadExecutor())
                    .exceptionally(e -> null);

        } else {

            boxRepository.insertOrUpdate(verify).thenComposeAsync(success -> {

                if (!success) {
                    return CompletableFuture.failedFuture(new IllegalStateException("Failed to save box to Database"));
                }

                return CompletableFuture.supplyAsync(() -> loadOrCreateWorld(verify, boxUuid), plugin.getWorldExecutor());

            }).thenAcceptAsync(pair -> initializeWorld(boxUuid, verify, pair.value(), player, verify.getBoxLevel(), visitBetweenServers, playerUuid), plugin.getBukkitMainThreadExecutor()).exceptionally(e -> {
                plugin.getLogger().log(Level.WARNING, "Failed to create world for player UUID: " + boxUuid, e.getCause() != null ? e.getCause() : e);
                return null;
            });

           CompletableFuture.supplyAsync(() -> loadOrCreateWorld(verify, boxUuid), plugin.getWorldExecutor()).thenAcceptAsync(pair -> initializeWorld(boxUuid, verify, pair.value(), player, pair.key().getBoxLevel(), visitBetweenServers, playerUuid), plugin.getBukkitMainThreadExecutor()).exceptionally(e -> {
                plugin.getLogger().log(Level.WARNING, "Failed to create world for player UUID: " + boxUuid, e.getCause() != null ? e.getCause() : e);
                return null;
            });

        }

    }

    @Override
    public Box get(UUID boxUuid) {
        return cache.get(boxUuid);
    }

    @Override
    public Box getBoxByOwnerUuid(UUID ownerUuid) {
        return boxByOwnerUuid.get(ownerUuid);
    }

    @Override
    public boolean update(Map<Box, BoxData> boxes) {
        return boxRepository.update(boxes);
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

    private Pair<Box, SlimeWorld> loadOrCreateWorld(Box box, UUID boxUuid) {

        String uuidString = boxUuid.toString();

        try {
            SlimeWorld slimeWorld = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld("template1").clone(uuidString, BoxPlugin.slimeLoader);
            return new Pair<>(box, slimeWorld);
        } catch (WorldAlreadyExistsException e) {

            SlimeWorld slimeWorld = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld(uuidString);

            if (slimeWorld != null) {
                return new Pair<>(box, slimeWorld);
            }

            try {
                slimeWorld = BoxPlugin.advancedSlimePaperAPI.readWorld(BoxPlugin.slimeLoader, uuidString, false, new SlimePropertyMap());
                return new Pair<>(box, slimeWorld);
            } catch (UnknownWorldException | IOException | CorruptedWorldException | NewerFormatException ex) {
                throw new CompletionException(ex);
            }

        } catch (IOException | IllegalArgumentException e) {
            throw new CompletionException(e);
        }

    }

    private void initializeWorld(UUID boxUuid, Box box, SlimeWorld slimeWorld, Player player, BoxLevel boxLevel, boolean visitBetweenServers, UUID playerUuid) {

        SlimeWorldInstance slimeWorldInstance = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld(boxUuid.toString());

        if (slimeWorldInstance == null) {
            slimeWorldInstance = BoxPlugin.advancedSlimePaperAPI.loadWorld(slimeWorld, true);
        }

        Location location = slimeWorldInstance.getBukkitWorld().getSpawnLocation();

        box.setCenterBoxLocation(new BoxLocation(location.getX(), location.getY(), location.getZ()));

        WorldBorder border = slimeWorldInstance.getBukkitWorld().getWorldBorder();
        border.setCenter(location.clone().add(1, 0, 1));
        border.setSize(boxLevel.size());

        player.teleport(location);

        cache.put(box.getBoxUuid(), box);
        boxByOwnerUuid.put(box.getOwnerUuid(), box);

        new BukkitRunnable() {
            @Override
            public void run() {

                BoxPlugin.boxServers.put(box.getBoxUuid(), BoxPlugin.serverId);
                BoxPlugin.ownerToBox.put(box.getOwnerUuid(), box.getBoxUuid());
                BoxPlugin.boxToOwner.put(box.getBoxUuid(), box.getOwnerUuid());

                if (!visitBetweenServers) {
                    BoxPlugin.serverReservations.remove(boxUuid);
                } else {
                    BoxPlugin.boxUuidByPlayerUuid.remove(playerUuid);
                }

            }
        }.runTaskAsynchronously(plugin);

    }

}
