package com.opengamma.strata.product.deposit.type;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.ImmutableHolidayCalendar;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.ResolvedTermDeposit;
import com.opengamma.strata.product.deposit.ResolvedTermDepositTrade;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class StandardTermDepositConventionsTest {
  private static final List<LocalDate> holidays = Arrays.asList(LocalDate.of(2024, Month.JANUARY, 1),
      LocalDate.of(2024, Month.FEBRUARY, 12), LocalDate.of(2024, Month.FEBRUARY, 13),
      LocalDate.of(2024, Month.FEBRUARY, 14), LocalDate.of(2024, Month.FEBRUARY, 15),
      LocalDate.of(2024, Month.FEBRUARY, 16), LocalDate.of(2024, Month.APRIL, 4),
      LocalDate.of(2024, Month.APRIL, 5), LocalDate.of(2024, Month.MAY, 1), LocalDate.of(2024, Month.MAY, 2),
      LocalDate.of(2024, Month.MAY, 3), LocalDate.of(2024, Month.JUNE, 10),
      LocalDate.of(2024, Month.SEPTEMBER, 16), LocalDate.of(2024, Month.SEPTEMBER, 17),
      LocalDate.of(2024, Month.OCTOBER, 1), LocalDate.of(2024, Month.OCTOBER, 2),
      LocalDate.of(2024, Month.OCTOBER, 3), LocalDate.of(2024, Month.OCTOBER, 4),
      LocalDate.of(2024, Month.OCTOBER, 7));

  private static final List<LocalDate> workingWeekends = Arrays.asList(LocalDate.of(2024, Month.FEBRUARY, 4),
      LocalDate.of(2024, Month.FEBRUARY, 18), LocalDate.of(2024, Month.APRIL, 7),
      LocalDate.of(2024, Month.APRIL, 28), LocalDate.of(2024, Month.MAY, 11),
      LocalDate.of(2024, Month.SEPTEMBER, 14), LocalDate.of(2024, Month.SEPTEMBER, 29),
      LocalDate.of(2024, Month.OCTOBER, 12));

  private static final HolidayCalendarId CNBE_CALENDAR = HolidayCalendarId.of("CNBE");

  @Test
  public void testFr007() {
    LocalDate tradeDate = LocalDate.of(2024, 11, 15);
    Period period = Period.ofDays(1);
    HolidayCalendar cnbe = ImmutableHolidayCalendar.of(CNBE_CALENDAR, holidays,
        Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), workingWeekends);
    ReferenceData refData = ReferenceData
                                .of(ImmutableMap.<HolidayCalendarId, HolidayCalendar>builder().put(cnbe.getId(), cnbe).build());

    int notional = 1000000;
    double rate = 0.019;

    TermDepositTrade trade =
        StandardTermDepositConventions.CNY_SHORT_DEPOSIT_T0.createTrade(tradeDate, period, BuySell.BUY, notional, rate,
            refData);

    ResolvedTermDepositTrade resolve = trade.resolve(refData);
    ResolvedTermDeposit product = resolve.getProduct();
    assertThat(product.getInterest()).isEqualTo(notional * rate * (3d/ 365d), Offset.offset(0.0001));
  }
}
