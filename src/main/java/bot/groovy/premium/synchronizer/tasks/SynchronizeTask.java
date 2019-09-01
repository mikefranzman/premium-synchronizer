package bot.groovy.premium.synchronizer.tasks;

import bot.groovy.chargebee.commons.EntityType;
import bot.groovy.chargebee.commons.models.Subscription;
import bot.groovy.chargebee.mirror.client.ChargebeeMirrorService;
import bot.groovy.premium.synchronizer.components.premium.UpgradeService;
import bot.groovy.premium.synchronizer.components.premium.models.AggregateResult;
import bot.groovy.premium.synchronizer.components.premium.models.UpgradeStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.stream.Collectors;

@Component
public class SynchronizeTask implements CommandLineRunner {

    @Autowired private UpgradeService upgradeService;
    @Autowired private ChargebeeMirrorService chargebeeMirrorService;

    @Override
    public void run(String... args) {
        upgradeService.getUpgradedCountByUser()
            .flatMap(result -> {
                return Mono.zip(
                    Mono.just(result),
                    getSubscription(result.getId())
                );
            })
            .map(tuple -> {
                var status = determineStatus(tuple.getT1(), tuple.getT2());
                var userId = tuple.getT1().getId();
                return Tuples.of(userId, status);
            })
            .collect(Collectors.toMap(Tuple2::getT1, Tuple2::getT2))
            .delayUntil(upgradeService::setStatusForUsers)
            .block();

        System.exit(0);
    }

    public Mono<Subscription> getSubscription(String subscriptionId) {
        return chargebeeMirrorService.getEntity(EntityType.fromSingular("subscription"), subscriptionId, Subscription.class);
    }

    public static UpgradeStatus determineStatus(AggregateResult result, Subscription subscription) {
        if(subscription == null || !subscription.isActive()) {
            return UpgradeStatus.INACTIVE;
        }

        var usedUpgrades = result.getCount();
        var allocatedUpgrades = subscription.getPlanQuantity();
        if(usedUpgrades > allocatedUpgrades) {
            return UpgradeStatus.LIMIT_EXCEEDED;
        }

        return UpgradeStatus.ACTIVE;
    }

}
