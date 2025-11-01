package pt.gongas.box.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.redisson.api.RFuture;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.service.BoxFoundationService;
import pt.gongas.box.util.BoxLocation;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final BoxFoundationService boxService;

    public PlayerListener(BoxFoundationService boxService) {
        this.boxService = boxService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        RFuture<String> reservedServer = BoxPlugin.serverReservations.getAsync(playerUuid);

        reservedServer.thenAcceptAsync(value -> {

            if (BoxPlugin.serverId.equals(value)) {

                Box box = boxService.get(playerUuid);

                if (box == null) {
                    return;
                }

                BoxLocation boxLocation = box.getCenterBoxLocation();
                Location centerLocation = new Location(box.getWorld(), boxLocation.x(), boxLocation.y(), boxLocation.z(), boxLocation.yaw(), boxLocation.pitch());

                player.teleport(centerLocation);

            } else {

                UUID boxUuid = BoxPlugin.boxUuidByPlayerUuid.get(playerUuid);

                if (boxUuid == null) {
                    return;
                }

                UUID ownerUuid = BoxPlugin.boxToOwner.get(boxUuid);

                if (ownerUuid == null) {
                    return;
                }

                Box box = boxService.get(boxUuid);

                if (box == null) {
                    return;
                }

                BoxLocation boxLocation = box.getCenterBoxLocation();
                Location centerLocation = new Location(box.getWorld(), boxLocation.x(), boxLocation.y(), boxLocation.z(), boxLocation.yaw(), boxLocation.pitch());

                player.teleport(centerLocation);
            }

        });

    }

}
