package pt.gongas.box;

import java.util.UUID;

public class BoxLoader {

    public static boolean isPlayerWorldLoaded(UUID playerUuid) {
        return BoxPlugin.playerServer.containsKey(playerUuid);
    }

    public static void addPlayerWorld(UUID playerUuid) {
        BoxPlugin.playerServer.put(playerUuid, BoxPlugin.serverId);
        BoxPlugin.worldCount.addAndGet(BoxPlugin.serverId, 1);
    }

    public static void removePlayerWorld(UUID playerUuid) {
        BoxPlugin.playerServer.remove(playerUuid);
    }

    public static String getPlayerServer(UUID playerUuid) {
        return BoxPlugin.playerServer.get(playerUuid);
    }

    public static String getBoxServer(UUID boxUuid) {
        return BoxPlugin.boxServers.get(boxUuid);
    }

    public static String getServerWithLeastWorlds() {

        String targetServer = null;
        int minWorlds = Integer.MAX_VALUE;

        for (String serverId : BoxPlugin.plugin.getServers()) {

            int count =  BoxPlugin.worldCount.getOrDefault(serverId, 0);

            if (count < minWorlds && count < 50) {
                minWorlds = count;
                targetServer = serverId;
            }

        }

        return targetServer;
    }

}