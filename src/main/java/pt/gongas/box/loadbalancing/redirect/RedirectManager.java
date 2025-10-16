package pt.gongas.box.loadbalancing.redirect;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedirectManager {

    private static final Map<String, byte[]> redirects = new HashMap<>();

    private final List<String> servers;

    public RedirectManager(List<String> servers) {
        this.servers = servers;
    }

    public void setup() {

        for (String server : servers) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(server.toLowerCase());
            redirects.put(server.toLowerCase(), out.toByteArray());
        }

    }

    public static byte[] getRedirect(String server) {
        return redirects.get(server.toLowerCase());
    }

}