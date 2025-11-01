package pt.gongas.box;

import co.aikar.commands.BukkitCommandManager;
import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.loaders.file.FileLoader;
import com.minecraftsolutions.database.Database;
import com.minecraftsolutions.database.DatabaseType;
import com.minecraftsolutions.database.connection.DatabaseConnection;
import com.minecraftsolutions.database.credentials.impl.DatabaseCredentialsImpl;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.redisson.api.*;
import org.redisson.client.codec.StringCodec;
import pt.gongas.box.command.BoxCommand;
import pt.gongas.box.command.BoxFriendAcceptCommand;
import pt.gongas.box.command.BoxUpgradeCommand;
import pt.gongas.box.inventory.BoxFriendsInventory;
import pt.gongas.box.inventory.BoxInventory;
import pt.gongas.box.inventory.BoxUpgradeInventory;
import pt.gongas.box.inventory.BoxVisitInventory;
import pt.gongas.box.listener.ChatListener;
import pt.gongas.box.listener.PlayerListener;
import pt.gongas.box.loadbalancing.redirect.RedirectManager;
import pt.gongas.box.manager.BoxManager;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.BoxData;
import pt.gongas.box.model.level.BoxLevel;
import pt.gongas.box.model.level.loader.BoxLevelLoader;
import pt.gongas.box.model.level.service.BoxLevelFoundationService;
import pt.gongas.box.model.level.service.BoxLevelService;
import pt.gongas.box.model.box.service.BoxFoundationService;
import pt.gongas.box.model.box.service.BoxService;
import pt.gongas.box.runnable.BoxRunnable;
import pt.gongas.box.runnable.UpdateRunnable;
import pt.gongas.box.util.BukkitMainThreadExecutor;
import pt.gongas.box.util.Formatter;
import pt.gongas.box.util.config.Configuration;
import pt.gongas.redis.redis.RedisManager;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BoxPlugin extends JavaPlugin {

    private Configuration lang;

    private Configuration level;

    private Configuration inventory;

    public static BoxLevelFoundationService boxLevelService;

    private Database datacenter;

    private BoxFoundationService boxService;

    private BoxManager boxManager;

    private BoxFriendsInventory boxFriendsInventory;

    private BoxUpgradeInventory boxUpgradeInventory;

    private BoxVisitInventory boxVisitInventory;

    private BoxInventory boxInventory;

    public static Economy economy;

    public static World boxWorld;

    private BukkitMainThreadExecutor bukkitMainThreadExecutor;

    public static AdvancedSlimePaperAPI advancedSlimePaperAPI;

    public static SlimeLoader slimeLoader;

    public static String serverId;

    public static Formatter formatter;

    public static RMap<String, Integer> worldCount; // put/remove on the Box server instance

    public static RMap<UUID, UUID> ownerToBox; // put/remove on the Box server instance

    public static RMap<UUID, UUID> boxToOwner; // put/remove on the Box server instance

    public static RMap<UUID, String> boxServers; // put/remove on the Box server instance

    public static RMapCache<UUID, UUID> boxUuidByPlayerUuid; // put/remove on the Box server instance

    public static RMapCache<UUID, String> serverReservations; // put/remove on the Box server instance but cleans automatically

    public static RMap<String, String> nameToUuid; // put/remove on VelocityRedisBridge

    public static RMap<String, String> uuidToName; // put/remove on VelocityRedisBridge

    public static RSetMultimap<UUID, UUID> boxUuidToInvitations; // put/remove on the Box server instance

    public static RSetMultimap<UUID, Object[]> boxListByPlayerUuid; // put/remove on the Box server instance

    public static RTopic boxEvents;

    private List<String> servers;

    private ExecutorService worldExecutor;

    private ExecutorService databaseExecutor;

    private ScheduledExecutorService updateExecutor;

    private DateFormat dateFormat;

    public static BoxPlugin plugin;

    @Override
    public void onEnable() {

        plugin = this;

        saveDefaultConfig();

        formatter = new Formatter();

        serverId = getConfig().getString("server-id");

        lang = new Configuration(this, "lang", "lang.yml");
        lang.saveDefaultConfig();

        level = new Configuration(this, "levels", "levels.yml");
        level.saveDefaultConfig();

        inventory = new Configuration(this, "inventory", "inventory.yml");
        inventory.saveDefaultConfig();

        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        servers = getConfig().getStringList("servers");
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        worldCount = RedisManager.getClient().getMap("server:world-count");
        ownerToBox = RedisManager.getClient().getMap("server:owner-box");
        boxToOwner = RedisManager.getClient().getMap("server:box-owner");
        boxServers = RedisManager.getClient().getMap("server:box-servers", StringCodec.INSTANCE);
        boxUuidByPlayerUuid = RedisManager.getClient().getMapCache("player_uuid_to_box_uuid", StringCodec.INSTANCE);
        serverReservations = RedisManager.getClient().getMapCache("server:reservations", StringCodec.INSTANCE);
        nameToUuid = RedisManager.getClient().getMap("player_name_to_uuid", StringCodec.INSTANCE);
        uuidToName = RedisManager.getClient().getMap("player_uuid_to_name", StringCodec.INSTANCE);
        boxUuidToInvitations = RedisManager.getClient().getSetMultimap("server:box-invitations");
        boxEvents = RedisManager.getClient().getTopic("box-events", StringCodec.INSTANCE);
        boxListByPlayerUuid = RedisManager.getClient().getSetMultimap("box_list_to_uuid");

        bukkitMainThreadExecutor = new BukkitMainThreadExecutor(this);
        worldExecutor = Executors.newFixedThreadPool(4);
        databaseExecutor = Executors.newCachedThreadPool();
        updateExecutor = Executors.newSingleThreadScheduledExecutor();

        boxWorld = Bukkit.getWorld("world");

        advancedSlimePaperAPI = AdvancedSlimePaperAPI.instance();

        slimeLoader = new FileLoader(new File("slime_worlds"));

        datacenter = new DatabaseConnection(new DatabaseCredentialsImpl(
                DatabaseType.MYSQL,
                getConfig().getString("database.host"),
                getConfig().getString("database.port"),
                getConfig().getString("database.database"),
                getConfig().getString("database.username"),
                getConfig().getString("database.password"),
                getConfig().getString("database.file")
        )).setup();

        boxLevelService = new BoxLevelService();
        boxService = new BoxService(this, datacenter);

        boxManager = new BoxManager(this, boxService, lang);

        List<BoxLevel> levels = new BoxLevelLoader(level).setup();
        levels.forEach(level -> boxLevelService.put(level));

        boxFriendsInventory = new BoxFriendsInventory(boxService, boxLevelService, boxManager, inventory, lang);
        boxUpgradeInventory = new BoxUpgradeInventory(boxService, boxLevelService, inventory);
        boxVisitInventory = new BoxVisitInventory(boxService, boxLevelService, this, inventory, lang, boxManager);
        boxInventory = new BoxInventory(boxService, boxLevelService, boxManager, inventory, lang, boxFriendsInventory, boxUpgradeInventory, boxVisitInventory);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        new RedirectManager(servers).setup();

        new BoxRunnable(this).runTaskTimer(this, 20 * 60, 20 * 60);
        new UpdateRunnable(boxService, updateExecutor).start();

        register();

        boxEvents.addListener(String.class, (msg, o) -> {

            String[] parts = o.split(";");

            if (parts[0].equals("createBox")) {

                UUID playerUuid = UUID.fromString(parts[1]);
                String playerName = parts[2];

                RFuture<String> reservedServer = BoxPlugin.serverReservations.getAsync(playerUuid);

                reservedServer.thenAcceptAsync(value -> {

                    if (BoxPlugin.serverId.equals(value)) {

                        boxService.createBox(playerUuid, playerName, playerUuid, playerUuid, false).thenAccept(box -> {

                            if (box != null) {
                                BoxLoader.addPlayerWorld(); // this executes before serverReservations timeout in 99% of cases
                                boxEvents.publishAsync("boxCreated;success;" + serverId + ";" + playerUuid);
                            } else {
                                boxEvents.publishAsync("boxCreated;fail;" + serverId + ";" + playerUuid);
                            }

                        });

                    } else {

                        UUID boxUuid = BoxPlugin.boxUuidByPlayerUuid.get(playerUuid);

                        if (boxUuid == null) {
                            return;
                        }

                        UUID ownerUuid = BoxPlugin.boxToOwner.get(boxUuid);

                        if (ownerUuid == null) {
                            return;
                        }

                        boxService.createBox(playerUuid, playerName, boxUuid, ownerUuid, true).thenAccept(box -> {

                            if (box != null) {
                                BoxLoader.addPlayerWorld(); // this executes before serverReservations timeout in 99% of cases
                                boxEvents.publishAsync("boxCreated;success;" + serverId + ";" + playerUuid);
                            } else {
                                boxEvents.publishAsync("boxCreated;fail;" + serverId + ";" + playerUuid);
                            }

                        });

                    }

                });

            } else if (parts[0].equals("acceptInvite")) {

                UUID boxUuid = UUID.fromString(parts[1]);

                new BukkitRunnable() {
                    @Override
                    public void run() {

                        Box box = boxService.get(boxUuid);

                        if (box != null) {

                            UUID playerUuid = UUID.fromString(parts[2]);
                            String name = parts[3];

                            box.addPlayer(playerUuid, name);
                            boxManager.attemptInviteMember(box, playerUuid, name);

                            boxEvents.publish("validInvite;" + playerUuid + ";" + parts[4]);

                        }

                    }
                }.runTask(this);


            }

        });

    }

    @Override
    public void onDisable() {

        for (SlimeWorldInstance world : BoxPlugin.advancedSlimePaperAPI.getLoadedWorlds()) {

            try {

                UUID uuid = UUID.fromString(world.getName());

                Bukkit.unloadWorld(world.getBukkitWorld(), false);
                BoxPlugin.slimeLoader.deleteWorld(uuid.toString());

            } catch (IllegalArgumentException | UnknownWorldException | IOException ignored) {}

        }

        clearRedis();

        try {
            // Wait for currently executing tasks to finish
            if (!updateExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                // Force shutdown if tasks are not finished in the given time
                updateExecutor.shutdownNow();
                // Wait for tasks to respond to being cancelled
                if (!updateExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Update Executor did not terminate in the specified time.");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            updateExecutor.shutdownNow();
        }

        Map<Box, BoxData> boxes = boxService.getPendingUpdates();

        if (!boxes.isEmpty()) {
            boxService.update(boxes);
        }

        databaseExecutor.shutdown();

        try {
            // Wait for currently executing tasks to finish
            if (!databaseExecutor.awaitTermination(36, TimeUnit.SECONDS)) {
                // Force shutdown if tasks are not finished in the given time
                databaseExecutor.shutdownNow();
                // Wait for tasks to respond to being cancelled
                if (!databaseExecutor.awaitTermination(36, TimeUnit.SECONDS)) {
                    System.err.println("Database Executor did not terminate in the specified time.");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            databaseExecutor.shutdownNow();
        }

        worldExecutor.shutdown();

        try {
            // Wait for currently executing tasks to finish
            if (!worldExecutor.awaitTermination(36, TimeUnit.SECONDS)) {
                // Force shutdown if tasks are not finished in the given time
                worldExecutor.shutdownNow();
                // Wait for tasks to respond to being cancelled
                if (!worldExecutor.awaitTermination(36, TimeUnit.SECONDS)) {
                    System.err.println("World executor did not terminate in the specified time.");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            worldExecutor.shutdownNow();
        }

        datacenter.close();

    }

    private void clearRedis() {

        worldCount.fastRemove(serverId);

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        int size = players.size();

        if (size != 0) {

            UUID[] uuids = new UUID[size];
            int i = 0;

            for (Player player : players) {
                uuids[i++] = player.getUniqueId();
                player.kick(MiniMessage.miniMessage().deserialize("<red>Server restarting"));
            }

            boxUuidByPlayerUuid.fastRemove(uuids);
            boxListByPlayerUuid.fastRemove(uuids);
        }

        Map<UUID, Box> boxes = boxService.getAll();

        if (!boxes.isEmpty()) {

            Set<UUID> boxUuids = boxes.keySet();
            UUID[] boxUuidsArray = boxUuids.toArray(UUID[]::new);

            Map<UUID, UUID> ownersMap = boxToOwner.getAll(boxUuids);
            UUID[] ownerUuids = ownersMap.values().toArray(UUID[]::new);

            boxUuidToInvitations.fastRemove(boxUuidsArray);
            ownerToBox.fastRemove(ownerUuids);
            boxToOwner.fastRemove(boxUuidsArray);
            boxServers.fastRemove(boxUuidsArray);
        }

    }

    private void register() {
        registerCommand();
        registerListener();
    }

    public void registerCommand() {
        BukkitCommandManager commandManager = new BukkitCommandManager(this);
        commandManager.enableUnstableAPI("help");
        commandManager.registerCommand(new BoxFriendAcceptCommand(this, boxManager, boxService, lang));
        commandManager.registerCommand(new BoxCommand(this, boxService, boxManager, lang, boxInventory));
        commandManager.registerCommand(new BoxUpgradeCommand(boxService, boxInventory, boxUpgradeInventory, lang));
    }

    public void registerListener() {
        getServer().getPluginManager().registerEvents(new PlayerListener(boxService), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, boxService, boxManager, lang), this);
    }

    private boolean setupEconomy() {

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return true;
    }

    public List<String> getServers() {
        return servers;
    }

    public BukkitMainThreadExecutor getBukkitMainThreadExecutor() {
        return bukkitMainThreadExecutor;
    }

    public ExecutorService getDatabaseExecutor() {
        return databaseExecutor;
    }

    public ExecutorService getWorldExecutor() {
        return worldExecutor;
    }

    public DateFormat getDateFormat() {
        return dateFormat;
    }

}
