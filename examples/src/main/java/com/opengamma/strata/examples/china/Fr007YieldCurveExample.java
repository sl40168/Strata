package com.opengamma.strata.examples.china;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceDataId;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
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
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.opengamma.strata.measure.StandardComponents.marketDataFactory;

public class Fr007YieldCurveExample {

    private static final LocalDate VAL_DATE = LocalDate.of(2024, 6, 5);

    private static final ResourceLocator QUOTES_RESOURCE = ResourceLocator.ofClasspath("example-china/quotes/quotes.csv");
    /**
     * The location of the curve calibration groups file.
     */
    private static final ResourceLocator GROUPS_RESOURCE = ResourceLocator.ofClasspath("example-china/curves/groups.csv");
    /**
     * The location of the curve calibration settings file.
     */
    private static final ResourceLocator SETTINGS_RESOURCE = ResourceLocator.ofClasspath("example-china/curves/settings.csv");
    /**
     * The location of the curve calibration nodes file.
     */
    private static final ResourceLocator CALIBRATION_RESOURCE = ResourceLocator.ofClasspath("example-china/curves/calibrations.csv");
    /**
     * The location of the historical fixing file.
     */
    private static final ResourceLocator FIXINGS_RESOURCE = ResourceLocator.ofClasspath("example-china/fixings/fr007.csv");

    /**
     * The curve group name.
     */
    private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("CNY-CFETS-FR007");

    /**
     * Runs the example, pricing the instruments, producing the output as an ASCII table.
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

    //-----------------------------------------------------------------------
    // create swap trades
    private static List<SwapTrade> createSwapTrades() {
        return ImmutableList.of(create2YTradedFR007IRS());
    }

    private static SwapTrade create2YTradedFR007IRS() {
        TradeInfo tradeInfo =
                TradeInfo.builder().id(StandardId.of("example", "1")).addAttribute(AttributeType.DESCRIPTION, "Fixed vs FR007").counterparty(StandardId.of(
                        "example", "A")).settlementDate(LocalDate.of(2024, 2, 26)).build();
        return FixedIborSwapConventions.CNY_REPO_1W_1M_A365F.toTrade(tradeInfo, LocalDate.of(2024, 2, 26), // the start date
                LocalDate.of(2026, 2, 26), // the end date
                BuySell.BUY,               // indicates wheter this trade is a buy or sell
                100_000_000,               // the notional amount
                0.021);                    // the fixed interest rate
    }

    private static void calculate(CalculationRunner runner) {
        List<SwapTrade> trades = createSwapTrades();

        // the columns, specifying the measures to be calculated
        List<Column> columns = ImmutableList.of(Column.of(Measures.LEG_INITIAL_NOTIONAL), Column.of(Measures.PRESENT_VALUE),
                Column.of(Measures.LEG_PRESENT_VALUE), Column.of(Measures.PV01_CALIBRATED_SUM), Column.of(Measures.PAR_RATE),
                Column.of(Measures.ACCRUED_INTEREST), Column.of(Measures.PV01_CALIBRATED_BUCKETED),
                Column.of(AdvancedMeasures.PV01_SEMI_PARALLEL_GAMMA_BUCKETED));

        // load quotes
        ImmutableMap<QuoteId, Double> quotes = QuotesCsvLoader.load(VAL_DATE, QUOTES_RESOURCE);
        ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> fixings = FixingSeriesCsvLoader.load(FIXINGS_RESOURCE);

        // create the market data
        MarketData marketData = MarketData.of(VAL_DATE, quotes, fixings);

        Map<HolidayCalendarId, HolidayCalendar> calendars = new HashMap<>();
        HolidayCalendar cal = HolidayCalendars.of("CNBE");
        calendars.put(cal.getId(), cal);
        cal = HolidayCalendars.of("NoHolidays");
        calendars.put(cal.getId(), cal);
        ImmutableReferenceData refData = ImmutableReferenceData.of(calendars);

        for (SwapTrade trade : trades) {
            trade.resolve(refData);
        }

        // load the curve definition
        Map<CurveGroupName, RatesCurveGroupDefinition> defns = RatesCalibrationCsvLoader.load(GROUPS_RESOURCE, SETTINGS_RESOURCE, CALIBRATION_RESOURCE);
        RatesCurveGroupDefinition curveGroupDefinition = defns.get(CURVE_GROUP_NAME).filtered(VAL_DATE, refData);

        // the configuration that defines how to create the curves when a curve group is requested
        MarketDataConfig marketDataConfig = MarketDataConfig.builder().add(CURVE_GROUP_NAME, curveGroupDefinition).build();

        // the complete set of rules for calculating measures
        CalculationFunctions functions = StandardComponents.calculationFunctions();
        RatesMarketDataLookup ratesLookup = RatesMarketDataLookup.of(curveGroupDefinition);
        CalculationRules rules = CalculationRules.of(functions, ratesLookup);

        // calibrate the curves and calculate the results
        MarketDataRequirements reqs = MarketDataRequirements.of(rules, trades, columns, refData);
        MarketData calibratedMarketData = marketDataFactory().create(reqs, marketDataConfig, marketData, refData);

        Results results = runner.calculate(rules, trades, columns, calibratedMarketData, refData);

        // use the report runner to transform the engine results into a trade report
        ReportCalculationResults calculationResults = ReportCalculationResults.of(VAL_DATE, trades, columns, results, functions, refData);
        TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("swap-report-template");
        TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
        tradeReport.writeAsciiTable(System.out);
    }

}
