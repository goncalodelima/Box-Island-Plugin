package pt.gongas.box.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.inventory.BoxInventory;
import pt.gongas.box.manager.BoxManager;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.service.BoxFoundationService;
import pt.gongas.box.util.config.Configuration;

import java.util.UUID;

@CommandAlias("box|boxs|boxes|zone|zones")
public class BoxCommand extends BaseCommand {

    private final BoxPlugin plugin;

    private final BoxFoundationService boxService;

    private final BoxManager boxManager;

    private final Configuration lang;

    private final BoxInventory boxInventory;

    public BoxCommand(BoxPlugin plugin, BoxFoundationService boxService, BoxManager boxManager, Configuration lang, BoxInventory boxInventory) {
        this.plugin = plugin;
        this.boxService = boxService;
        this.boxManager = boxManager;
        this.lang = lang;
        this.boxInventory = boxInventory;
    }

    @Default
    @Description("Opens the Box Main Menu")
    public void mainMenu(Player player) {

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

        boxInventory.openMenu(player, box);
    }

    @Subcommand("go|tp|teleport")
    @Description("Teleport to your box")
    public void teleport(Player player) {

        UUID playerUuid = player.getUniqueId();
        Box box = boxService.getBoxByOwnerUuid(playerUuid);

        if (box == null) {

            new BukkitRunnable() {
                @Override
                public void run() {

                    UUID boxUuid = BoxPlugin.ownerToBox.get(playerUuid);
                    BoxPlugin.boxServers.get(boxUuid);

                }
            }.runTaskAsynchronously(plugin);

            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("wait", "<red>Wait...")));
            return;
        }

        SlimeWorldInstance slimeWorldInstance = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld(playerUuid.toString());

        if (slimeWorldInstance == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("congrulations", "<red>You did the impossible! Please contact an administrator to report this error.")));
            return;
        }

        boxManager.teleport(player, box, slimeWorldInstance);
    }

    @Subcommand("rename")
    @Description("Rename your box")
    public void rename(Player player, String newName) {

        Box box = boxService.getBoxByOwnerUuid(player.getUniqueId());

        if (box == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("need-stay-box", "<red>Teleport to your box (/box tp) and type the command again.")));
            return;
        }

        SlimeWorldInstance slimeWorldInstance = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld(player.getUniqueId().toString());

        if (slimeWorldInstance == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("congrulations", "<red>You did the impossible! Please contact an administrator to report this error.")));
            return;
        }

        boxManager.rename(box, player, newName);
    }

    @Subcommand("friends|friend|team|teams add")
    @Description("Add friends to your box")
    @Syntax("<target>")
    @CommandCompletion("@players")
    public void addFriends(Player player, String target) {

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

        boxManager.addFriendsForceAsync(player, target, box);
    }

    @Subcommand("friends|friend|team|teams remove")
    @Description("Remove friends from your box")
    @Syntax("<target>")
    @CommandCompletion("@players")
    public void removeFriends(Player player, String target) {

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

        boxManager.removeFriend(box, target, player);

    }

    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

}
