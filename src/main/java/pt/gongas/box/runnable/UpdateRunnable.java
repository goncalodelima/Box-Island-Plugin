package pt.gongas.box.runnable;

import org.bukkit.scheduler.BukkitRunnable;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.BoxData;
import pt.gongas.box.model.box.service.BoxFoundationService;

import java.util.HashMap;
import java.util.Map;

public class UpdateRunnable extends BukkitRunnable {

    private final BoxPlugin plugin;

    private final BoxFoundationService boxService;

    public UpdateRunnable(BoxPlugin plugin, BoxFoundationService boxService) {
        this.plugin = plugin;
        this.boxService = boxService;
    }

    @Override
    public void run() {

        Map<Box, BoxData> boxes = new HashMap<>(boxService.getPendingUpdates());

        if (!boxes.isEmpty()) {

            boolean success = boxService.update(boxes);

            if (success) {
                boxService.getPendingUpdates().keySet().removeAll(boxes.keySet());
            }

        }

    }

}
