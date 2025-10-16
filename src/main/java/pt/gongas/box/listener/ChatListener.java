package pt.gongas.box.listener;

import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.controller.ChatController;
import pt.gongas.box.manager.BoxManager;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.service.BoxFoundationService;
import pt.gongas.box.util.config.Configuration;

import java.util.UUID;
import java.util.logging.Level;

public class ChatListener implements Listener {

    private final BoxPlugin plugin;

    private final BoxFoundationService boxService;

    private final BoxManager boxManager;

    private final Configuration lang;

    public ChatListener(BoxPlugin plugin, BoxFoundationService boxService, BoxManager boxManager, Configuration lang) {
        this.plugin = plugin;
        this.boxService = boxService;
        this.boxManager = boxManager;
        this.lang = lang;
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (ChatController.ADD_FRIENDS.contains(uuid)) {

            event.setCancelled(true);

            ChatController.ADD_FRIENDS.remove(uuid);

            Component message = event.message();
            String target = PlainTextComponentSerializer.plainText().serialize(message).trim().split(" ")[0];

            Bukkit.getLogger().log(Level.INFO, "target: " + target);

            Box box = boxService.getBoxByOwnerUuid(player.getUniqueId());

            if (box == null) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("wait", "<red>Wait...")));
                return;
            }

            SlimeWorldInstance slimeWorldInstance = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld(player.getUniqueId().toString());

            if (slimeWorldInstance == null) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("congrulations", "<red>You did the impossible! Please contact an administrator to report this error.")));
                return;
            }

            boxManager.addFriends(player, target, box);

        } else if (ChatController.RENAME_BOX.contains(uuid)) {

            event.setCancelled(true);

            ChatController.RENAME_BOX.remove(uuid);

            Component message = event.message();
            String newName = PlainTextComponentSerializer.plainText().serialize(message);

            Bukkit.getLogger().log(Level.INFO, "newName: " + newName);

            new BukkitRunnable() {
                @Override
                public void run() {

                    Box box = boxService.getBoxByOwnerUuid(player.getUniqueId());

                    if (box == null) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("wait", "<red>Wait...")));
                        return;
                    }

                    SlimeWorldInstance slimeWorldInstance = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld(player.getUniqueId().toString());

                    if (slimeWorldInstance == null) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("congrulations", "<red>You did the impossible! Please contact an administrator to report this error.")));
                        return;
                    }

                    boxManager.rename(box, player, newName);

                }
            }.runTask(plugin);

        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        ChatController.ADD_FRIENDS.remove(uuid);
        ChatController.RENAME_BOX.remove(uuid);
    }

}
