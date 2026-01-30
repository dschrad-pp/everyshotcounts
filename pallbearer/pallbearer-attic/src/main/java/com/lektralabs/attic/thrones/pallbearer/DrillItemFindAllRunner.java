package com.lektralabs.attic.thrones.pallbearer;

import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillItemService;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

public class DrillItemFindAllRunner {

    public static void main(String[] args) {
        Quarkus.run(DrillItemFindAllApp.class);
    }

    public static class DrillItemFindAllApp implements QuarkusApplication {
        @Inject
        DrillItemService drillItemService;

        @Override
        public int run(String... args) throws Exception {
            findAllDrillItem();
            System.out.println("DONE!");
            Quarkus.waitForExit();
            return 0;
        }

        @ActivateRequestContext
        public void findAllDrillItem() {
            FindOptions findOptions = new FindOptions(3, 0, Optional.of("name"), "DESC");
            List<DrillItemRow> rows = drillItemService.findAll(findOptions);
            System.out.println("rows = " + rows);
        }
    }
}

