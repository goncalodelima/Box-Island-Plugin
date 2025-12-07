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

import java.util.ArrayList;
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
                                    ownerUuid BINARY(16) PRIMARY KEY,
                                    boxName VARCHAR(32),
                                    ownerName VARCHAR(32),
                                    centerLocation VARCHAR(255),
                                    level INT,
                                    firstTime VARCHAR(128),
                                    lastTime VARCHAR(128)
                                )
                            """)
                    .write();

            executor.query("""
                    CREATE TABLE IF NOT EXISTS box_members (
                        ownerUuid BINARY(16),
                        memberUuid BINARY(16),
                        memberName VARCHAR(32),
                        position INT,
                        PRIMARY KEY (ownerUuid, memberUuid),
                        FOREIGN KEY (ownerUuid) REFERENCES box(ownerUuid) ON DELETE CASCADE
                    )
                    """).write();

        }
    }

    @Override
    public boolean insertOrUpdateSync(Box box) {

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
                                (ownerUuid, boxName, ownerName, centerLocation, level, firstTime, lastTime)
                                VALUES (?, ?, ?, ?, ?, ?, ?)
                                ON DUPLICATE KEY UPDATE
                                    boxName = VALUES(boxName),
                                    ownerName = VALUES(ownerName),
                                    centerLocation = VALUES(centerLocation),
                                    level = VALUES(level),
                                    firstTime = VALUES(firstTime),
                                    lastTime = VALUES(lastTime)
                            """)
                    .write(statement -> {
                        statement.set(1, UUIDConverter.convert(ownerUuid));
                        statement.set(2, boxName);
                        statement.set(3, ownerName);
                        statement.set(4, centerLocation);
                        statement.set(5, level);
                        statement.set(6, firstTime);
                        statement.set(7, lastTime);
                    });

            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to insert or update box data sync", e);
            return false;
        }

    }

    @Override
    public CompletableFuture<Boolean> insertOrUpdate(Box box) {

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
                                    (ownerUuid, boxName, ownerName, centerLocation, level, firstTime, lastTime)
                                    VALUES (?, ?, ?, ?, ?, ?, ?)
                                    ON DUPLICATE KEY UPDATE
                                        boxName = VALUES(boxName),
                                        ownerName = VALUES(ownerName),
                                        centerLocation = VALUES(centerLocation),
                                        level = VALUES(level),
                                        firstTime = VALUES(firstTime),
                                        lastTime = VALUES(lastTime)
                                """)
                        .write(statement -> {
                            statement.set(1, UUIDConverter.convert(ownerUuid));
                            statement.set(2, boxName);
                            statement.set(3, ownerName);
                            statement.set(4, centerLocation);
                            statement.set(5, level);
                            statement.set(6, firstTime);
                            statement.set(7, lastTime);
                        });

                return true;
            }
        }, plugin.getDatabaseExecutor()).exceptionally(e -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to insert or update box data", e);
            return false;
        });
    }

    @Override
    public CompletableFuture<Result<Box>> findOne(UUID ownerUuid) {

        return CompletableFuture.supplyAsync(() -> {

            try (DatabaseExecutor executor = database.execute()) {

                byte[] bytes = UUIDConverter.convert(ownerUuid);

                Box box = executor
                        .query("SELECT * FROM box WHERE ownerUuid = ?")
                        .readOne(statement -> statement.set(1, bytes), adapter)
                        .orElse(null);

                if (box != null) {

                    executor
                            .query("SELECT * FROM box_members WHERE ownerUuid = ?")
                            .readMany(statement -> statement.set(1, bytes), query -> {

                                UUID memberUuid = UUIDConverter.convert((byte[]) query.get("memberUuid"));
                                String memberName = (String) query.get("memberName");
                                int position = (int) query.get("position");

                                box.addPlayer(memberUuid, memberName, position);
                                return null;
                            }, ArrayList::new);

                }

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
                            (ownerUuid, boxName, ownerName, centerLocation, level, firstTime, lastTime)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            ON DUPLICATE KEY UPDATE
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
                        statement.set(1, UUIDConverter.convert(box.getOwnerUuid()));
                        statement.set(2, data.boxName());
                        statement.set(3, data.ownerName());
                        statement.set(4, data.centerLocation());
                        statement.set(5, data.level());
                        statement.set(6, data.firstTime());
                        statement.set(7, data.lastTime());
                    });

            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to batch update boxes data", e);
            return false;
        }

    }

    @Override
    public CompletableFuture<Boolean> addMember(Box box, UUID memberUuid, String memberName, int position) {
        return CompletableFuture.supplyAsync(() -> {
            try (DatabaseExecutor executor = database.execute()) {
                executor.query("""
                        INSERT INTO box_members (ownerUuid, memberUuid, memberName, position)
                        VALUES (?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            memberName = VALUES(memberName),
                            position = VALUES(position)
                        """).write(statement -> {
                    statement.set(1, UUIDConverter.convert(box.getOwnerUuid()));
                    statement.set(2, UUIDConverter.convert(memberUuid));
                    statement.set(3, memberName);
                    statement.set(4, position);
                });

                return true;
            }
        }, plugin.getDatabaseExecutor()).exceptionally(e -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to add box member to database", e);
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> removeMember(Box box, UUID memberUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (DatabaseExecutor executor = database.execute()) {
                executor.query("DELETE FROM box_members WHERE ownerUuid = ? AND memberUuid = ?")
                        .write(statement -> {
                            statement.set(1, UUIDConverter.convert(box.getOwnerUuid()));
                            statement.set(2, UUIDConverter.convert(memberUuid));
                        });

                return true;
            }
        }, plugin.getDatabaseExecutor()).exceptionally(e -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to remove box member from database", e);
            return false;
        });
    }


    @Override
    public int getBoxCount() {
        return 0;
    }

}
