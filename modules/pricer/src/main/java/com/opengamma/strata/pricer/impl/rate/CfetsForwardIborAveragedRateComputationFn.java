package com.opengamma.strata.pricer.impl.rate;

import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.IborAveragedFixing;
import com.opengamma.strata.product.rate.IborAveragedRateComputation;

import java.time.LocalDate;

/**
 * Rate computation implementation for  Cfets convention, in which a rate is based on the compounding of multiple fixings of a
 * single Ibor floating rate index.
 * <p>
 * The rate computation queries the rates from the {@code RatesProvider} and weighted-average them.
 * There is no convexity adjustment computed in this implementation.
 */

public class CfetsForwardIborAveragedRateComputationFn
    implements RateComputationFn<IborAveragedRateComputation> {

  /**
   * Default instance.
   */
  public static final CfetsForwardIborAveragedRateComputationFn DEFAULT = new CfetsForwardIborAveragedRateComputationFn();
  @Override
  public double rate(IborAveragedRateComputation computation, LocalDate startDate, LocalDate endDate,
      RatesProvider provider) {
    IborIndexRates rates = provider.iborIndexRates(computation.getIndex());

    // take (rate * weight) for each fixing and divide by total weight
    double fv = computation.getFixings().stream()
                              .mapToDouble(fixing -> futureValue(fixing, rates))
                              .reduce(1, (left, right) -> left * right);
    return (fv - 1) / computation.getTotalWeight();
  }

  // Compute the rate adjusted by the weight for one IborAverageFixing.
  private double futureValue(IborAveragedFixing fixing, IborIndexRates rates) {
    double rate = fixing.getFixedRate().orElse(rates.rate(fixing.getObservation()));
    return 1 + rate * fixing.getWeight();
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(IborAveragedRateComputation computation, LocalDate startDate,
      LocalDate endDate, RatesProvider provider) {
    IborIndexRates rates = provider.iborIndexRates(computation.getIndex());

    // combine the weighted sensitivity to each fixing
    // omit fixed rates as they have no sensitivity to a curve
    return computation.getFixings().stream()
               .filter(fixing -> !fixing.getFixedRate().isPresent())
               .map(fixing -> weightedSensitivity(fixing, computation.getTotalWeight(), rates))
               .reduce(PointSensitivityBuilder.none(), PointSensitivityBuilder::combinedWith);
  }

  // Compute the weighted sensitivity for one IborAverageFixing.
  private PointSensitivityBuilder weightedSensitivity(
      IborAveragedFixing fixing,
      double totalWeight,
      IborIndexRates rates) {

    return rates.ratePointSensitivity(fixing.getObservation())
               .multipliedBy(fixing.getWeight() / totalWeight);
  }

  @Override
  public double explainRate(IborAveragedRateComputation computation, LocalDate startDate, LocalDate endDate,
      RatesProvider provider, ExplainMapBuilder builder) {
    IborIndexRates rates = provider.iborIndexRates(computation.getIndex());
    for (IborAveragedFixing fixing : computation.getFixings()) {
      rates.explainRate(fixing.getObservation(), builder, child -> child.put(ExplainKey.WEIGHT, fixing.getWeight()));
    }
    double rate = rate(computation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }
}
