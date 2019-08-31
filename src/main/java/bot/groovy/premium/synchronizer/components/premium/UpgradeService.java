package bot.groovy.premium.synchronizer.components.premium;

import bot.groovy.premium.synchronizer.components.premium.models.AggregateResult;
import bot.groovy.premium.synchronizer.components.premium.models.UpgradeStatus;
import com.mongodb.client.model.*;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

@Component
public class UpgradeService {

    private static final String COLLECTION_NAME = "upgrades";

    private final MongoCollection<Document> collection;

    @Autowired
    public UpgradeService(MongoDatabase database) {
        this.collection = database.getCollection(COLLECTION_NAME);
    }

    public Flux<AggregateResult> getUpgradedCountByUser() {
        var pipeline = List.of(
            Aggregates.group("$createdBy", Accumulators.sum("count", 1))
        );

        var pub = collection.aggregate(pipeline, AggregateResult.class);

        return Flux.from(pub);
    }

    public Mono<Void> setStatusForUsers(Map<String, UpgradeStatus> statuses) {
        return Mono.just(statuses)
            .flatMapIterable(Map::entrySet)
            .map(e -> {
                var filter = eq("createdBy", e.getKey());
                var update = set("status", e.getValue().name());
                return new UpdateManyModel<Document>(filter, update);
            })
            .collectList()
            .flatMapMany(collection::bulkWrite)
            .then();
    }

}
