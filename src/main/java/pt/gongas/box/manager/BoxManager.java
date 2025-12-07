package pt.gongas.box.manager;

import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
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
import java.util.concurrent.TimeUnit;

public class BoxManager {

    private final BoxPlugin plugin;

    private final BoxFoundationService boxService;

    private final Configuration lang;

    public BoxManager(BoxPlugin plugin, BoxFoundationService boxService, Configuration lang) {
        this.plugin = plugin;
        this.boxService = boxService;
        this.lang = lang;
    }

    public void teleport(Player player, Box box, World world, boolean sendMessageAndSound) {

        BoxLocation boxLocation = box.getCenterBoxLocation();
        Location location = new Location(world, boxLocation.x(), boxLocation.y(), boxLocation.z(), boxLocation.yaw(), boxLocation.pitch());
        player.teleport(location);

        if (sendMessageAndSound) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("teleport", "<green>You successfully teleported to your box.")));
            player.playSound(player, Sound.ENTITY_PLAYER_TELEPORT, 1, 1);
        }

    }

    public void rename(Box box, Player player, String newName) {

        box.setBoxName(newName);
        boxService.getPendingUpdates().merge(box, BoxData.withBoxName(newName), BoxData::merge);

        player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("rename", "<green>You have successfully changed the box name to '<white>%newName%<green>'.")).replaceText(TextReplacementConfig.builder().matchLiteral("%newName%").replacement(newName).build()));
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
    }

    public void sethome(Box box, Player player) {

        Location location = player.getLocation();
        BoxLocation boxLocation = new BoxLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        System.out.println("set centerbox here 1");
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

        BoxPlugin.plugin.getRedisExecutor().submit(() -> {

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

            Set<UUID> pendingInvites = BoxPlugin.boxUuidToInvitations.get(box.getOwnerUuid());

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

                    BoxPlugin.plugin.getRedisExecutor().submit(() -> {
                        BoxPlugin.boxUuidToInvitations.put(box.getOwnerUuid(), uuid);
                        BoxPlugin.boxEvents.publish("receivedInvite;" + uuid + ";" + player.getName());
                    });

                    BoxPlugin.plugin.getRedisExecutor().schedule(() -> {

                        Set<UUID> current = BoxPlugin.boxUuidToInvitations.get(box.getOwnerUuid());

                        current.remove(uuid);

                        if (current.isEmpty()) {
                            BoxPlugin.boxUuidToInvitations.removeAll(box.getOwnerUuid());
                        }

                    }, 60, TimeUnit.SECONDS);

                    player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("invite", "<green>You have successfully invited player <white>%target%<green>.")).replaceText(TextReplacementConfig.builder().matchLiteral("%target%").replacement(name).build()));
                    player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

                }
            }.runTask(plugin);

        });

    }

    public void addFriendsForceAsync(Player player, String target, Box box) {

        if (player.getName().equalsIgnoreCase(target)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("invite-yourself", "<red>You can't invite yourself")));
            return;
        }

        UUID playerUuid = player.getUniqueId();

        BoxPlugin.plugin.getRedisExecutor().submit(() -> {

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

            if (playerUuid.equals(uuid)) { // Is it redundant?
                player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("invite-yourself", "<red>You can't invite yourself")));
                return;
            }

            Set<UUID> pendingInvites = BoxPlugin.boxUuidToInvitations.get(box.getOwnerUuid());

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

                    BoxPlugin.plugin.getRedisExecutor().submit(() -> {
                        BoxPlugin.boxUuidToInvitations.put(box.getOwnerUuid(), uuid);
                        BoxPlugin.boxEvents.publish("receivedInvite;" + uuid + ";" + player.getName());
                    });

                    BoxPlugin.plugin.getRedisExecutor().schedule(() -> {

                        Set<UUID> current = BoxPlugin.boxUuidToInvitations.get(box.getOwnerUuid());

                        current.remove(uuid);

                        if (current.isEmpty()) {
                            BoxPlugin.boxUuidToInvitations.removeAll(box.getOwnerUuid());
                        }

                    }, 60, TimeUnit.SECONDS);

                    player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("invite", "<green>You have successfully invited player <white>%target%<green>.")).replaceText(TextReplacementConfig.builder().matchLiteral("%target%").replacement(name).build()));
                    player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

                }
            }.runTask(plugin);

        });

    }

    public void removeFriend(Box box, String target, Player player) {

        String realName = box.getPlayerNameByLowercaseName().remove(target.toLowerCase());

        if (realName == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("no-friend", "<red>The inserted player is not added to your box.")));
            return;
        }

        UUID uuid = box.getUuidByPlayerName().get(realName);

        box.removePlayer(uuid);
        attemptRemoveMember(box, uuid);

        player.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("friend-remove", "<green>You have successfully removed player '<white>%target%<green>'.")).replaceText(TextReplacementConfig.builder().matchLiteral("%target%").replacement(realName).build()));
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

        Player targetPlayer = Bukkit.getPlayer(target);

        if (targetPlayer != null) {
            targetPlayer.sendMessage(MiniMessage.miniMessage().deserialize(lang.getString("friend-removed", "<red>You have been removed from the '<white>%target%<red>' box.")).replaceText(TextReplacementConfig.builder().matchLiteral("%target%").replacement(realName).build()));
            targetPlayer.playSound(targetPlayer, Sound.ENTITY_VILLAGER_NO, 1, 1);
        } else {
            BoxPlugin.plugin.getRedisExecutor().submit(() -> BoxPlugin.boxEvents.publish("friendRemoval;" + uuid + ";" + player.getName()));
        }

    }

    public void attemptInviteMember(Box box, UUID playerUuid, String playerName) {

        boxService.addMember(box, playerUuid, playerName, box.getNextPosition()).thenAcceptAsync(success -> {

            if (!success) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> attemptInviteMember(box, playerUuid, playerName), 20 * 10);
            }

        }, Bukkit.getScheduler().getMainThreadExecutor(plugin));

    }

    public void attemptRemoveMember(Box box, UUID playerUuid) {

        boxService.removeMember(box, playerUuid).thenAcceptAsync(success -> {

            if (!success) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> attemptRemoveMember(box, playerUuid), 20 * 10);
            }

        }, Bukkit.getScheduler().getMainThreadExecutor(plugin));

    }

}
