package pt.gongas.box.listener;

import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.manager.BoxManager;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.service.BoxFoundationService;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final BoxManager boxManager;

    private final BoxFoundationService boxService;

    public PlayerListener(BoxManager boxManager, BoxFoundationService boxService) {
        this.boxManager = boxManager;
        this.boxService = boxService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        BoxPlugin.plugin.getRedisExecutor().submit(() -> {

           String value = BoxPlugin.boxServers.get(playerUuid);

            System.out.println("A");
            System.out.println("sv id: " + BoxPlugin.serverId);
            System.out.println("value: " + value);

            if (BoxPlugin.serverId.equals(value)) {

                System.out.println("B");

                new BukkitRunnable() {
                    @Override
                    public void run() {

                        Box box = boxService.get(playerUuid);

                        if (box == null) {
                            System.out.println("C");
                            return;
                        }

                        SlimeWorldInstance slimeWorldInstance = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld(playerUuid.toString());

                        if (slimeWorldInstance == null) {
                            System.out.println("D");
                            return;
                        }

                        System.out.println("E");
                        boxManager.teleport(player, box, slimeWorldInstance.getBukkitWorld(), false);

                    }
                }.runTask(BoxPlugin.plugin);

            } else {

                System.out.println("E");

                UUID boxUuid = BoxPlugin.boxUuidByPlayerUuid.get(playerUuid);

                if (boxUuid == null) {
                    System.out.println("F");
                    return;
                }

                System.out.println("G");

                new BukkitRunnable() {
                    @Override
                    public void run() {

                        Box box = boxService.get(boxUuid);

                        if (box == null) {
                            System.out.println("J");
                            return;
                        }

                        System.out.println("K");
                        boxManager.teleport(player, box, box.getWorld(), false);

                    }
                }.runTask(BoxPlugin.plugin);

            }

        });

//        RFuture<String> reservedServer = BoxPlugin.serverReservations.getAsync(playerUuid);
//
//        reservedServer.thenAcceptAsync(value -> {
//
//            System.out.println("A");
//            System.out.println("sv id: " + BoxPlugin.serverId);
//            System.out.println("value: " + value);
//
//            if (BoxPlugin.serverId.equals(value)) {
//
//                System.out.println("B");
//
//                new BukkitRunnable() {
//                    @Override
//                    public void run() {
//
//                        Box box = boxService.get(playerUuid);
//
//                        if (box == null) {
//                            System.out.println("C");
//                            return;
//                        }
//
//                        SlimeWorldInstance slimeWorldInstance = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld(playerUuid.toString());
//
//                        if (slimeWorldInstance == null) {
//                            System.out.println("D");
//                            return;
//                        }
//
//                        System.out.println("E");
//                        boxManager.teleport(player, box, slimeWorldInstance.getBukkitWorld(), false);
//
//                    }
//                }.runTask(BoxPlugin.plugin);
//
//            } else {
//
//                System.out.println("E");
//
//                UUID boxUuid = BoxPlugin.boxUuidByPlayerUuid.get(playerUuid);
//
//                if (boxUuid == null) {
//                    System.out.println("F");
//                    return;
//                }
//
//                System.out.println("G");
//
//                new BukkitRunnable() {
//                    @Override
//                    public void run() {
//
//                        Box box = boxService.get(boxUuid);
//
//                        if (box == null) {
//                            System.out.println("J");
//                            return;
//                        }
//
//                        System.out.println("K");
//                        boxManager.teleport(player, box, box.getWorld(), false);
//
//                    }
//                }.runTask(BoxPlugin.plugin);
//
//            }
//
//        });

    }

}
