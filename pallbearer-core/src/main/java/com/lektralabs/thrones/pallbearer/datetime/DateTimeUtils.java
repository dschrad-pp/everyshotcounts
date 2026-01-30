package com.lektralabs.thrones.pallbearer.datetime;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Optional;
import java.util.TimeZone;

public class DateTimeUtils {
    private static final DateTimeFormatter ISODATETIME_FORMATTER = ISODateTimeFormat.dateTime();
    private static final DateTimeFormatter SHORT_DATETIME_FORMATTER = DateTimeFormat.forPattern("MMMM dd, yyyy hh:mm aa");
    private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormat.forPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter MMDDYYYY_DATE_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy");
    private static final DateTimeFormatter MMDD_DATE_FORMATTER = DateTimeFormat.forPattern("MM/dd");

    private static final DateTimeFormatter FFMPEG_DURATION_FORMATTER = DateTimeFormat.forPattern("HH:mm:ss.SSS");

    private static final int TWO_WEEKS_IN_DAYS = 14;

    // not really, but any dates in our system all "guaranteed" to be between these two dates
    public static final DateTime BEGINNING_OF_TIME = new DateTime(1900, 1, 1, 0, 0, 0);
    public static final DateTime END_OF_TIME = new DateTime(2100, 1, 1, 0, 0, 0);

    public static DateTime now(){
        return new DateTime(ISOChronology.getInstanceUTC());
    }

    public static Long getCurrentDateInMillis(){
        return now().getMillis();
    }

    public static Optional<DateTime> fromEpoch(String epochTimeUTC){
        Optional<Long> instantOpt = NumberUtils.toLong(epochTimeUTC);
        return instantOpt.map((input) -> new DateTime(input, ISOChronology.getInstanceUTC()));
    }

    public static Optional<DateTime> fromEpoch(String epochTime, final TimeZone zone){
        Optional<Long> instantOpt = NumberUtils.toLong(epochTime);
        return instantOpt.map((input) -> new DateTime(input, DateTimeZone.forTimeZone(zone)));
    }

    public static DateTime fromEpoch(long epochTimeUTC){
        return new DateTime(epochTimeUTC, ISOChronology.getInstanceUTC());
    }

    public static String formatDateTime(DateTime input){
        return ISODATETIME_FORMATTER.print(input);
    }

    public static String formatDateTime(Long millis){
        Optional<Long> millisOpt = Optional.ofNullable(millis);
        return millisOpt.isPresent() ? formatDateTime(new DateTime(millisOpt.get())) : "";
    }

    public static String formatDateTimeShort(DateTime input){
        return SHORT_DATETIME_FORMATTER.print(input);
    }

    public static String formatDateTimeShort(Long millis){
        Optional<Long> millisOpt = Optional.ofNullable(millis);
        return millisOpt.isPresent() ? formatDateTimeShort(new DateTime(millisOpt.get())) : "";
    }

    public static String formatDateTimeShort(Long millis, DateTimeZone zone){
        Optional<Long> millisOpt = Optional.ofNullable(millis);
        return millisOpt.isPresent() ? SHORT_DATETIME_FORMATTER.print(new DateTime(millisOpt.get(), zone)) : "";
    }

    public static String formatDateShort(DateTime input){
        return SHORT_DATE_FORMATTER.print(input);
    }

    public static String formatDateShort(Long millis){
        Optional<Long> millisOpt = Optional.ofNullable(millis);
        return millisOpt.isPresent() ? formatDateShort(new DateTime(millisOpt.get())) : "";
    }

    public static String formatDateShort(Long millis, DateTimeZone zone){
        Optional<Long> millisOpt = Optional.ofNullable(millis);
        return millisOpt.isPresent() ? formatDateShort(new DateTime(millisOpt.get(), zone)) : "";
    }

    public static String formatDateShort(String input){
        Optional<Long> millisOpt = NumberUtils.toLong(input);
        return millisOpt.isPresent() ? formatDateShort(new DateTime(millisOpt.get())) : "";
    }

    public static DateTime parseDateShort(String input){
        return SHORT_DATE_FORMATTER.parseDateTime(input);
    }

    public static String formatDateCondensed(DateTime input){
        return MMDDYYYY_DATE_FORMATTER.print(input);
    }

    public static String formatDateCondensed(Long millis){
        Optional<Long> millisOpt = Optional.ofNullable(millis);
        return millisOpt.isPresent() ? formatDateCondensed(new DateTime(millisOpt.get())) : "";
    }

    public static String formatDateMinimal(DateTime input){
        return MMDD_DATE_FORMATTER.print(input);
    }

    public static String formatDateMinimal(Long millis){
        Optional<Long> millisOpt = Optional.ofNullable(millis);
        return millisOpt.isPresent() ? formatDateCondensed(new DateTime(millisOpt.get())) : "";
    }

    public static String formatDurationWithSeconds(Integer seconds) {
        return FFMPEG_DURATION_FORMATTER.print(
                BEGINNING_OF_TIME.withTimeAtStartOfDay()
                        .plusSeconds(seconds));
    }

    public static String formatDurationWithSeconds(Double seconds) {
        return FFMPEG_DURATION_FORMATTER.print(
                BEGINNING_OF_TIME.withTimeAtStartOfDay()
                        .plusMillis((int)Math.round(seconds * 1000)));
    }

    public static DateTime parseDateMinimal(String input){
        return MMDD_DATE_FORMATTER.parseDateTime(input);
    }


