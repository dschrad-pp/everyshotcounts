package com.lektralabs.thrones.pallbearer.datetime.jdbi;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Map a {@link Timestamp} to a {@link DateTime}.
 *
 * @see org.jdbi.v3.jodatime2.DateTimeMapper
 */
public class DateTimeMapper implements ColumnMapper<DateTime> {

    private static Calendar CALENDAR;

    static {
        CALENDAR = Calendar.getInstance();
        CALENDAR.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public DateTime map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        Timestamp ts = r.getTimestamp(columnNumber, CALENDAR);
        return ts == null ? null : new DateTime(ts.getTime(), ISOChronology.getInstanceUTC());
    }
}
