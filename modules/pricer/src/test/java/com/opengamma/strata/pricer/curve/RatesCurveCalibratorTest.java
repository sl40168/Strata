/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

import com.google.common.collect.Sets;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.date.*;
import com.opengamma.strata.basics.index.IborIndices;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.*;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.TermDepositCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.product.deposit.type.TermDepositConventions;
import com.opengamma.strata.product.deposit.type.TermDepositTemplate;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;
import static  com.opengamma.strata.basics.currency.Currency.CNY;

/**
 * Tests {@link RatesCurveCalibrator}.
 */
public class RatesCurveCalibratorTest {

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

	@Test
	public void test_toString() {
		assertThat(RatesCurveCalibrator.standard().toString()).isEqualTo("CurveCalibrator[ParSpread]");
	}

	@Test
	public void testCfetsFR007Curve() {
		RatesCurveCalibrator calibrator = RatesCurveCalibrator.standard();
		Collection<CurveDefinition> curveDefinitions = Collections
				.<CurveDefinition>singletonList(this.createFR007CurveDefn());
		CurveGroupName name = CurveGroupName.of("CFETS-FR007");
		Collection<RatesCurveGroupEntry> entries = Collections.singletonList(RatesCurveGroupEntry.builder().curveName(CurveName.of(curveName))
				.discountCurrencies(Sets.newHashSet(CNY)).indices(IborIndices.CNY_REPO_1W).build());
		RatesCurveGroupDefinition curveGroupDefn = RatesCurveGroupDefinition.of(name, entries, curveDefinitions);
		Map<QuoteId, Double> quotes = ImmutableMap.<QuoteId, Double>builder()
				.put(QuoteId.of(StandardId.of(schema, n1d)), 0.0185).put(QuoteId.of(StandardId.of(schema, n1w)), 0.0185)
				.put(QuoteId.of(StandardId.of(schema, n1m)), 0.0188)
				.put(QuoteId.of(StandardId.of(schema, n3m)), 0.018875)
				.put(QuoteId.of(StandardId.of(schema, n6m)), 0.0191)
				.put(QuoteId.of(StandardId.of(schema, n9m)), 0.018875)
				.put(QuoteId.of(StandardId.of(schema, n1y)), 0.018687)
				.put(QuoteId.of(StandardId.of(schema, n2y)), 0.0186)
				.put(QuoteId.of(StandardId.of(schema, n3y)), 0.018975)
				.put(QuoteId.of(StandardId.of(schema, n4y)), 0.019525)
				.put(QuoteId.of(StandardId.of(schema, n5y)), 0.020125)
				.put(QuoteId.of(StandardId.of(schema, n7y)), 0.02125)
				.put(QuoteId.of(StandardId.of(schema, n10y)), 0.022175).build();
		MarketData marketData = MarketData.of(VAL_DATE, quotes);
		Map<HolidayCalendarId, HolidayCalendar> calendars = new HashMap<>();
		HolidayCalendar cal = HolidayCalendars.of("CNBE");
		calendars.put(cal.getId(), cal);
		cal = HolidayCalendars.of("NoHolidays");
		calendars.put(cal.getId(), cal);
		ImmutableReferenceData refData = ImmutableReferenceData.of(calendars);
		calibrator.calibrate(curveGroupDefn, marketData, refData);
	}

	private CurveDefinition createFR007CurveDefn() {
		List<CurveNode> nodes = Lists.newLinkedList();
		nodes.add(TermDepositCurveNode.of(
				TermDepositTemplate.builder().depositPeriod(Period.ofDays(1))
						.convention(TermDepositConventions.CNY_SHORT_DEPOSIT_T0).build(),
				QuoteId.of(StandardId.of(schema, n1d))));
		nodes.add(FixedIborSwapCurveNode.of(
				FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_1W)
						.convention(FixedIborSwapConventions.CNY_REPO_1W_1M_A365F).periodToStart(Period.ZERO).build(),
				QuoteId.of(StandardId.of(schema, n1w))));
		nodes.add(FixedIborSwapCurveNode.of(
				FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_1M)
						.convention(FixedIborSwapConventions.CNY_REPO_1W_1M_A365F).periodToStart(Period.ZERO).build(),
				QuoteId.of(StandardId.of(schema, n1m))));
		nodes.add(FixedIborSwapCurveNode.of(
				FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_3M)
						.convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ZERO).build(),
				QuoteId.of(StandardId.of(schema, n3m))));
		nodes.add(FixedIborSwapCurveNode.of(
				FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_6M)
						.convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ZERO).build(),
				QuoteId.of(StandardId.of(schema, n6m))));
		nodes.add(FixedIborSwapCurveNode.of(
				FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_9M)
						.convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ZERO).build(),
				QuoteId.of(StandardId.of(schema, n9m))));
		nodes.add(FixedIborSwapCurveNode.of(
				FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_1Y)
						.convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ZERO).build(),
				QuoteId.of(StandardId.of(schema, n1y))));
		nodes.add(FixedIborSwapCurveNode.of(
				FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_2Y)
						.convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ZERO).build(),
				QuoteId.of(StandardId.of(schema, n2y))));
		nodes.add(FixedIborSwapCurveNode.of(
				FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_3Y)
						.convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ZERO).build(),
				QuoteId.of(StandardId.of(schema, n3y))));
		nodes.add(FixedIborSwapCurveNode.of(
				FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_4Y)
						.convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ZERO).build(),
				QuoteId.of(StandardId.of(schema, n4y))));
		nodes.add(FixedIborSwapCurveNode.of(
				FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_5Y)
						.convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ZERO).build(),
				QuoteId.of(StandardId.of(schema, n5y))));
		nodes.add(FixedIborSwapCurveNode.of(
				FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_7Y)
						.convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ZERO).build(),
				QuoteId.of(StandardId.of(schema, n7y))));
		nodes.add(FixedIborSwapCurveNode.of(
				FixedIborSwapTemplate.builder().tenor(Tenor.TENOR_10Y)
						.convention(FixedIborSwapConventions.CNY_REPO_1W_3M_A365F).periodToStart(Period.ZERO).build(),
				QuoteId.of(StandardId.of(schema, n10y))));

		return InterpolatedNodalCurveDefinition.builder().nodes(nodes).name(CurveName.of(curveName))
				.dayCount(DayCounts.ACT_365F).xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.ZERO_RATE)
				.interpolator(CurveInterpolators.LINEAR).extrapolatorLeft(CurveExtrapolators.FLAT)
				.extrapolatorRight(CurveExtrapolators.FLAT).build();
	}
}
