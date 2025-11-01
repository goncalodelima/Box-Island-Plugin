package pt.gongas.box.inventory;

import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.BoxData;
import pt.gongas.box.model.box.service.BoxFoundationService;
import pt.gongas.box.model.level.BoxLevel;
import pt.gongas.box.model.level.loader.BoxLevelLoader;
import pt.gongas.box.model.level.service.BoxLevelFoundationService;
import pt.gongas.box.util.config.Configuration;

import java.util.*;

public class BoxUpgradeInventory {

    private final BoxFoundationService boxService;

    private final BoxLevelFoundationService boxLevelService;

    private final Configuration inventory;

    private final Map<Box, Set<Player>> viewers = new HashMap<>();

    private final GuiItem decorationItem;

    private final int backSlot;

    private final int informationSlot;

    private final int closeSlot;

    private final List<Integer> decorationSlots;

    public BoxUpgradeInventory(BoxFoundationService boxService, BoxLevelFoundationService boxLevelService, Configuration inventory) {
        this.boxService = boxService;
        this.boxLevelService = boxLevelService;
        this.inventory = inventory;
        this.decorationItem = PaperItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).asGuiItem();
        this.backSlot = inventory.getInt("upgrade.back.slot");
        this.informationSlot = inventory.getInt("upgrade.information.slot");
        this.closeSlot = inventory.getInt("upgrade.close.slot");
        this.decorationSlots = inventory.getIntegerList("upgrade.decoration.slots");
    }

    public void openMenu(Player player, Box box, BoxInventory boxInventory) {

        double balance = BoxPlugin.economy.getBalance(player);

        BoxLevel boxLevel = box.getBoxLevel();
        int level = boxLevel.level();

        Gui gui = Gui.gui()
                .rows(inventory.getInt("upgrade.rows", 6))
                .title(MiniMessage.miniMessage().deserialize(inventory.getString("upgrade.title", "ʙᴏx ᴜᴘɢʀᴀᴅᴇꜱ")))
                .disableItemTake()
                .disableItemPlace()
                .disableItemSwap()
                .disableItemDrop()
                .create();

        List<String> backLoreString = inventory.getStringList("upgrade.back.lore");
        List<Component> backLoreComponents = new ArrayList<>();

        for (String string : backLoreString) {
            backLoreComponents.add(MiniMessage.miniMessage().deserialize(string));
        }

        gui.setItem(backSlot, PaperItemBuilder.from(Material.ARROW)
                .name(MiniMessage.miniMessage().deserialize(inventory.getString("back.name", "<green>ɢᴏ ʙᴀᴄᴋ")))
                .lore(backLoreComponents)
                .asGuiItem(click -> boxInventory.openMenu(player, box)));

        List<String> informationLoreString = inventory.getStringList("upgrade.information.lore");
        List<Component> informationLoreComponents = new ArrayList<>();

        for (String string : informationLoreString) {
            informationLoreComponents.add(MiniMessage.miniMessage().deserialize(string));
        }

        gui.setItem(informationSlot, PaperItemBuilder.from(Material.BRUSH)
                .name(MiniMessage.miniMessage().deserialize(inventory.getString("upgrade.information.name", "<gold>ʙᴏx ᴜᴘɢʀᴀᴅᴇ ɪɴꜰᴏʀᴍᴀᴛɪᴏɴ")))
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

        int slot = 10;

        for (int i = 1; i <= BoxLevelLoader.MAX_LEVEL; i++) {

            int nextSlot = slot;

            if (slot == 16 || slot == 25 || slot == 34) {

                if (i == BoxLevelLoader.MAX_LEVEL) {
                    nextSlot = -1;
                } else {
                    nextSlot += 3;
                }

            } else if (slot == 17 || slot == 26 || slot == 35) {

                slot += 2;

                if (i == BoxLevelLoader.MAX_LEVEL) {
                    nextSlot = -1;
                } else {
                    nextSlot += 3;
                }

            } else {

                if (i == BoxLevelLoader.MAX_LEVEL) {
                    nextSlot = -1;
                } else {
                    nextSlot++;
                }

            }

            double money;
            int size;

            if (level != i) {
                boxLevel = boxLevelService.get(i);
            }

            money = boxLevel.money();
            size = boxLevel.size();

            TextReplacementConfig levelConfig = TextReplacementConfig.builder().matchLiteral("%level%").replacement(String.valueOf(i)).build();
            TextReplacementConfig moneyConfig = TextReplacementConfig.builder().matchLiteral("%money%").replacement(BoxPlugin.formatter.formatNumber(money)).build();
            TextReplacementConfig balanceConfig = TextReplacementConfig.builder().matchLiteral("%player_money%").replacement(BoxPlugin.formatter.formatNumber(balance)).build();
            TextReplacementConfig sizeConfig = TextReplacementConfig.builder().matchLiteral("%size%").replacement(String.valueOf(size)).build();

            if (level == i - 1) { // verify if can upgrade to i level

                if (balance < money) {

                    List<String> loreString = inventory.getStringList("upgrade.currentStep.notEvolvable.lore");
                    List<Component> lore = new ArrayList<>();

                    for (String string : loreString) {
                        lore.add(MiniMessage.miniMessage().deserialize(string)
                                .replaceText(levelConfig)
                                .replaceText(moneyConfig)
                                .replaceText(balanceConfig)
                                .replaceText(sizeConfig));
                    }

                    gui.setItem(slot, PaperItemBuilder.from(Material.RED_STAINED_GLASS_PANE)
                            .name(MiniMessage.miniMessage().deserialize(inventory.getString("upgrade.currentStep.notEvolvable.name", "<red><bold>ʟᴇᴠᴇʟ %level% <dark_gray>(<red>ʟᴏᴄᴋᴇᴅ<dark_gray>)")).replaceText(levelConfig))
                            .lore(lore)
                            .asGuiItem(handleClick(player, box, i, slot, nextSlot)));

                } else {

                    List<String> loreString = inventory.getStringList("upgrade.currentStep.evolvable.lore");
                    List<Component> lore = new ArrayList<>();

                    for (String string : loreString) {
                        lore.add(MiniMessage.miniMessage().deserialize(string)
                                .replaceText(levelConfig)
                                .replaceText(moneyConfig)
                                .replaceText(balanceConfig)
                                .replaceText(sizeConfig));
                    }

                    gui.setItem(slot, PaperItemBuilder.from(Material.YELLOW_STAINED_GLASS_PANE)
                            .name(MiniMessage.miniMessage().deserialize(inventory.getString("upgrade.currentStep.evolvable.name", "<green><bold>ʟᴇᴠᴇʟ %level% <dark_gray>(<green>ᴀᴠᴀɪʟᴀʙʟᴇ<dark_gray>)")).replaceText(levelConfig))
                            .lore(lore)
                            .asGuiItem(handleClick(player, box, i, slot, nextSlot)));

                }

            } else if (level < i) {

                List<String> loreString = inventory.getStringList("upgrade.previousStep.lore");
                List<Component> lore = new ArrayList<>();

                for (String string : loreString) {
                    lore.add(MiniMessage.miniMessage().deserialize(string)
                            .replaceText(levelConfig)
                            .replaceText(moneyConfig)
                            .replaceText(balanceConfig)
                            .replaceText(sizeConfig));
                }

                gui.setItem(slot, PaperItemBuilder.from(Material.RED_STAINED_GLASS_PANE)
                        .name(MiniMessage.miniMessage().deserialize(inventory.getString("upgrade.previousStep.name", "<green><bold>ʟᴇᴠᴇʟ %level% <dark_gray>(<green>ᴀᴠᴀɪʟᴀʙʟᴇ<dark_gray>)")).replaceText(levelConfig))
                        .lore(lore)
                        .asGuiItem(handleClick(player, box, i, slot, nextSlot)));

            } else {

                List<String> loreString = inventory.getStringList("upgrade.claimed.lore");
                List<Component> lore = new ArrayList<>();

                for (String string : loreString) {
                    lore.add(MiniMessage.miniMessage().deserialize(string)
                            .replaceText(levelConfig)
                            .replaceText(moneyConfig)
                            .replaceText(balanceConfig)
                            .replaceText(sizeConfig));
                }

                gui.setItem(slot, PaperItemBuilder.from(Material.GREEN_STAINED_GLASS_PANE)
                        .name(MiniMessage.miniMessage().deserialize(inventory.getString("upgrade.claimed.name", "<green><bold>ʟᴇᴠᴇʟ %level% <dark_gray>(<green>ᴄʟᴀɪᴍᴇᴅ<dark_gray>)")).replaceText(levelConfig))
                        .lore(lore)
                        .asGuiItem(handleClick(player, box, i, slot, nextSlot)));

            }

            slot++;
        }

        gui.setCloseGuiAction(close -> {

            Set<Player> viewers = this.viewers.get(box);

            if (viewers != null && viewers.remove(player) && viewers.isEmpty()) {
                this.viewers.remove(box);
            }

        });

        gui.open(player);
        this.viewers.computeIfAbsent(box, k -> new HashSet<>()).add(player);

    }

    private GuiAction<InventoryClickEvent> handleClick(Player player, Box box, int i, int clickedSlot, int nextSlot) {
        return click -> {

            BoxLevel boxLevel = box.getBoxLevel();
            int level = boxLevel.level();

            if (level == BoxLevelLoader.MAX_LEVEL) { // box already at max level
                player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1, 1);
                return;
            }

            if (level != i - 1) { // verify if can upgrade to i level
                player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1, 1);
                return;
            }

            BoxLevel newBoxLevel = boxLevelService.get(i);
            double money = newBoxLevel.money();

            double balance = BoxPlugin.economy.getBalance(player);

            if (balance < money) {
                player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1, 1);
                return;
            }

            BoxPlugin.economy.withdrawPlayer(player, money);

            box.setBoxLevel(newBoxLevel);
            boxService.getPendingUpdates().merge(box, BoxData.withLevel(i), BoxData::merge);

            SlimeWorldInstance slimeWorldInstance = BoxPlugin.advancedSlimePaperAPI.getLoadedWorld(player.getUniqueId().toString());

            if (slimeWorldInstance != null) { // maybe impossible to be null ?
                WorldBorder border = slimeWorldInstance.getBukkitWorld().getWorldBorder();
                border.setSize(newBoxLevel.size());
            }

            Set<Player> playersToUpdateInventory = viewers.get(box);

            if (playersToUpdateInventory != null) {

                double level1 = newBoxLevel.level();

                for (Player playerToUpdate : playersToUpdateInventory) {

                    if (playerToUpdate.getOpenInventory().getTopInventory().getHolder() instanceof BaseGui baseGui) {

                        double balance1 = BoxPlugin.economy.getBalance(playerToUpdate);

                        int slot = 10;

                        for (int j = 1; j <= BoxLevelLoader.MAX_LEVEL; j++) {

                            if (slot == 17 || slot == 26 || slot == 35) {
                                slot += 2;
                            }

                            double money1;
                            int size1;

                            BoxLevel newBoxLevel1;

                            if (level1 != j) {
                                newBoxLevel1 = boxLevelService.get(j);
                            } else {
                                newBoxLevel1 = newBoxLevel;
                            }

                            money1 = newBoxLevel1.money();
                            size1 = newBoxLevel1.size();

                            TextReplacementConfig levelConfig = TextReplacementConfig.builder().matchLiteral("%level%").replacement(String.valueOf(j)).build();
                            TextReplacementConfig moneyConfig = TextReplacementConfig.builder().matchLiteral("%money%").replacement(BoxPlugin.formatter.formatNumber(money1)).build();
                            TextReplacementConfig balanceConfig = TextReplacementConfig.builder().matchLiteral("%player_money%").replacement(BoxPlugin.formatter.formatNumber(balance1)).build();
                            TextReplacementConfig sizeConfig = TextReplacementConfig.builder().matchLiteral("%size%").replacement(String.valueOf(size1)).build();

                            if (level1 == j - 1) { // verify if can upgrade to j level

                                if (balance1 < money1) {

                                    List<String> loreString = inventory.getStringList("upgrade.currentStep.notEvolvable.lore");
                                    List<Component> lore = new ArrayList<>();

                                    for (String string : loreString) {
                                        lore.add(MiniMessage.miniMessage().deserialize(string)
                                                .replaceText(levelConfig)
                                                .replaceText(moneyConfig)
                                                .replaceText(balanceConfig)
                                                .replaceText(sizeConfig));
                                    }

                                    baseGui.updateItem(slot, PaperItemBuilder.from(Material.RED_STAINED_GLASS_PANE)
                                            .name(MiniMessage.miniMessage().deserialize(inventory.getString("upgrade.currentStep.notEvolvable.name", "<red><bold>ʟᴇᴠᴇʟ %level% <dark_gray>(<red>ʟᴏᴄᴋᴇᴅ<dark_gray>)")).replaceText(levelConfig))
                                            .lore(lore)
                                            .build());

                                } else {

                                    List<String> loreString = inventory.getStringList("upgrade.currentStep.evolvable.lore");
                                    List<Component> lore = new ArrayList<>();

                                    for (String string : loreString) {
                                        lore.add(MiniMessage.miniMessage().deserialize(string)
                                                .replaceText(levelConfig)
                                                .replaceText(moneyConfig)
                                                .replaceText(balanceConfig)
                                                .replaceText(sizeConfig));
                                    }

                                    baseGui.updateItem(slot, PaperItemBuilder.from(Material.YELLOW_STAINED_GLASS_PANE)
                                            .name(MiniMessage.miniMessage().deserialize(inventory.getString("upgrade.currentStep.evolvable.name", "<green><bold>ʟᴇᴠᴇʟ %level% <dark_gray>(<green>ᴀᴠᴀɪʟᴀʙʟᴇ<dark_gray>)")).replaceText(levelConfig))
                                            .lore(lore)
                                            .build());

                                }

                            } else if (level1 < j) {

                                List<String> loreString = inventory.getStringList("upgrade.previousStep.lore");
                                List<Component> lore = new ArrayList<>();

                                for (String string : loreString) {
                                    lore.add(MiniMessage.miniMessage().deserialize(string)
                                            .replaceText(levelConfig)
                                            .replaceText(moneyConfig)
                                            .replaceText(balanceConfig)
                                            .replaceText(sizeConfig));
                                }

                                baseGui.updateItem(slot, PaperItemBuilder.from(Material.RED_STAINED_GLASS_PANE)
                                        .name(MiniMessage.miniMessage().deserialize(inventory.getString("upgrade.previousStep.name", "<green><bold>ʟᴇᴠᴇʟ %level% <dark_gray>(<green>ᴀᴠᴀɪʟᴀʙʟᴇ<dark_gray>)")).replaceText(levelConfig))
                                        .lore(lore)
                                        .build());

                            } else {

                                List<String> loreString = inventory.getStringList("upgrade.claimed.lore");
                                List<Component> lore = new ArrayList<>();

                                for (String string : loreString) {
                                    lore.add(MiniMessage.miniMessage().deserialize(string)
                                            .replaceText(levelConfig)
                                            .replaceText(moneyConfig)
                                            .replaceText(balanceConfig)
                                            .replaceText(sizeConfig));
                                }

                                baseGui.updateItem(slot, PaperItemBuilder.from(Material.GREEN_STAINED_GLASS_PANE)
                                        .name(MiniMessage.miniMessage().deserialize(inventory.getString("upgrade.claimed.name", "<green><bold>ʟᴇᴠᴇʟ %level% <dark_gray>(<green>ᴄʟᴀɪᴍᴇᴅ<dark_gray>)")).replaceText(levelConfig))
                                        .lore(lore)
                                        .build());

                            }

                            slot++;
                        }

                    }

                }

            }

            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        };
    }

}
