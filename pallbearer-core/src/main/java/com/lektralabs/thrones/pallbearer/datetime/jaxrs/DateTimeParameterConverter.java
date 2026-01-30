package com.lektralabs.thrones.pallbearer.datetime.jaxrs;

import jakarta.ws.rs.ext.ParamConverter;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class DateTimeParameterConverter implements ParamConverter<DateTime> {
    private static final DateTimeFormatter ISODATETIME_FORMATTER = ISODateTimeFormat.dateTime();

    @Override
    public DateTime fromString(String string) {
        try {
            return ISODATETIME_FORMATTER.parseDateTime(string).toDateTime(ISOChronology.getInstanceUTC());
        } catch (Exception e) {
            System.out.println(String.format("Could not parse date [%s]", string));
        }
        return null;
    }

    @Override
    public String toString(DateTime dateTime) {
        return ISODATETIME_FORMATTER.print(dateTime.toDateTime(ISOChronology.getInstanceUTC()));
    }
}