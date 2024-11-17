package com.opengamma.strata.pricer.curve;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.curve.RatesCurveGroupEntry;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.TermDepositCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.deposit.type.TermDepositConventions;
import com.opengamma.strata.product.deposit.type.TermDepositTemplate;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;
import org.assertj.core.api.DoubleAssert;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.opengamma.strata.basics.currency.Currency.CNY;
import static org.assertj.core.api.Assertions.assertThat;

public class CfetsRatesCurveBootstrapTest {

  private static final String curveName = "CFETS-FR007";
  private static final LocalDate VAL_DATE = LocalDate.of(2024, 6, 5);
  private static final String schema = "CFETS";
  private static final String n1d = "n1d";
  private static final String n1w = "n1w";
  private static final String n1m = "n1m";
  private static final String n3m = "n3m";;
  private static final String n6m = "n6m";
  private static final String n9m = "n9m";
  private static final String n1y = "n1y";
  private static final String n2y = "n2y";
  private static final String n3y = "n3y";
  private static final String n4y = "n4y";
  private static final String n5y = "n5y";
  private static final String n7y = "n7y";
  private static final String n10y = "n10y";

  public static final double DELTA_Y = 1e-8;
  public static final double DELTA_X = 1e-8;

  private static ReferenceData getRefData() {
    Map<HolidayCalendarId, HolidayCalendar> calendars = new HashMap<>();
    HolidayCalendar cal = HolidayCalendars.of(HolidayCalendarIds.CNBE.getName());
    calendars.put(cal.getId(), cal);
    cal = HolidayCalendars.of("NoHolidays");
    calendars.put(cal.getId(), cal);
    return ImmutableReferenceData.of(calendars);
  }

  @Test
  public void testFr007() {
    CfetsRatesCurveBootstrap bootstrap = CfetsRatesCurveBootstrap.standard();
    ReferenceData refData = getRefData();
    DayCount dayCount = DayCounts.ACT_365F;
    CurveId curveId = CurveId.of("CFETS", "FR007");

    Collection<CurveDefinition> curveDefinitions = Collections
                                                       .<CurveDefinition>singletonList(this.createFR007CurveDefn());
    CurveGroupName name = CurveGroupName.of("CFETS-FR007");
    Collection<RatesCurveGroupEntry> entries = Collections.singletonList(RatesCurveGroupEntry.builder().curveName(CurveName.of(curveName))
                                                                             .discountCurrencies(Sets.newHashSet(CNY)).indices(IborIndices.CNY_REPO_1W).build());

    RatesCurveGroupDefinition curveGroupDefn = RatesCurveGroupDefinition.of(name, entries, curveDefinitions);
    Map<QuoteId, Double> quotes = ImmutableMap.<QuoteId, Double>builder()
                                      .put(QuoteId.of(StandardId.of(schema, n1d)), 0.0184).put(QuoteId.of(StandardId.of(schema, n1w)), 0.0184)
                                      .put(QuoteId.of(StandardId.of(schema, n1m)), 0.01895)
                                      .put(QuoteId.of(StandardId.of(schema, n3m)), 0.018625)
                                      .put(QuoteId.of(StandardId.of(schema, n6m)), 0.018366)
                                      .put(QuoteId.of(StandardId.of(schema, n9m)), 0.018391)
                                      .put(QuoteId.of(StandardId.of(schema, n1y)), 0.018288)
                                      .put(QuoteId.of(StandardId.of(schema, n2y)), 0.018325)
                                      .put(QuoteId.of(StandardId.of(schema, n3y)), 0.018725)
                                      .put(QuoteId.of(StandardId.of(schema, n4y)), 0.019463)
                                      .put(QuoteId.of(StandardId.of(schema, n5y)), 0.0202)
                                      .put(QuoteId.of(StandardId.of(schema, n7y)), 0.021363)
                                      .put(QuoteId.of(StandardId.of(schema, n10y)), 0.022363).build();
    MarketData marketData = MarketData.of(VAL_DATE, quotes);

    ImmutableRatesProvider rateProvider =
        bootstrap.bootstrap(ImmutableList.of(curveGroupDefn), ImmutableRatesProvider.builder(VAL_DATE).build(),
            marketData, refData);

    InterpolatedNodalCurve curve = (InterpolatedNodalCurve) rateProvider.getIndexCurves().get(IborIndices.CNY_REPO_1W);
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

  private CurveDefinition createFR007CurveDefn() {
    List<CurveNode> nodes = Lists.newLinkedList();
    nodes.add(TermDepositCurveNode.of(
        TermDepositTemplate.builder().depositPeriod(Period.ofDays(1))
            .convention(TermDepositConventions.CNY_SHORT_DEPOSIT_T0).build(),
        QuoteId.of(StandardId.of(schema, n1d))));
    nodes.add(FixedIborSwapCurveNode.of(
        FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_1W)
            .convention(FixedIborSwapConventions.CNY_REPO_1W_1M_A365F).periodToStart(Period.ofDays(1)).build(),
        QuoteId.of(StandardId.of(schema, n1w))));
    nodes.add(FixedIborSwapCurveNode.of(
        FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_1M)
            .convention(FixedIborSwapConventions.CNY_REPO_1W_1M_A365F).periodToStart(Period.ofDays(1)).build(),
        QuoteId.of(StandardId.of(schema, n1m))));
    nodes.add(FixedIborSwapCurveNode.of(
        FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_3M)
            .convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ofDays(1)).build(),
        QuoteId.of(StandardId.of(schema, n3m))));
    nodes.add(FixedIborSwapCurveNode.of(
        FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_6M)
            .convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ofDays(1)).build(),
        QuoteId.of(StandardId.of(schema, n6m))));
    nodes.add(FixedIborSwapCurveNode.of(
        FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_9M)
            .convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ofDays(1)).build(),
        QuoteId.of(StandardId.of(schema, n9m))));
    nodes.add(FixedIborSwapCurveNode.of(
        FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_1Y)
            .convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ofDays(1)).build(),
        QuoteId.of(StandardId.of(schema, n1y))));
    nodes.add(FixedIborSwapCurveNode.of(
        FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_2Y)
            .convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ofDays(1)).build(),
        QuoteId.of(StandardId.of(schema, n2y))));
    nodes.add(FixedIborSwapCurveNode.of(
        FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_3Y)
            .convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ofDays(1)).build(),
        QuoteId.of(StandardId.of(schema, n3y))));
    nodes.add(FixedIborSwapCurveNode.of(
        FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_4Y)
            .convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ofDays(1)).build(),
        QuoteId.of(StandardId.of(schema, n4y))));
    nodes.add(FixedIborSwapCurveNode.of(
        FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_5Y)
            .convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ofDays(1)).build(),
        QuoteId.of(StandardId.of(schema, n5y))));
    nodes.add(FixedIborSwapCurveNode.of(
        FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_7Y)
            .convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ofDays(1)).build(),
        QuoteId.of(StandardId.of(schema, n7y))));
    nodes.add(FixedIborSwapCurveNode.of(
        FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_10Y)
            .convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ofDays(1)).build(),
        QuoteId.of(StandardId.of(schema, n10y))));

    return InterpolatedNodalCurveDefinition.builder().nodes(nodes).name(CurveName.of(curveName))
               .dayCount(DayCounts.ACT_365F).xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.ZERO_RATE)
               .interpolator(CurveInterpolators.LINEAR).extrapolatorLeft(CurveExtrapolators.FLAT)
               .extrapolatorRight(CurveExtrapolators.FLAT).build();
  }
}
