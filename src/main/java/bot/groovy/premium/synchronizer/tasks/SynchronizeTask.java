package bot.groovy.premium.synchronizer.tasks;

import bot.groovy.chargebee.commons.EntityType;
import bot.groovy.chargebee.commons.models.Plan;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SynchronizeTask implements CommandLineRunner {

    @Autowired private UpgradeService upgradeService;
    @Autowired private ChargebeeMirrorService chargebeeMirrorService;

    private Map<String, Mono<Plan>> cachedPlans;

    public SynchronizeTask() {
        this.cachedPlans = new ConcurrentHashMap<>();
    }

    @Override
    public void run(String... args) {
        upgradeService.getUpgradedCountByUser()
            .flatMap(result -> {
                var subscription = getSubscription(result.getId())
                    .cache();

                var plan = subscription
                    .map(Subscription::getPlanId)
                    .flatMap(this::getPlan);

                return Mono.zip(
                    Mono.just(result),
                    subscription,
                    plan
                );
            })
            .map(tuple -> {
                var status = getStatus(tuple.getT1(), tuple.getT2(), tuple.getT3());
                var userId = tuple.getT1().getId();
                return Tuples.of(userId, status);
            })
            .collect(Collectors.toMap(Tuple2::getT1, Tuple2::getT2))
            .delayUntil(upgradeService::setStatusForUsers)
            .block();
    }

    public Mono<Subscription> getSubscription(String subscriptionId) {
        return chargebeeMirrorService.getEntity(EntityType.fromSingular("subscription"), subscriptionId, Subscription.class);
    }

    public synchronized Mono<Plan> getPlan(String planId) {
        return cachedPlans.computeIfAbsent(planId, __ ->
            chargebeeMirrorService.getEntity(EntityType.fromSingular("plan"), planId, Plan.class)
                .cache()
        );
    }

    public static UpgradeStatus getStatus(AggregateResult result, Subscription subscription, Plan plan) {
        if(!subscription.isActive()) {
            return UpgradeStatus.INACTIVE;
        }

        var usedUpgrades = result.getCount();
        var allocatedUpgrades = plan.getMetaData().get("features").get("upgrades").asInt();
        if(usedUpgrades > allocatedUpgrades) {
            return UpgradeStatus.LIMIT_EXCEEDED;
        }

        return UpgradeStatus.ACTIVE;
    }

}
