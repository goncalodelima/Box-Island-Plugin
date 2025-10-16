package pt.gongas.box.inventory;

import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.controller.ChatController;
import pt.gongas.box.manager.BoxManager;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.level.BoxLevel;
import pt.gongas.box.model.level.service.BoxLevelFoundationService;
import pt.gongas.box.model.box.service.BoxFoundationService;
import pt.gongas.box.util.config.Configuration;

import java.util.*;

public class BoxInventory {

    private final BoxFoundationService boxService;

    private final BoxLevelFoundationService boxLevelService;

    private final BoxManager boxManager;

    private final Configuration inventory;

    private final Configuration lang;

    private final BoxFriendsInventory boxFriendsInventory;

    private final BoxUpgradeInventory boxUpgradeInventory;

    private final BoxVisitInventory boxVisitInventory;

    private final GuiItem decorationItem;

    private final int closeSlot;

    private final int sethomeSlot;

    private final int teleportSlot;

    private final int addedSlot;

    private final int friendsSlot;

    private final int renameSlot;

    private final int upgradeSlot;

    private final List<Integer> decorationSlots;

    public BoxInventory(BoxFoundationService boxService, BoxLevelFoundationService boxLevelService, BoxManager boxManager, Configuration inventory, Configuration lang, BoxFriendsInventory boxFriendsInventory, BoxUpgradeInventory boxUpgradeInventory, BoxVisitInventory boxVisitInventory) {
        this.boxService = boxService;
        this.boxLevelService = boxLevelService;
        this.boxManager = boxManager;
        this.inventory = inventory;
        this.lang = lang;
        this.boxFriendsInventory = boxFriendsInventory;
        this.boxUpgradeInventory = boxUpgradeInventory;
        this.boxVisitInventory = boxVisitInventory;
        this.decorationItem = PaperItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).asGuiItem();
        this.closeSlot = inventory.getInt("main.close.slot");
        this.sethomeSlot = inventory.getInt("main.sethome.slot");
        this.teleportSlot = inventory.getInt("main.teleport.slot");
        this.addedSlot = inventory.getInt("main.added.slot");
        this.friendsSlot = inventory.getInt("main.friends.slot");
        this.renameSlot = inventory.getInt("main.rename.slot");
        this.upgradeSlot = inventory.getInt("main.upgrade.slot");
        this.decorationSlots = inventory.getIntegerList("main.decoration.slots");
    }

    public void openMenu(Player player, Box box) {

        double balance = BoxPlugin.economy.getBalance(player);

        BoxLevel boxLevel = box.getBoxLevel();
        int level = boxLevel.level();

        Gui gui = Gui.gui()
                .rows(inventory.getInt("main.rows", 6))
                .title(MiniMessage.miniMessage().deserialize(inventory.getString("main.title", "ʙᴏx ᴜᴘɢʀᴀᴅᴇꜱ")))
                .disableItemTake()
                .disableItemPlace()
                .disableItemSwap()
                .disableItemDrop()
                .create();

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

        List<String> sethomeLore = inventory.getStringList("main.sethome.lore");
        List<Component> sethomeLoreComponents = new ArrayList<>();

        for (String string : sethomeLore) {
            sethomeLoreComponents.add(MiniMessage.miniMessage().deserialize(string));
        }

        gui.setItem(sethomeSlot, PaperItemBuilder.from(Material.END_CRYSTAL)
                .name(MiniMessage.miniMessage().deserialize(inventory.getString("main.sethome.name", "<yellow>ꜱᴇᴛ ᴛʜᴇ ʙᴏx ʜᴏᴍᴇ")))
                .lore(sethomeLoreComponents).asGuiItem(click -> {
                    boxManager.sethome(box, player);
                    gui.close(player);
                }));

        List<String> teleportLore = inventory.getStringList("main.teleport.lore");
        List<Component> teleportLoreComponents = new ArrayList<>();

        for (String string : teleportLore) {
            teleportLoreComponents.add(MiniMessage.miniMessage().deserialize(string));
        }

        gui.setItem(teleportSlot, PaperItemBuilder.from(Material.ENDER_PEARL)
                .name(MiniMessage.miniMessage().deserialize(inventory.getString("main.teleport.name", "<gold>ᴛᴇʟᴇᴘᴏʀᴛ ᴛᴏ ᴛʜᴇ ʙᴏx")))
                .lore(teleportLoreComponents).asGuiItem(click -> {

                    SlimeWorldInstance slimeWorldInstance = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld(player.getUniqueId().toString());

                    if (slimeWorldInstance == null) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("congrulations", "<red>You did the impossible! Please contact an administrator to report this error.")));
                        return;
                    }

                    boxManager.teleport(player, box, slimeWorldInstance);
                }));

        List<String> addedLore = inventory.getStringList("main.added.lore");
        List<Component> addedLoreComponents = new ArrayList<>();

        for (String string : addedLore) {
            addedLoreComponents.add(MiniMessage.miniMessage().deserialize(string));
        }

        gui.setItem(addedSlot, PaperItemBuilder.from(Material.EMERALD)
                .name(MiniMessage.miniMessage().deserialize(inventory.getString("main.added.name", "<gold>ʏᴏᴜʀ ꜰʀɪᴇɴᴅꜱ' ʙᴏxᴇꜱ")))
                .lore(addedLoreComponents).asGuiItem(click -> {
                    boxVisitInventory.openMenu(player, box, this);
                }));

        List<String> friendsLore = inventory.getStringList("main.friends.lore");
        List<Component> friendsLoreComponents = new ArrayList<>();

        for (String string : friendsLore) {
            friendsLoreComponents.add(MiniMessage.miniMessage().deserialize(string));
        }

        gui.setItem(friendsSlot, PaperItemBuilder.from(Material.PLAYER_HEAD)
                .name(MiniMessage.miniMessage().deserialize(inventory.getString("main.friends.name", "<yellow>ᴀᴅᴅ ꜰʀɪᴇɴᴅꜱ ᴛᴏ ʏᴏᴜʀ ʙᴏx")))
                .lore(friendsLoreComponents).asGuiItem(click -> {
                    boxFriendsInventory.openMenu(player, box, this);
                }));

        List<String> renameLore = inventory.getStringList("main.rename.lore");
        List<Component> renameLoreComponents = new ArrayList<>();

        for (String string : renameLore) {
            renameLoreComponents.add(MiniMessage.miniMessage().deserialize(string));
        }

        gui.setItem(renameSlot, PaperItemBuilder.from(Material.WRITABLE_BOOK)
                .name(MiniMessage.miniMessage().deserialize(inventory.getString("main.rename.name", "<yellow>ʀᴇɴᴀᴍᴇ ʏᴏᴜʀ ʙᴏx ɴᴀᴍᴇ")))
                .lore(renameLoreComponents).asGuiItem(click -> {

                    gui.close(player);
                    ChatController.RENAME_BOX.add(player.getUniqueId());

                    for (String string : lang.getStringList("rename-box")) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize(string));
                    }

                    player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

                }));

        List<String> upgradeLore = inventory.getStringList("main.upgrade.lore");
        List<Component> upgradeLoreComponents = new ArrayList<>();

        for (String string : upgradeLore) {
            upgradeLoreComponents.add(MiniMessage.miniMessage().deserialize(string));
        }

        ItemStack upgradeStack = new ItemStack(Material.POISONOUS_POTATO);

        ItemLore lore = ItemLore.lore(upgradeLoreComponents);

        upgradeStack.setData(DataComponentTypes.CUSTOM_NAME, MiniMessage.miniMessage().deserialize(inventory.getString("main.upgrade.name", "<yellow>ᴜᴘɢʀᴀᴅᴇ ʏᴏᴜʀ ʙᴏx ꜱɪᴢᴇ")));
        upgradeStack.setData(DataComponentTypes.LORE, lore);
        upgradeStack.setData(DataComponentTypes.ITEM_MODEL, new NamespacedKey("minecraft", "netherite_upgrade_smithing_template"));

        gui.setItem(upgradeSlot, new GuiItem(upgradeStack, click -> {
            boxUpgradeInventory.openMenu(player, box, this);
        }));

        gui.open(player);

    }

}
