package pt.gongas.box.inventory;

import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.redisson.api.RFuture;
import pt.gongas.box.BoxLoader;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.controller.ChatController;
import pt.gongas.box.loadbalancing.redirect.RedirectManager;
import pt.gongas.box.manager.BoxManager;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.BoxInfo;
import pt.gongas.box.model.level.service.BoxLevelFoundationService;
import pt.gongas.box.model.box.service.BoxFoundationService;
import pt.gongas.box.util.config.Configuration;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class BoxVisitInventory {

    private final BoxFoundationService boxService;

    private final BoxLevelFoundationService boxLevelService;

    private final BoxPlugin plugin;

    private final BoxManager boxManager;

    private final Configuration inventory;

    private final Configuration lang;

    private final GuiItem decorationItem;

    private final int backSlot;

    private final int informationSlot;

    private final int closeSlot;

    private final List<Integer> decorationSlots;

    public BoxVisitInventory(BoxFoundationService boxService, BoxLevelFoundationService boxLevelService, BoxPlugin plugin, Configuration inventory, Configuration lang, BoxManager boxManager) {
        this.boxService = boxService;
        this.boxLevelService = boxLevelService;
        this.plugin = plugin;
        this.inventory = inventory;
        this.lang = lang;
        this.boxManager = boxManager;
        this.decorationItem = PaperItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).asGuiItem();
        this.backSlot = inventory.getInt("visit.back.slot");
        this.informationSlot = inventory.getInt("visit.information.slot");
        this.closeSlot = inventory.getInt("visit.close.slot");
        this.decorationSlots = inventory.getIntegerList("visit.decoration.slots");
    }

    public void openMenu(Player player, Box box, BoxInventory boxInventory) {

        UUID uuid = player.getUniqueId();
        String name = player.getName();
        RFuture<Collection<Object[]>> future = BoxPlugin.boxListByPlayerUuid.getAllAsync(uuid);

        future.thenAcceptAsync(boxList -> {

            Gui gui = Gui.gui()
                    .rows(inventory.getInt("visit.rows", 6))
                    .title(MiniMessage.miniMessage().deserialize(inventory.getString("visit.title", "ʙᴏx ꜰʀɪᴇɴᴅꜱ")))
                    .disableItemTake()
                    .disableItemPlace()
                    .disableItemSwap()
                    .disableItemDrop()
                    .create();

            List<String> backLoreString = inventory.getStringList("visit.back.lore");
            List<Component> backLoreComponents = new ArrayList<>();

            for (String string : backLoreString) {
                backLoreComponents.add(MiniMessage.miniMessage().deserialize(string));
            }

            gui.setItem(backSlot, PaperItemBuilder.from(Material.ARROW)
                    .name(MiniMessage.miniMessage().deserialize(inventory.getString("back.name", "<green>ɢᴏ ʙᴀᴄᴋ")))
                    .lore(backLoreComponents)
                    .asGuiItem(click -> boxInventory.openMenu(player, box)));

            List<String> informationLoreString = inventory.getStringList("visit.information.lore");
            List<Component> informationLoreComponents = new ArrayList<>();

            for (String string : informationLoreString) {
                informationLoreComponents.add(MiniMessage.miniMessage().deserialize(string));
            }

            gui.setItem(informationSlot, PaperItemBuilder.from(Material.BRUSH)
                    .name(MiniMessage.miniMessage().deserialize(inventory.getString("visit.information.name", "<gold>ʙᴏx ꜰʀɪᴇɴᴅ ɪɴꜰᴏʀᴍᴀᴛɪᴏɴ")))
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

            List<String> yoursLore = inventory.getStringList("visit.yours.lore");
            List<Component> yoursLoreComponents = new ArrayList<>();

            for (String string : yoursLore) {
                Component component = MiniMessage.miniMessage().deserialize(string)
                        .replaceText(TextReplacementConfig.builder()
                                .matchLiteral("%size%")
                                .replacement(String.valueOf(box.getBoxLevel().size()))
                                .build())
                        .replaceText(TextReplacementConfig.builder()
                                .matchLiteral("%owner%")
                                .replacement(box.getOwnerName())
                                .build())
                        .replaceText(TextReplacementConfig.builder()
                                .matchLiteral("%members%")
                                .replacement(String.valueOf(box.getPlayerNameByUuid().size() + 1))
                                .build())
                        .replaceText(TextReplacementConfig.builder()
                                .matchLiteral("%createdDate%")
                                .replacement(box.getFirstTime())
                                .build())
                        .replaceText(TextReplacementConfig.builder()
                                .matchLiteral("%lastAccess%")
                                .replacement(box.getLastTime())
                                .build());

                yoursLoreComponents.add(component);
            }

            gui.setItem(20, PaperItemBuilder.from(Material.EMERALD_BLOCK)
                    .name(MiniMessage.miniMessage().deserialize(inventory.getString("visit.yours.name", "<gold>Your box")))
                    .lore(yoursLoreComponents)
                    .asGuiItem(click -> {

                    }));

            List<String> otherLore = inventory.getStringList("visit.other.lore");

            int i = 1;

            for (Object[] object : boxList) {

                BoxInfo boxInfo = new BoxInfo((UUID) object[0], (UUID) object[1], (String) object[2], (int) object[3], (int) object[4], (String) object[5], (String) object[6]);
                List<Component> otherLoreComponents = new ArrayList<>();

                for (String string : otherLore) {
                    otherLoreComponents.add(MiniMessage.miniMessage().deserialize(string)
                            .replaceText(TextReplacementConfig.builder()
                                    .matchLiteral("%size%")
                                    .replacement(String.valueOf(boxInfo.getSize()))
                                    .build())
                            .replaceText(TextReplacementConfig.builder()
                                    .matchLiteral("%owner%")
                                    .replacement(String.valueOf(boxInfo.getBoxOwner()))
                                    .build())
                            .replaceText(TextReplacementConfig.builder()
                                    .matchLiteral("%members%")
                                    .replacement(String.valueOf(boxInfo.getMembers()))
                                    .build())
                            .replaceText(TextReplacementConfig.builder()
                                    .matchLiteral("%createdDate%")
                                    .replacement(boxInfo.getFirstTime())
                                    .build())
                            .replaceText(TextReplacementConfig.builder()
                                    .matchLiteral("%lastAccess%")
                                    .replacement(boxInfo.getLastTime())
                                    .build()));
                }

                gui.setItem(20 + i, PaperItemBuilder.from(Material.EMERALD_BLOCK)
                        .name(MiniMessage.miniMessage().deserialize(inventory.getString("visit.other.name", "<gold>%owner%'s Box"))
                                .replaceText(TextReplacementConfig.builder().matchLiteral("%owner%").replacement(boxInfo.getBoxOwner()).build())
                        )
                        .lore(otherLoreComponents)
                        .asGuiItem(click -> {

                            new BukkitRunnable() {
                                @Override
                                public void run() {

                                    String playerServer = BoxLoader.getPlayerServer(boxInfo.getBoxUuid());
                                    String server;

                                    if (playerServer == null) {

                                        server = BoxLoader.getServerWithLeastWorlds();

                                        if (server == null) {
                                            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("server-redirect-full", "<red>All servers are full. ;(")));
                                            return;
                                        }

                                    } else {
                                        server = playerServer;
                                    }

                                    byte[] redirect = RedirectManager.getRedirect(server);

                                    if (redirect == null) {
                                        player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("server-redirect-error", "<red>The server '%server%' no longer exists ;(")).replaceText(TextReplacementConfig.builder().matchLiteral("%server%").replacement(server).build()));
                                        return;
                                    }

                                    if (BoxPlugin.serverReservations.containsKey(box.getBoxUuid()) || BoxPlugin.boxUuidByPlayerUuid.containsKey(uuid)) {
                                        player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("server-redirect-wait", "<red>Wait...")));
                                        return;
                                    }

                                    BoxPlugin.serverReservations.put(box.getBoxUuid(), server, 10, TimeUnit.SECONDS);

                                    if (server.equals(BoxPlugin.serverId)) {

                                        player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("box-visit", "<green>Visiting %owner%'s box...")).replaceText(TextReplacementConfig.builder().matchLiteral("%owner%").replacement(boxInfo.getBoxOwner()).build()));
                                        BoxLoader.addPlayerWorld(uuid);
                                        boxService.createBox(player, uuid, name, boxInfo.getBoxUuid(), boxInfo.getBoxOwnerUuid(), false);

                                    } else {

                                        BoxPlugin.boxUuidByPlayerUuid.put(uuid, box.getBoxUuid(), 10, TimeUnit.SECONDS);
                                        BoxPlugin.ownerToBox.put(box.getOwnerUuid(), box.getBoxUuid());
                                        BoxPlugin.boxToOwner.put(box.getBoxUuid(), box.getOwnerUuid());

                                        player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("server-redirect", "<green>Redirecting to '%server%'...")).replaceText(TextReplacementConfig.builder().matchLiteral("%server%").replacement(server).build()));
                                        Bukkit.getScheduler().runTask(plugin, () -> player.sendPluginMessage(plugin, "BungeeCord", redirect));

                                    }


                                }
                            }.runTaskAsynchronously(plugin);

                        }));

                i++;
            }

            gui.open(player);

        }, plugin.getBukkitMainThreadExecutor());

    }

    private GuiAction<InventoryClickEvent> handleClick(Player player, UUID uuid, Box box, Gui gui, int pos) {
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
                ChatController.ADD_FRIENDS.add(uuid);

                for (String string : lang.getStringList("add-friend")) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(string));
                }

                player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

            }

        };
    }

}
