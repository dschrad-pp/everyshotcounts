package com.lektralabs.thrones.pallbearer.jdbi;

import com.lektralabs.thrones.pallbearer.datetime.jdbi.DateTimeMapper;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.stringtemplate4.StringTemplateEngine;

import com.lektralabs.thrones.pallbearer.jdbi.reducer.OptionalUUIDMapper;

@ApplicationScoped
public class JdbiProvider {

    @Inject
    AgroalDataSource defaultDataSource;

    private Jdbi jdbi;

    @PostConstruct
    public void init() {
        SqlLogger sqlLogger = new JdbiSqlLogger();
        jdbi = Jdbi.create(defaultDataSource)
                .registerColumnMapper(new DateTimeMapper())
                .registerColumnMapper(new OptionalUUIDMapper())
                .registerColumnMapper(new com.lektralabs.thrones.pallbearer.jdbi.reducer.JsonMetadataColumnMapper())
                .setSqlLogger(sqlLogger)
                .installPlugin(new SqlObjectPlugin())
                .installPlugin(new PostgresPlugin())
                .setTemplateEngine(new StringTemplateEngine());
    }

    @Produces
    @Default
    @Dependent
    public Jdbi getJdbi() {
        return jdbi;
    }

    public AgroalDataSourceMetrics getDataSourceMetrics() {
        return this.defaultDataSource.getMetrics();
    }

}
