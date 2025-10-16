package pt.gongas.box.model.box.repository;

import com.minecraftsolutions.database.Database;
import com.minecraftsolutions.database.executor.DatabaseExecutor;
import pt.gongas.box.BoxPlugin;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.BoxData;
import pt.gongas.box.model.box.adapter.BoxAdapter;
import pt.gongas.box.util.BoxLocation;
import pt.gongas.box.util.Result;
import pt.gongas.box.util.UUIDConverter;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class BoxRepository implements BoxFoundationRepository {

    private final BoxPlugin plugin;

    private final Database database;

    private final BoxAdapter adapter = new BoxAdapter();

    public BoxRepository(BoxPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public void setup() {
        try (DatabaseExecutor executor = database.execute()) {
            executor
                    .query("""
                                CREATE TABLE IF NOT EXISTS box (
                                    boxUuid BINARY(16) PRIMARY KEY,
                                    ownerUuid BINARY(16),
                                    boxName VARCHAR(32),
                                    ownerName VARCHAR(32),
                                    centerLocation VARCHAR(255),
                                    level INT,
                                    firstTime VARCHAR(128),
                                    lastTime VARCHAR(128)
                                )
                            """)
                    .write();
        }
    }

    @Override
    public boolean insertOrUpdateSync(Box box) {

        UUID boxUuid = box.getBoxUuid();
        UUID ownerUuid = box.getOwnerUuid();
        String boxName = box.getBoxName();
        String ownerName = box.getOwnerName();
        String centerLocation = BoxLocation.serialize(box.getCenterBoxLocation());
        int level = box.getBoxLevel().level();
        String firstTime = box.getFirstTime();
        String lastTime = box.getLastTime();

        try (DatabaseExecutor executor = database.execute()) {

            executor
                    .query("""
                                INSERT INTO box
                                (boxUuid, ownerUuid, boxName, ownerName, centerLocation, level, firstTime, lastTime)
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                                ON DUPLICATE KEY UPDATE
                                    ownerUuid = VALUES(ownerUuid),
                                    boxName = VALUES(boxName),
                                    ownerName = VALUES(ownerName),
                                    centerLocation = VALUES(centerLocation),
                                    level = VALUES(level),
                                    firstTime = VALUES(firstTime),
                                    lastTime = VALUES(lastTime)
                            """)
                    .write(statement -> {
                        statement.set(1, UUIDConverter.convert(boxUuid));
                        statement.set(2, UUIDConverter.convert(ownerUuid));
                        statement.set(3, boxName);
                        statement.set(4, ownerName);
                        statement.set(5, centerLocation);
                        statement.set(6, level);
                        statement.set(7, firstTime);
                        statement.set(8, lastTime);
                    });

            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to insert or update box data sync", e);
            return false;
        }

    }

    @Override
    public CompletableFuture<Boolean> insertOrUpdate(Box box) {

        UUID boxUuid = box.getBoxUuid();
        UUID ownerUuid = box.getOwnerUuid();
        String boxName = box.getBoxName();
        String ownerName = box.getOwnerName();
        String centerLocation = BoxLocation.serialize(box.getCenterBoxLocation());
        int level = box.getBoxLevel().level();
        String firstTime = box.getFirstTime();
        String lastTime = box.getLastTime();

        return CompletableFuture.supplyAsync(() -> {
            try (DatabaseExecutor executor = database.execute()) {

                executor
                        .query("""
                                    INSERT INTO box
                                    (boxUuid, ownerUuid, boxName, ownerName, centerLocation, level, firstTime, lastTime)
                                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                                    ON DUPLICATE KEY UPDATE
                                        ownerUuid = VALUES(ownerUuid),
                                        boxName = VALUES(boxName),
                                        ownerName = VALUES(ownerName),
                                        centerLocation = VALUES(centerLocation),
                                        level = VALUES(level),
                                        firstTime = VALUES(firstTime),
                                        lastTime = VALUES(lastTime)
                                """)
                        .write(statement -> {
                            statement.set(1, UUIDConverter.convert(boxUuid));
                            statement.set(2, UUIDConverter.convert(ownerUuid));
                            statement.set(3, boxName);
                            statement.set(4, ownerName);
                            statement.set(5, centerLocation);
                            statement.set(6, level);
                            statement.set(7, firstTime);
                            statement.set(8, lastTime);
                        });

                return true;
            }
        }, plugin.getDatabaseExecutor()).exceptionally(e -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to insert or update box data", e);
            return false;
        });
    }

    @Override
    public CompletableFuture<Result<Box>> findOne(UUID boxUuid) {

        return CompletableFuture.supplyAsync(() -> {

            try (DatabaseExecutor executor = database.execute()) {

                Box box = executor
                        .query("SELECT * FROM box WHERE boxUuid = ?")
                        .readOne(statement -> statement.set(1, UUIDConverter.convert(boxUuid)), adapter)
                        .orElse(null);

                return Result.ok(box);
            }

        }, plugin.getDatabaseExecutor()).exceptionally(e -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to fetch box from database", e);
            return Result.fail("Failed to fetch box from database: " + e.getMessage());
        });
    }

    @Override
    public boolean update(Map<Box, BoxData> boxes) {

        try (DatabaseExecutor executor = database.execute()) {
            executor.query("""
                            INSERT INTO box
                            (boxUuid, ownerUuid, boxName, ownerName, centerLocation, level, firstTime, lastTime)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            ON DUPLICATE KEY UPDATE
                                ownerUuid      = COALESCE(VALUES(ownerUuid), ownerUuid),
                                boxName        = COALESCE(VALUES(boxName), boxName),
                                ownerName      = COALESCE(VALUES(ownerName), ownerName),
                                centerLocation = VALUES(centerLocation),
                                level          = COALESCE(VALUES(level), level),
                                firstTime      = COALESCE(VALUES(firstTime), firstTime),
                                lastTime       = COALESCE(VALUES(lastTime), lastTime)
                            """)
                    .batch(boxes.entrySet(), (entry, statement) -> {
                        Box box = entry.getKey();
                        BoxData data = entry.getValue();
                        statement.set(1, UUIDConverter.convert(box.getBoxUuid()));
                        statement.set(2, UUIDConverter.convert(box.getOwnerUuid()));
                        statement.set(3, data.boxName());
                        statement.set(4, data.ownerName());
                        statement.set(5, data.centerLocation());
                        statement.set(6, data.boxLevel().level());
                        statement.set(7, data.firstTime());
                        statement.set(8, data.lastTime());
                    });

            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to batch update boxes data", e);
            return false;
        }

    }

    @Override
    public int getBoxCount() {
        return 0;
    }

}
