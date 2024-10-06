package com.opengamma.strata.examples.china;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.*;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import org.assertj.core.api.DoubleAssert;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;


public class CfetsBootStrapResolverTest {
	private static ReferenceData getRefData() {
		Map<HolidayCalendarId, HolidayCalendar> calendars = new HashMap<>();
		HolidayCalendar cal = HolidayCalendars.of(CfetsBootStrapResolver.HOLIDAY_CALENDAR_ID.getName());
		calendars.put(cal.getId(), cal);
		cal = HolidayCalendars.of("NoHolidays");
		calendars.put(cal.getId(), cal);
		return ImmutableReferenceData.of(calendars);
	}

	public static final double DELTA_Y = 1e-8;
	public static final double DELTA_X = 1e-8;
	@Test
	public void testFr007() {
		ReferenceData refData = getRefData();
		CfetsBootStrapResolver resolver = new CfetsBootStrapResolver(refData);
		LocalDate anchor = LocalDate.of(2024, Month.JUNE, 5);
		DayCount dayCount = DayCounts.ACT_365F;
		CurveId curveId = CurveId.of("CFETS", "FR007");
		List<CfetsBootStrapResolver.Node> nodes = Arrays.asList(
				new CfetsBootStrapResolver.Node(Period.ZERO, Period.ofDays(1), BusinessDayConventions.FOLLOWING,
						Frequency.P1D, 0.0184),
				new CfetsBootStrapResolver.Node(Period.ofDays(1), Period.ofWeeks(1), BusinessDayConventions.FOLLOWING,
						Frequency.P1W, 0.0184),
				new CfetsBootStrapResolver.Node(Period.ofDays(1), Period.ofMonths(1),
						BusinessDayConventions.MODIFIED_FOLLOWING, Frequency.P1M, 0.01895),
				new CfetsBootStrapResolver.Node(Period.ofDays(1), Period.ofMonths(3),
						BusinessDayConventions.MODIFIED_FOLLOWING, Frequency.P3M, 0.018625),
				new CfetsBootStrapResolver.Node(Period.ofDays(1), Period.ofMonths(6),
						BusinessDayConventions.MODIFIED_FOLLOWING, Frequency.P3M, 0.018366),
				new CfetsBootStrapResolver.Node(Period.ofDays(1), Period.ofMonths(9),
						BusinessDayConventions.MODIFIED_FOLLOWING, Frequency.P3M, 0.018391),
				new CfetsBootStrapResolver.Node(Period.ofDays(1), Period.ofYears(1),
						BusinessDayConventions.MODIFIED_FOLLOWING, Frequency.P3M, 0.018288),
				new CfetsBootStrapResolver.Node(Period.ofDays(1), Period.ofYears(2),
						BusinessDayConventions.MODIFIED_FOLLOWING, Frequency.P3M, 0.018325),
				new CfetsBootStrapResolver.Node(Period.ofDays(1), Period.ofYears(3),
						BusinessDayConventions.MODIFIED_FOLLOWING, Frequency.P3M, 0.018725),
				new CfetsBootStrapResolver.Node(Period.ofDays(1), Period.ofYears(4),
						BusinessDayConventions.MODIFIED_FOLLOWING, Frequency.P3M, 0.019463),
				new CfetsBootStrapResolver.Node(Period.ofDays(1), Period.ofYears(5),
						BusinessDayConventions.MODIFIED_FOLLOWING, Frequency.P3M, 0.0202),
				new CfetsBootStrapResolver.Node(Period.ofDays(1), Period.ofYears(7),
						BusinessDayConventions.MODIFIED_FOLLOWING, Frequency.P3M, 0.021363),
				new CfetsBootStrapResolver.Node(Period.ofDays(1), Period.ofYears(10),
						BusinessDayConventions.MODIFIED_FOLLOWING, Frequency.P3M, 0.022363));

		InterpolatedNodalCurve curve = (InterpolatedNodalCurve) resolver.resolve(anchor, nodes, dayCount, CfetsBootStrapResolver.HOLIDAY_CALENDAR_ID, curveId);

		double[] expectedXValues = new double[] { 0.00273973, 0.02191781, 0.09041096, 0.25479452, 0.50410959, 0.75068493,
						1.00273973, 2.00821918, 3.00547945, 4.00547945, 5.00547945, 7.00547945, 10.00821918 };

		double[] expectedYValues = new double[] { 0.018399536, 0.018397102, 0.018918072, 0.018579463, 0.018323663, 0.018348861,
						0.018245823, 0.018283116, 0.018688923, 0.019443740, 0.020203051, 0.021413670, 0.022466493 };

		assertThat(curve.getXValues().toArray()).hasSize(expectedXValues.length);
		for (int i = 0; i < expectedXValues.length; i++) {
			new DoubleAssert(curve.getXValues().get(i)).describedAs("X value %d is not pass test.", i).isCloseTo(expectedXValues[i], Offset.offset(DELTA_X));
		}

		assertThat(curve.getYValues().toArray()).hasSize(expectedYValues.length);
		for (int i = 0; i < expectedYValues.length; i++) {
			new DoubleAssert(curve.getYValues().get(i)).describedAs("Y value %d is not pass test.", i).isCloseTo(expectedYValues[i], Offset.offset(DELTA_Y));
		}
	}
}
