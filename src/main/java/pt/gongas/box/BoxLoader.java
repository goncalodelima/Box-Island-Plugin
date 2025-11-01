package pt.gongas.box;

import java.util.List;
import java.util.UUID;

public class BoxLoader {

    public static void addPlayerWorld() {
        BoxPlugin.worldCount.addAndGet(BoxPlugin.serverId, 1);
    }

    public static void removeBoxServer(UUID uuid) {
        BoxPlugin.boxServers.remove(uuid);
    }

    public static String getBoxServer(UUID boxUuid) {
        return BoxPlugin.boxServers.get(boxUuid);
    }

    public static String getServerWithLeastWorlds() {

        String targetServer = null;
        int minWorlds = Integer.MAX_VALUE;

        for (String serverId : BoxPlugin.plugin.getServers()) {

            int worldCount = BoxPlugin.worldCount.getOrDefault(serverId, 0);
            int reservedCount = 0;

            for (String server : BoxPlugin.serverReservations.values()) {

                if (serverId.equals(server)) {
                    reservedCount++;
                }

            }

            int count = worldCount + reservedCount;

            if (count < minWorlds && count < 50) {
                minWorlds = count;
                targetServer = serverId;
            }

        }

        return targetServer;
    }

}