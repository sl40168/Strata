package com.opengamma.strata.examples.china;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataFactory;
import com.opengamma.strata.calc.marketdata.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.ObservableDataProvider;
import com.opengamma.strata.calc.marketdata.TimeSeriesProvider;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.examples.marketdata.ExampleData;
import com.opengamma.strata.loader.csv.FixingSeriesCsvLoader;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.measure.AdvancedMeasures;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.measure.curve.CurveMarketDataFunction;
import com.opengamma.strata.measure.fx.FxRateMarketDataFunction;
import com.opengamma.strata.measure.fxopt.FxOptionVolatilitiesMarketDataFunction;
import com.opengamma.strata.measure.rate.CfetsRatesCurveGroupMarketDataFunction;
import com.opengamma.strata.measure.rate.RatesCurveInputsMarketDataFunction;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

public class FR007PricingExample {
	private static final LocalDate VAL_DATE = LocalDate.of(2024, 4, 18);

	private static final ResourceLocator QUOTES_RESOURCE = ResourceLocator
			.ofClasspath("example-china-0418/quotes/quotes.csv");
	/**
	 * The location of the curve calibration groups file.
	 */
	private static final ResourceLocator GROUPS_RESOURCE = ResourceLocator
			.ofClasspath("example-china-0418/curves/groups.csv");
	/**
	 * The location of the curve calibration settings file.
	 */
	private static final ResourceLocator SETTINGS_RESOURCE = ResourceLocator
			.ofClasspath("example-china-0418/curves/settings.csv");
	/**
	 * The location of the curve calibration nodes file.
	 */
	private static final ResourceLocator CALIBRATION_RESOURCE = ResourceLocator
			.ofClasspath("example-china-0418/curves/calibrations.csv");
	/**
	 * The location of the historical fixing file.
	 */
	private static final ResourceLocator FIXINGS_RESOURCE = ResourceLocator
			.ofClasspath("example-china-0418/fixings/fr007.csv");

	/**
	 * The curve group name.
	 */
	private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("CNY-CFETS-FR007");

