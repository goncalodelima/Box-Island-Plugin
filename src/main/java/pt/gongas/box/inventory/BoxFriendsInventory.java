package pt.gongas.box.inventory;

import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import pt.gongas.box.controller.ChatController;
import pt.gongas.box.manager.BoxManager;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.level.service.BoxLevelFoundationService;
import pt.gongas.box.model.box.service.BoxFoundationService;
import pt.gongas.box.util.config.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoxFriendsInventory {

    private final BoxFoundationService boxService;

    private final BoxLevelFoundationService boxLevelService;

    private final BoxManager boxManager;

    private final Configuration inventory;

    private final Configuration lang;

    private final GuiItem decorationItem;

    private final int backSlot;

    private final int informationSlot;

    private final int closeSlot;

    private final List<Integer> decorationSlots;

    public BoxFriendsInventory(BoxFoundationService boxService, BoxLevelFoundationService boxLevelService, BoxManager boxManager, Configuration inventory, Configuration lang) {
        this.boxService = boxService;
        this.boxLevelService = boxLevelService;
        this.boxManager = boxManager;
        this.inventory = inventory;
        this.lang = lang;
        this.decorationItem = PaperItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).asGuiItem();
        this.backSlot = inventory.getInt("friends.back.slot");
        this.informationSlot = inventory.getInt("friends.information.slot");
        this.closeSlot = inventory.getInt("friends.close.slot");
        this.decorationSlots = inventory.getIntegerList("friends.decoration.slots");
    }

    public void openMenu(Player player, Box box, BoxInventory boxInventory) {

        Gui gui = Gui.gui()
                .rows(inventory.getInt("friends.rows", 6))
                .title(MiniMessage.miniMessage().deserialize(inventory.getString("friends.title", "ʙᴏx ꜰʀɪᴇɴᴅꜱ")))
                .disableItemTake()
                .disableItemPlace()
                .disableItemSwap()
                .disableItemDrop()
                .create();

        List<String> backLoreString = inventory.getStringList("friends.back.lore");
        List<Component> backLoreComponents = new ArrayList<>();

        for (String string : backLoreString) {
            backLoreComponents.add(MiniMessage.miniMessage().deserialize(string));
        }

        gui.setItem(backSlot, PaperItemBuilder.from(Material.ARROW)
                .name(MiniMessage.miniMessage().deserialize(inventory.getString("back.name", "<green>ɢᴏ ʙᴀᴄᴋ")))
                .lore(backLoreComponents)
                .asGuiItem(click -> boxInventory.openMenu(player, box)));

        List<String> informationLoreString = inventory.getStringList("friends.information.lore");
        List<Component> informationLoreComponents = new ArrayList<>();

        for (String string : informationLoreString) {
            informationLoreComponents.add(MiniMessage.miniMessage().deserialize(string));
        }

        gui.setItem(informationSlot, PaperItemBuilder.from(Material.BRUSH)
                .name(MiniMessage.miniMessage().deserialize(inventory.getString("friends.information.name", "<gold>ʙᴏx ꜰʀɪᴇɴᴅ ɪɴꜰᴏʀᴍᴀᴛɪᴏɴ")))
                .lore(informationLoreComponents)
                .asGuiItem());

        for (int decorationSlot : decorationSlots) {
            gui.setItem(decorationSlot, decorationItem);
        }

        List<String> closeLore = inventory.getStringList("close.lore");
        List<Component> closeLoreComponents = new ArrayList<>();

        for (String string : closeLore) {
            closeLoreComponents.add(MiniMessage.miniMessage().deserialize(string));
        }

        gui.setItem(closeSlot, PaperItemBuilder.from(Material.FLOWER_BANNER_PATTERN)
                .name(MiniMessage.miniMessage().deserialize(inventory.getString("close.name", "<red>ᴄʟᴏꜱᴇ ᴍᴇɴᴜ")))
                .lore(closeLoreComponents)
                .asGuiItem(click -> gui.close(player)));

        int slot = 20;

        for (Map.Entry<UUID, String> entry : box.getPlayerNameByUuid().entrySet()) {

            List<Component> occupiedList = new ArrayList<>();

            for (String string : inventory.getStringList("friends.slot.occupied.lore")) {
                occupiedList.add(MiniMessage.miniMessage().deserialize(string));
            }

            int pos = slot - 20;

            GuiItem occupiedItem = PaperItemBuilder.from(Material.PLAYER_HEAD)
                    .name(MiniMessage.miniMessage().deserialize(inventory.getString("friends.slot.occupied.name", "<green>%player%")).replaceText(TextReplacementConfig.builder().matchLiteral("%player%").replacement(entry.getValue()).build()))
                    .lore(occupiedList)
                    .asGuiItem(handleClick(player, box, gui, pos));

            gui.setItem(slot, occupiedItem);
            slot++;
        }

        List<Component> availableList = new ArrayList<>();

        for (String string : inventory.getStringList("friends.slot.available.lore")) {
            availableList.add(MiniMessage.miniMessage().deserialize(string));
        }

        for (int i = slot; i <= 24; i++) {

            int pos = i - 20;

            gui.setItem(i, PaperItemBuilder.from(Material.PLAYER_HEAD)
                    .name(MiniMessage.miniMessage().deserialize(inventory.getString("friends.slot.available.name", "<green>ᴀᴠᴀɪʟᴀʙʟᴇ ꜱʟᴏᴛ")))
                    .lore(availableList)
                    .asGuiItem(handleClick(player, box, gui, pos)));

        }

        gui.open(player);

    }

    private GuiAction<InventoryClickEvent> handleClick(Player player, Box box, Gui gui, int pos) {
        return click -> {

            UUID targetUuid = box.getPlayerUuidByPosition().get(pos);

            if (targetUuid != null) {

                String target = box.getPlayerNameByUuid().get(targetUuid);

                if (click.isLeftClick()) { // expel
                    gui.close(player);
                    boxManager.removeFriend(box, target, player);
                } else if (click.isRightClick()) { // promote

                }

            } else {

                gui.close(player);
                ChatController.ADD_FRIENDS.add(player.getUniqueId());

                for (String string : lang.getStringList("add-friend")) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(string));
                }

                player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

            }

        };
    }

}
