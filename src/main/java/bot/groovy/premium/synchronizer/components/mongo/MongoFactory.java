package bot.groovy.premium.synchronizer.components.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.connection.netty.NettyStreamFactoryFactory;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class MongoFactory {

    @Bean
    public MongoClient getMongo(MongoConfig config) {
        var connectionString = new ConnectionString(config.getConnectionString());

        var streamFactoryFactory = NettyStreamFactoryFactory.builder()
            .eventLoopGroup(new NioEventLoopGroup())
            .build();

        var settings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .streamFactoryFactory(streamFactoryFactory)
            .build();

        return MongoClients.create(settings);
    }

    @Bean
    public MongoDatabase getMongoDatabase(MongoClient mongo, MongoConfig config) {
        return mongo.getDatabase(config.getDatabase());
    }

}
