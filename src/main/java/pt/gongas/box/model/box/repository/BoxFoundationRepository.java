package pt.gongas.box.model.box.repository;

import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.BoxData;
import pt.gongas.box.util.Result;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface BoxFoundationRepository {

    void setup();

    boolean insertOrUpdateSync(Box box);

    CompletableFuture<Boolean> insertOrUpdate(Box box);

    CompletableFuture<Result<Box>> findOne(UUID boxUuid);

    boolean update(Map<Box, BoxData> boxes);

    int getBoxCount();

}
