package com.lektralabs.attic.thrones.pallbearer;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillItemPartial;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillItemService;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.UUID;

public class DrillItemCreateRunner {

    public static void main(String[] args) {
        Quarkus.run(DrillItemCreateApp.class);
    }

    public static class DrillItemCreateApp implements QuarkusApplication {
        @Inject
        DrillItemService drillItemService;

        @Override
        public int run(String... args) throws Exception {
            createDrillItem();
            System.out.println("DONE!");
            Quarkus.waitForExit();
            return 0;
        }

        @ActivateRequestContext
        public void createDrillItem() {
            DrillItemPartial partial = DrillItemPartial.builder()
                .drillGroupId(UUID.fromString("6ccc50a3-f356-416e-98db-ed50e58e976b"))
                .name(Optional.of("Middle School Drill #1"))
                .description(Optional.of("Middle School Drill #1"))
                .mediaId(Optional.of(UUID.fromString("5c347a34-99fc-49c1-a344-9bd51ed1358f")))
                .drillItemOrder(1)
                .passingScore(10)
                .visibilityCode("PROTECTED")
                .allowRetryCode("ACTIVE")
                .retryMax(3)
                .timeLimitMs(180000L)
                .build();
            drillItemService.create(partial);

        }
    }
}