    /* Year */
    public static DateTime endOfYear(DateTime dateTime) {
        return endOfDay(dateTime).withMonthOfYear(12).withDayOfMonth(31);
    }

    public static DateTime beginningOfYear(DateTime dateTime) {
        return beginningOfMonth(dateTime).withMonthOfYear(1);
    }

    /* Semi-Yearly */
    public static DateTime endOfSemiYear(DateTime dateTime) {
        int m = dateTime.getMonthOfYear();
        int mm = (m >= 1 && m <= 6) ? 6 : 12;
        int dd = (mm == 6) ?  30: 31;
        return endOfDay(dateTime).withMonthOfYear(mm).withDayOfMonth(dd);
    }

    public static DateTime beginningOfSemiYear(DateTime dateTime) {
        int m = dateTime.getMonthOfYear();
        int mm = (m >= 1 && m <= 6) ? 1 : 7;
        return beginningOfMonth(dateTime).withMonthOfYear(mm);
    }

    /* Quarter */
    public static DateTime endOfQuarter(DateTime dateTime) {
        int m = dateTime.getMonthOfYear();
        int mm = (m >= 1 && m <= 3) ? 3 : ((m >=4 && m <= 6) ? 6 : ((m >= 7 && m <= 9) ? 9 : 12));
        int dd = (mm == 3 || mm == 12) ?  31: 30;
        return endOfDay(dateTime).withMonthOfYear(mm).withDayOfMonth(dd);
    }

    public static DateTime beginningOfQuarter(DateTime dateTime) {
        int m = dateTime.getMonthOfYear();
        int mm = (m >=1 && m <= 3) ? 1 : ((m >=4 && m <= 6) ? 4 : ((m >= 7 && m <= 9) ? 7 : 10));
        return beginningOfMonth(dateTime).withMonthOfYear(1);
    }

    /* Month */
    public static DateTime endOfMonth(DateTime dateTime) {
        return endOfDay(dateTime).withDayOfMonth(dateTime.dayOfMonth().getMaximumValue());
    }

    public static DateTime beginningOfMonth(DateTime dateTime) {
        return beginningOfDay(dateTime).withDayOfMonth(1);
    }

    /* Semi-Month */
    public static DateTime endOfSemiMonth(DateTime dateTime) {
        int d = dateTime.getDayOfMonth();
        int dd = (d >=1 && d <= 15) ? 15 : dateTime.dayOfMonth().getMaximumValue();
        return endOfDay(dateTime).withDayOfMonth(dd);
    }

    public static DateTime beginningOfSemiMonth(DateTime dateTime) {
        int d = dateTime.getDayOfMonth();
        int dd = (d >=1 && d <= 15) ? 1 : 16;
        return beginningOfDay(dateTime).withDayOfMonth(dd);
    }

    /* Bi-Weekly */
    // Bi-Weekly needs a fixed date to calculate when the biweekly period will start. The fixed date can occur anytime
    // in the past
    public static DateTime beginningOfBiWeekly(DateTime input, DateTime fixedDate) {
        int delta = Days.daysBetween(fixedDate.withTimeAtStartOfDay(), input.withTimeAtStartOfDay()).getDays();
        int offset = delta % TWO_WEEKS_IN_DAYS;
        offset = (offset < 0 ) ? offset + TWO_WEEKS_IN_DAYS : offset;
        DateTime startDate = input.minusDays(offset);
        // System.out.println(String.format("delta [%d] offset [%d] start [%s]", delta, offset, formatDateCondensed(startDate)));
        return beginningOfDay(startDate);
    }

    public static DateTime endOfBiWeekly(DateTime input, DateTime fixedDate){
        DateTime startDate = beginningOfBiWeekly(input, fixedDate);
        DateTime endDate = startDate.plusDays(TWO_WEEKS_IN_DAYS - 1);
        return endOfDay(endDate);
    }

    /* Week */
    // Joda weeks go Mon - Sun (ISO and European std), so to get the Sun - Sat week, we need to go to the next week
    public static DateTime endOfWeek(DateTime dateTime) {
        int plusDays = (dateTime.getDayOfWeek() == DateTimeConstants.SUNDAY) ? 7 : 0;
        return endOfDay(dateTime).withDayOfWeek(DateTimeConstants.SATURDAY).plusDays(plusDays);
    }

    // Joda weeks go Mon - Sun (ISO and European std), so to get the Sun - Sat week, we need to go to the prev week
    public static DateTime beginningOfWeek(DateTime dateTime) {
        int minusDays = (dateTime.getDayOfWeek() == DateTimeConstants.SUNDAY) ? 0 : 7;
        return beginningOfDay(dateTime).withDayOfWeek(DateTimeConstants.SUNDAY).minusDays(minusDays);
    }


    /* Day */
    public static DateTime endOfDay(DateTime dateTime) {
        return endOfHour(dateTime).withHourOfDay(23);
    }

    public static DateTime beginningOfDay(DateTime dateTime) {
        return beginningOfHour(dateTime).withHourOfDay(0);
    }

    /* Hour */
    public static DateTime beginningOfHour(DateTime dateTime) {
        return dateTime.withMillisOfSecond(0).withSecondOfMinute(0).withMinuteOfHour(0);
    }

    public static DateTime endOfHour(DateTime dateTime) {
        return dateTime.withMillisOfSecond(999).withSecondOfMinute(59).withMinuteOfHour(59);
    }


}