	/**
	 * Runs the example, pricing the instruments, producing the output as an ASCII
	 * table.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
		// setup calculation runner component, which needs life-cycle management
		// a typical application might use dependency injection to obtain the instance
		try (CalculationRunner runner = CalculationRunner.ofMultiThreaded()) {
			calculate(runner);
		}
	}

	// -----------------------------------------------------------------------
	// create swap trades
	private static List<SwapTrade> createSwapTrades(ReferenceData refData) {
		return create2YTradedFR007IRS(refData);
	}

	private static List<SwapTrade> create2YTradedFR007IRS(ReferenceData refData) {
		LocalDate tradeDate1 = LocalDate.of(2024, 7, 2), startDate1 = FixedIborSwapConventions.CNY_REPO_1W_3M_A365F
				.calculateSpotDateFromTradeDate(tradeDate1, refData);
		SwapTrade trade1 = FixedIborSwapConventions.CNY_REPO_1W_3M_A365F.toTrade(
				TradeInfo.builder().tradeDate(tradeDate1).id(StandardId.of("Cfets", "FR007_20240702_9M")).settlementDate(startDate1)
						.addAttribute(AttributeType.DESCRIPTION, "Fixed vs FR007")
						.counterparty(StandardId.of("example", "A")).build(),
				startDate1, startDate1.plus(Tenor.TENOR_9M), BuySell.SELL, 400000000d, 0.0195);// the fixed interest
																								// rate

		LocalDate tradeDate2 = LocalDate.of(2024, 1, 3), startDate2 = FixedIborSwapConventions.CNY_REPO_1W_3M_A365F
				.calculateSpotDateFromTradeDate(tradeDate2, refData);
		SwapTrade trade2 = FixedIborSwapConventions.CNY_REPO_1W_3M_A365F.toTrade(
				TradeInfo.builder().tradeDate(tradeDate2).id(StandardId.of("Cfets", "FR007_20240103_1Y")).settlementDate(startDate2)
						.addAttribute(AttributeType.DESCRIPTION, "Fixed vs FR007")
						.counterparty(StandardId.of("example", "A")).build(),
				startDate2, startDate2.plus(Tenor.TENOR_1Y), BuySell.SELL, 400000000d, 0.0198);

		return ImmutableList.of(trade1, trade2);
	}

	private static void calculate(CalculationRunner runner) {
		Map<HolidayCalendarId, HolidayCalendar> calendars = new HashMap<>();
		HolidayCalendar cal = HolidayCalendars.of("CNBE");
		calendars.put(cal.getId(), cal);
		cal = HolidayCalendars.of("NoHolidays");
		calendars.put(cal.getId(), cal);
		ImmutableReferenceData refData = ImmutableReferenceData.of(calendars);

		List<SwapTrade> trades = createSwapTrades(refData);

		// the columns, specifying the measures to be calculated
		List<Column> columns = ImmutableList.of(Column.of(Measures.LEG_INITIAL_NOTIONAL),
				Column.of(Measures.PRESENT_VALUE), Column.of(Measures.LEG_PRESENT_VALUE),
				Column.of(Measures.PV01_CALIBRATED_SUM), Column.of(Measures.PAR_RATE),
				Column.of(Measures.ACCRUED_INTEREST), Column.of(Measures.PV01_CALIBRATED_BUCKETED),
				Column.of(AdvancedMeasures.PV01_SEMI_PARALLEL_GAMMA_BUCKETED));

		// load quotes
		ImmutableMap<QuoteId, Double> quotes = QuotesCsvLoader.load(VAL_DATE, QUOTES_RESOURCE);
		ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> fixings = FixingSeriesCsvLoader.load(FIXINGS_RESOURCE);

		// create the market data
		MarketData marketData = MarketData.of(VAL_DATE, quotes, fixings);

		List<ResolvedSwapTrade> resolvedSwapTrades = new ArrayList<>(2);

		for (SwapTrade trade : trades) {
			resolvedSwapTrades.add(trade.resolve(refData));
		}

		// load the curve definition
		Map<CurveGroupName, RatesCurveGroupDefinition> defns = RatesCalibrationCsvLoader.load(GROUPS_RESOURCE,
				SETTINGS_RESOURCE, CALIBRATION_RESOURCE);
		RatesCurveGroupDefinition curveGroupDefinition = defns.get(CURVE_GROUP_NAME).filtered(VAL_DATE, refData);

		// the configuration that defines how to create the curves when a curve group is
		// requested
		MarketDataConfig marketDataConfig = MarketDataConfig.builder().add(CURVE_GROUP_NAME, curveGroupDefinition)
				.build();

		// the complete set of rules for calculating measures
		CalculationFunctions functions = StandardComponents.calculationFunctions();
		RatesMarketDataLookup ratesLookup = RatesMarketDataLookup.of(curveGroupDefinition);
		CalculationRules rules = CalculationRules.of(functions, ratesLookup);

		// calibrate the curves and calculate the results
		MarketDataRequirements reqs = MarketDataRequirements.of(rules, trades, columns, refData);
		MarketDataFactory marketDataFactory = MarketDataFactory.of(ObservableDataProvider.none(),
				TimeSeriesProvider.none(), marketDataFunctions());
//        MarketData calibratedMarketData = marketDataFactory().create(reqs, marketDataConfig, marketData, refData);
		MarketData calibratedMarketData = marketDataFactory.create(reqs, marketDataConfig, marketData, refData);
		Results results = runner.calculate(rules, trades, columns, calibratedMarketData, refData);

		// use the report runner to transform the engine results into a trade report
		ReportCalculationResults calculationResults = ReportCalculationResults.of(VAL_DATE, trades, columns, results,
				functions, refData);
		TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("swap-report-template");
		TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
		tradeReport.writeAsciiTable(System.out);
	}

	public static List<MarketDataFunction<?, ?>> marketDataFunctions() {
		return ImmutableList.of(new CurveMarketDataFunction(), new CfetsRatesCurveGroupMarketDataFunction(),
				new RatesCurveInputsMarketDataFunction(), new FxRateMarketDataFunction(),
				new FxOptionVolatilitiesMarketDataFunction());
	}
}
