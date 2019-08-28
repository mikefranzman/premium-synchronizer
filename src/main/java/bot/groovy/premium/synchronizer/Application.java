package bot.groovy.premium.synchronizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("bot.groovy")
@SpringBootApplication(exclude = { MongoReactiveAutoConfiguration.class })
@EnableTask
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
