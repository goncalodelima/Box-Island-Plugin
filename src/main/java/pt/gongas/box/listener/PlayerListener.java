package pt.gongas.box.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.redisson.api.RFuture;
import pt.gongas.box.BoxLoader;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.model.box.service.BoxFoundationService;

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
        String playerName = player.getName();

        RFuture<String> reservedServer = BoxPlugin.serverReservations.getAsync(playerUuid);

        reservedServer.thenAcceptAsync(value -> {

            if (BoxPlugin.serverId.equals(value)) {
                BoxLoader.addPlayerWorld(playerUuid);
                boxService.createBox(player, playerUuid, playerName, playerUuid, playerUuid, false);
                player.sendMessage("entrastes pelo load balancing.");
            } else {

                UUID boxUuid = BoxPlugin.boxUuidByPlayerUuid.get(playerUuid);
                UUID ownerUuid = BoxPlugin.boxToOwner.get(boxUuid);

                BoxLoader.addPlayerWorld(playerUuid);
                boxService.createBox(player, playerUuid, playerName, boxUuid, ownerUuid, true);
                player.sendMessage("destes /server ou tas a visitar alguem");
            }

        });

    }

}
