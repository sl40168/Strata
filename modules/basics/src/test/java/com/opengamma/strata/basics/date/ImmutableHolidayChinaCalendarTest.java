package com.opengamma.strata.basics.date;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;

public class ImmutableHolidayChinaCalendarTest {

    private static final HolidayCalendarId chinaIB = HolidayCalendarId.of("CNBE");

    private static final LocalDate FEB_4_2024 = LocalDate.of(2024, Month.FEBRUARY, 4);
    private static final LocalDate FEB_18_2024 = LocalDate.of(2024, Month.FEBRUARY, 18);

    private static final LocalDate FEB_10_2024 = LocalDate.of(2024, Month.FEBRUARY, 10);
    private static final LocalDate FEB_11_2024 = LocalDate.of(2024, Month.FEBRUARY, 11);
    private static final LocalDate FEB_12_2024 = LocalDate.of(2024, Month.FEBRUARY, 12);
    private static final LocalDate FEB_13_2024 = LocalDate.of(2024, Month.FEBRUARY, 13);
    private static final LocalDate FEB_14_2024 = LocalDate.of(2024, Month.FEBRUARY, 14);
    private static final LocalDate FEB_15_2024 = LocalDate.of(2024, Month.FEBRUARY, 15);
    private static final LocalDate FEB_16_2024 = LocalDate.of(2024, Month.FEBRUARY, 16);
    private static final LocalDate FEB_17_2024 = LocalDate.of(2024, Month.FEBRUARY, 17);

    @Test
    public void testChinaIBCalendar() {


        ImmutableList<LocalDate> holidays = ImmutableList.of(FEB_10_2024, FEB_11_2024, FEB_12_2024, FEB_13_2024, FEB_14_2024, FEB_15_2024, FEB_16_2024,
                FEB_17_2024);
        ImmutableList<LocalDate> workingWeekends = ImmutableList.of(FEB_4_2024, FEB_18_2024);
//        ImmutableHolidayCalendar calendar = ImmutableHolidayCalendar.of(chinaIB, holidays, ImmutableList.of(SATURDAY, SUNDAY), workingWeekends);
        HolidayCalendar calendar  =  HolidayCalendarIniLookup.INSTANCE.lookupAll().get("CNBE");
        for (LocalDate ww : workingWeekends) {
            Assertions.assertTrue(calendar.isBusinessDay(ww), ww::toString);
        }

        for (LocalDate hd : holidays) {
            Assertions.assertTrue(calendar.isHoliday(hd), hd::toString);
        }

    }
}
