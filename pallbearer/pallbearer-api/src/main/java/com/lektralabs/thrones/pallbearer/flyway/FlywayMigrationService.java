package com.lektralabs.thrones.pallbearer.flyway;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import java.util.List;

@ApplicationScoped
public class FlywayMigrationService {
    private static final boolean FIXER_ENABLED = true;
    private static final List<String> SQL_LIST = List.of(
        // "DELETE FROM flyway_schema_history WHERE version IN ('202405051236', '202405212014');",
        // "DELETE FROM flyway_schema_history WHERE version IN ('202405212002');
        // "DELETE FROM flyway_schema_history WHERE version IN ('202410061335', '202410131658');"
        "DELETE FROM flyway_schema_history WHERE version = '202501010000';"
    );

    @Inject
    Flyway flyway;

    @Inject
    AgroalDataSource defaultDataSource;

    @Startup
    public void checkMigration() {
        System.out.println(">====> Running flyway >====>");
        fix();
        migrate();
        System.out.println("<====< Running flyway <====<");
    }

    protected void fix() {
        FlywayFixer flywayFixer = new FlywayFixer(FIXER_ENABLED, SQL_LIST);
        flywayFixer.fix(defaultDataSource);
    }

    protected void migrate() {
        FluentConfiguration config = new FluentConfiguration();
        config.dataSource(defaultDataSource)
                .outOfOrder(true)
                .locations("db/migration")
                .validateOnMigrate(true);
        Flyway flyway = new Flyway(config);
        flyway.migrate();
    }
}
