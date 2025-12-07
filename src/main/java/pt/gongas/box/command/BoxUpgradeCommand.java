package pt.gongas.box.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pt.gongas.box.inventory.BoxInventory;
import pt.gongas.box.inventory.BoxUpgradeInventory;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.service.BoxFoundationService;
import pt.gongas.box.util.config.Configuration;

@CommandAlias("upgrade|upgrades")
public class BoxUpgradeCommand extends BaseCommand {

    private final BoxFoundationService boxService;

    private final BoxInventory boxInventory;

    private final BoxUpgradeInventory boxUpgradeInventory;

    private final Configuration lang;

    public BoxUpgradeCommand(BoxFoundationService boxService, BoxInventory boxInventory, BoxUpgradeInventory boxUpgradeInventory, Configuration lang) {
        this.boxService = boxService;
        this.boxInventory = boxInventory;
        this.boxUpgradeInventory = boxUpgradeInventory;
        this.lang = lang;
    }

    @Default
    @Description("Upgrade the size of your box")
    public void mainMenu(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            return;
        }

        Box box = boxService.get(player.getUniqueId());

        if (box == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("wait", "<red>Wait...")));
            return;
        }

        boxUpgradeInventory.openMenu(player, box, boxInventory);
    }

}
