package bot.groovy.premium.synchronizer.components.premium;

import bot.groovy.premium.synchronizer.components.premium.models.AggregateResult;
import bot.groovy.premium.synchronizer.components.premium.models.UpgradeStatus;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class UpgradeService {

    private static final String COLLECTION_NAME = "upgrades";

    private final MongoCollection<Document> collection;

    @Autowired
    public UpgradeService(MongoDatabase database) {
        this.collection = database.getCollection(COLLECTION_NAME);
    }

    public Flux<AggregateResult> getUpgradedCountByUser() {
        // The driver breaks if you try and use the automatic pojo creator and
        // the aggregate stuff. It tries to encode the list as a pojo and just ends up
        // sending { "empty": false }

        var pipeline = List.of(
            new Document()
                .append("$group", new Document()
                    .append("_id", "$createdBy")
                    .append("count", new Document()
                        .append("$sum", 1)
                    )
                )
        );

        var pub = collection.aggregate(pipeline);

        return Flux.from(pub)
            .map(document -> {
                var result = new AggregateResult();
                result.setId(document.getString("_id"));
                result.setCount(result.getCount());
                return result;
            });
    }

    public Mono<Void> setStatusForUsers(Map<String, UpgradeStatus> statuses) {
        return Mono.just(statuses)
            .flatMapIterable(Map::entrySet)
            .map(e -> {
                Map<String, Object> filter = Map.of(
                    "createdBy", e.getKey()
                );
                Map<String, Object> update = Map.of(
                    "$set", Map.of(
                        "status", e.getValue().name()
                   )
               );
                return new UpdateManyModel<Document>(new Document(filter), new Document(update));
            })
            .collectList()
            .flatMapMany(collection::bulkWrite)
            .then();
    }

}
