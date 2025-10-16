package pt.gongas.box.runnable;

import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.BoxData;
import pt.gongas.box.model.box.service.BoxFoundationService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UpdateRunnable {

    private final BoxFoundationService boxService;

    private final ScheduledExecutorService executor;

    public UpdateRunnable(BoxFoundationService boxService, ScheduledExecutorService executor) {
        this.boxService = boxService;
        this.executor = executor;
    }

    public void start() {
        executor.scheduleAtFixedRate(this::run, 36, 36, TimeUnit.SECONDS);
    }

    private void run() {

        Map<Box, BoxData> boxes = new HashMap<>(boxService.getPendingUpdates());

        if (!boxes.isEmpty()) {

            boolean success = boxService.update(boxes);

            if (success) {
                boxService.getPendingUpdates().keySet().removeAll(boxes.keySet());
            }

        }

    }

}
