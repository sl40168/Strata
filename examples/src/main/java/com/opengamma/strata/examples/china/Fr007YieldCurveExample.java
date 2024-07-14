package com.opengamma.strata.examples.china;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.loader.csv.FixingSeriesCsvLoader;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.observable.QuoteId;

import java.time.LocalDate;
import java.util.Map;

public class Fr007YieldCurveExample {

    private static final LocalDate VAL_DATE = LocalDate.of(2024, 6, 5);

    private static final ResourceLocator QUOTES_RESOURCE = ResourceLocator.ofClasspath("example-china/quotes/quotes.csv");
    /**
     * The location of the curve calibration groups file.
     */
    private static final ResourceLocator GROUPS_RESOURCE =
            ResourceLocator.ofClasspath("example-china/curves/groups.csv");
    /**
     * The location of the curve calibration settings file.
     */
    private static final ResourceLocator SETTINGS_RESOURCE =
            ResourceLocator.ofClasspath("example-china/curves/settings.csv");
    /**
     * The location of the curve calibration nodes file.
     */
    private static final ResourceLocator CALIBRATION_RESOURCE =
            ResourceLocator.ofClasspath("example-china/curves/calibrations.csv");
    /**
     * The location of the historical fixing file.
     */
    private static final ResourceLocator FIXINGS_RESOURCE =
            ResourceLocator.ofClasspath("example-china/fixings/fr007.csv");

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

    private static void calculate(CalculationRunner runner) {
// load quotes
        ImmutableMap<QuoteId, Double> quotes = QuotesCsvLoader.load(VAL_DATE, QUOTES_RESOURCE);
        ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> fixings = FixingSeriesCsvLoader.load(FIXINGS_RESOURCE);

        // load the curve definition
        Map<CurveGroupName, RatesCurveGroupDefinition> defns =
                RatesCalibrationCsvLoader.load(GROUPS_RESOURCE, SETTINGS_RESOURCE, CALIBRATION_RESOURCE);
    }

}
