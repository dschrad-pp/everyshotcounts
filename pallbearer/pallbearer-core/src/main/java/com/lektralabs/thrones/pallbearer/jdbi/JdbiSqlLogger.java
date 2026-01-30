package com.lektralabs.thrones.pallbearer.jdbi;

import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Duration;

/**
 * @see org.jdbi.v3.core.statement.Slf4JSqlLogger
 */
public class JdbiSqlLogger implements SqlLogger {
    private final Logger log;

    public JdbiSqlLogger() {
        this(LoggerFactory.getLogger("com.kazzah.mitchell.jdbi"));
    }

    public JdbiSqlLogger(Logger log) {
        this.log = log;
    }

    @Override
    public void logAfterExecution(StatementContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Executed in {} '{}' with parameters '{}'",
                    format(Duration.between(context.getExecutionMoment(), context.getCompletionMoment())),
                    context.getParsedSql().getSql(),
                    context.getBinding());
        }
    }

    @Override
    public void logException(StatementContext context, SQLException ex) {
        if (log.isErrorEnabled()) {
            log.error("Exception while executing '{}' with parameters '{}'",
                    context.getParsedSql().getSql(),
                    context.getBinding(),
                    ex);
        }
    }

    private static String format(Duration duration) {
        final long totalSeconds = duration.getSeconds();
        final long h = totalSeconds / 3600;
        final long m = (totalSeconds % 3600) / 60;
        final long s = totalSeconds % 60;
        final long ms = duration.toMillis() % 1000;
        return String.format(
                "%d:%02d:%02d.%03d",
                h, m, s, ms);
    }
}
