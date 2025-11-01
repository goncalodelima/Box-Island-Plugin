package pt.gongas.box.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.manager.BoxManager;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.service.BoxFoundationService;
import pt.gongas.box.util.config.Configuration;

import java.util.Set;
import java.util.UUID;

@CommandAlias("box|boxs|boxes|zone|zones")
public class BoxFriendAcceptCommand extends BaseCommand {

    private final BoxPlugin plugin;

    private final BoxManager boxManager;

    private final BoxFoundationService boxService;

    private final Configuration lang;

    public BoxFriendAcceptCommand(BoxPlugin plugin, BoxManager boxManager, BoxFoundationService boxService, Configuration lang) {
        this.plugin = plugin;
        this.boxManager = boxManager;
        this.boxService = boxService;
        this.lang = lang;
    }

    @Subcommand("friends|friend|team|teams accept")
    @Description("Accept friend requests in your box")
    @Syntax("<target>")
    @CommandCompletion("@players")
    public void acceptFriendRequest(Player player, String target) {

        if (player.getName().equalsIgnoreCase(target)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("invite-yourself", "<red>You can't invite yourself")));
            return;
        }

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {

            String uuidString = BoxPlugin.nameToUuid.get(target.toLowerCase());

            if (uuidString == null) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("player-offline-all-network", "<red>The inserted player is not online across our network.")));
                return;
            }

            String name = BoxPlugin.uuidToName.get(uuidString);

            if (name == null) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("player-offline-all-network", "<red>The inserted player is not online across our network.")));
                return;
            }

            UUID ownerUuid = UUID.fromString(uuidString);
            Box box = boxService.getBoxByOwnerUuid(ownerUuid);

            if (box == null) {

                UUID boxUuid = BoxPlugin.ownerToBox.get(ownerUuid);

                if (boxUuid == null) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("target-no-box", "<red>The inserted player no longer has a box.")));
                    return;
                }

                Set<UUID> invitations = BoxPlugin.boxUuidToInvitations.get(boxUuid);

                UUID playerUuid = player.getUniqueId();

                if (!invitations.contains(playerUuid)) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("no-invited", "<red>You have not been invited to the inserted player's box.")));
                    return;
                }

                BoxPlugin.boxEvents.publish("acceptInvite;" + boxUuid + ";" + playerUuid + ";" + player.getName() + ";" + name);

                invitations.remove(playerUuid);

                if (invitations.isEmpty()) {
                    BoxPlugin.boxUuidToInvitations.removeAll(boxUuid);
                }

            } else {


                SlimeWorldInstance slimeWorldInstance = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld(uuidString);

                if (slimeWorldInstance == null) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("congrulations", "<red>You did the impossible! Please contact an administrator to report this error.")));
                    return;
                }

                Set<UUID> invitations = BoxPlugin.boxUuidToInvitations.get(box.getBoxUuid());

                UUID playerUuid = player.getUniqueId();

                if (!invitations.contains(playerUuid)) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("no-invited", "<red>You have not been invited to the inserted player's box.")));
                    return;
                }

                String playerName = player.getName();

                box.addPlayer(playerUuid, playerName);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        boxManager.attemptInviteMember(box, playerUuid, playerName);
                    }
                }.runTask(plugin);

                invitations.remove(playerUuid);

                if (invitations.isEmpty()) {
                    BoxPlugin.boxUuidToInvitations.removeAll(box.getBoxUuid());
                }

                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>You accepted the " + name + " box invitation."));
            }

        });

    }

    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

}
