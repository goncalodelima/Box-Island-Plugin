package pt.gongas.box.manager;

import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.BoxData;
import pt.gongas.box.model.box.service.BoxFoundationService;
import pt.gongas.box.util.BoxLocation;
import pt.gongas.box.util.config.Configuration;

import java.util.Set;
import java.util.UUID;

public class BoxManager {

    private final BoxPlugin plugin;

    private final BoxFoundationService boxService;

    private final Configuration lang;

    public BoxManager(BoxPlugin plugin, BoxFoundationService boxService, Configuration lang) {
        this.plugin = plugin;
        this.boxService = boxService;
        this.lang = lang;
    }

    public void teleport(Player player, Box box, SlimeWorldInstance slimeWorldInstance) {
        BoxLocation boxLocation = box.getCenterBoxLocation();
        Location location = new Location(slimeWorldInstance.getBukkitWorld(), boxLocation.x(), boxLocation.y(), boxLocation.z());
        player.teleport(location);
        player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("teleport", "<green>You successfully teleported to your box.")));
        player.playSound(player, Sound.ENTITY_PLAYER_TELEPORT, 1, 1);
    }

    public void rename(Box box, Player player, String newName) {

        box.setBoxName(newName);
        boxService.getPendingUpdates().merge(box, BoxData.withBoxName(newName), BoxData::merge);

        player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("rename", "<green>You have successfully changed the box name to '<white>%newName%<green>'.")).replaceText(TextReplacementConfig.builder().matchLiteral("%newName%").replacement(newName).build()));
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
    }

    public void sethome(Box box, Player player) {

        Location location = player.getLocation();
        BoxLocation boxLocation = new BoxLocation(location.getX(), location.getY(), location.getZ());

        box.setCenterBoxLocation(boxLocation);
        boxService.getPendingUpdates().merge(box, BoxData.withCenterLocation(boxLocation.serialize()), BoxData::merge);

        player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("sethome", "<green>You have successfully changed your box's home location.")));
        player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
    }

    public void addFriends(Player player, String target, Box box) {

        if (player.getName().equalsIgnoreCase(target)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("invite-yourself", "<red>You can't invite yourself")));
            return;
        }

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

        UUID uuid = UUID.fromString(uuidString);

        if (player.getUniqueId().equals(uuid)) { // Is it redundant?
            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("invite-yourself", "<red>You can't invite yourself")));
            return;
        }

        Set<UUID> pendingInvites = BoxPlugin.boxUuidToInvitations.get(box.getBoxUuid());

        if (pendingInvites.contains(uuid)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("already-invited", "<red>You've already invited this player! Wait for him to accept the invitation.")));
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {

                if (box.getPlayerNameByUuid().containsKey(uuid)) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("already-friend", "<red>The inserted player is already added to your box.")));
                    return;
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        BoxPlugin.boxUuidToInvitations.put(box.getBoxUuid(), uuid);
                        BoxPlugin.boxEvents.publish("receivedInvite;" + uuid + ";" + player.getName());
                    }
                }.runTaskAsynchronously(plugin);

                new BukkitRunnable() {
                    @Override
                    public void run() {

                        Set<UUID> current = BoxPlugin.boxUuidToInvitations.get(box.getBoxUuid());

                        current.remove(uuid);

                        if (current.isEmpty()) {
                            BoxPlugin.boxUuidToInvitations.removeAll(box.getBoxUuid());
                        }

                    }
                }.runTaskLaterAsynchronously(plugin, 20 * 60);

                player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("invite", "<green>You have successfully invited player <white>%target%<green>.")).replaceText(TextReplacementConfig.builder().matchLiteral("%target%").replacement(name).build()));
                player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

            }
        }.runTask(plugin);

    }

    public void addFriendsForceAsync(Player player, String target, Box box) {

        if (player.getName().equalsIgnoreCase(target)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("invite-yourself", "<red>You can't invite yourself")));
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {

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

                UUID uuid = UUID.fromString(uuidString);

                if (player.getUniqueId().equals(uuid)) { // Is it redundant?
                    player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("invite-yourself", "<red>You can't invite yourself")));
                    return;
                }

                Set<UUID> pendingInvites = BoxPlugin.boxUuidToInvitations.get(box.getBoxUuid());

                if (pendingInvites.contains(uuid)) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("already-invited", "<red>You've already invited this player! Wait for him to accept the invitation.")));
                    return;
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {

                        if (box.getPlayerNameByUuid().containsKey(uuid)) {
                            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("already-friend", "<red>The inserted player is already added to your box.")));
                            return;
                        }

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                BoxPlugin.boxUuidToInvitations.put(box.getBoxUuid(), uuid);
                                BoxPlugin.boxEvents.publish("receivedInvite;" + uuid + ";" + player.getName());
                            }
                        }.runTaskAsynchronously(plugin);

                        new BukkitRunnable() {
                            @Override
                            public void run() {

                                Set<UUID> current = BoxPlugin.boxUuidToInvitations.get(box.getBoxUuid());

                                current.remove(uuid);

                                if (current.isEmpty()) {
                                    BoxPlugin.boxUuidToInvitations.removeAll(box.getBoxUuid());
                                }

                            }
                        }.runTaskLaterAsynchronously(plugin, 20 * 60);

                        player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("invite", "<green>You have successfully invited player <white>%target%<green>.")).replaceText(TextReplacementConfig.builder().matchLiteral("%target%").replacement(name).build()));
                        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

                    }
                }.runTask(plugin);

            }
        }.runTaskAsynchronously(plugin);

    }

    public void removeFriend(Box box, String target, Player player) {

        String realName = box.getPlayerNameByLowercaseName().remove(target.toLowerCase());

        if (realName == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("no-friend", "<red>The inserted player is not added to your box.")));
            return;
        }

        UUID uuid = box.removePlayer(realName);
        // add pending updates

        player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("friend-remove", "<green>You have successfully removed player '<white>%target%<green>'.")).replaceText(TextReplacementConfig.builder().matchLiteral("%target%").replacement(realName).build()));
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

        Player targetPlayer = Bukkit.getPlayer(target);

        if (targetPlayer != null) {

            targetPlayer.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("friend-removed", "<red>You have been removed from the '<white>%target%<red>' box.")).replaceText(TextReplacementConfig.builder().matchLiteral("%target%").replacement(realName).build()));
            targetPlayer.playSound(targetPlayer, Sound.ENTITY_VILLAGER_NO, 1, 1);

        } else {
           Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> BoxPlugin.boxEvents.publish("friendRemoval;" + uuid + ";" + player.getName()));
        }

    }

}
