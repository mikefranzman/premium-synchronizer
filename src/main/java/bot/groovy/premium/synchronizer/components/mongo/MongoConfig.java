package bot.groovy.premium.synchronizer.components.mongo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("mongo")
public class MongoConfig {

    private String connectionString;
    private String database;

}
