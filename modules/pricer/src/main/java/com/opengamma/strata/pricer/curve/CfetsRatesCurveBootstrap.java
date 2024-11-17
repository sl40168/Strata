package com.opengamma.strata.pricer.curve;

import static com.opengamma.strata.collect.Guavate.filtering;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.FloatingRateIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataFxRateProvider;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.math.impl.rootfinding.newton.NewtonDefaultVectorRootFinder;
import com.opengamma.strata.math.rootfind.NewtonVectorRootFinder;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.impl.swap.DispatchingSwapPaymentPeriodPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.deposit.ResolvedTermDeposit;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapTrade;

public final class CfetsRatesCurveBootstrap {

  public static CfetsRatesCurveBootstrap STANDARD = CfetsRatesCurveBootstrap.of(1e-10, 1e-10, 100);

  public static  CfetsRatesCurveBootstrap standard() {
    return STANDARD;
  }
	/**
	 * The root finder used for curve calibration.
	 */
	private final NewtonVectorRootFinder rootFinder;

	private final DispatchingSwapPaymentPeriodPricer swapPaymentPeriodPricer = DispatchingSwapPaymentPeriodPricer.DEFAULT;

	private CfetsRatesCurveBootstrap(NewtonVectorRootFinder rootFinder) {
		this.rootFinder = ArgChecker.notNull(rootFinder, "rootFinder");
	}

	public static CfetsRatesCurveBootstrap of(double toleranceAbs, double toleranceRel, int stepMaximum) {
		NewtonVectorRootFinder rootFinder = new NewtonDefaultVectorRootFinder(toleranceAbs, toleranceRel, stepMaximum);
		return of(rootFinder);
	}

	public static CfetsRatesCurveBootstrap of(NewtonVectorRootFinder rootFinder) {
		return new CfetsRatesCurveBootstrap(rootFinder);
	}

  public ImmutableRatesProvider bootstrap(
      RatesCurveGroupDefinition curveGroupDefn,
      MarketData marketData,
      ReferenceData refData) {

    Map<Index, LocalDateDoubleTimeSeries> timeSeries = marketData.getTimeSeriesIds().stream()
                                                           .flatMap(filtering(IndexQuoteId.class))
                                                           .collect(toImmutableMap(id -> id.getIndex(), id -> marketData.getTimeSeries(id)));
    ImmutableRatesProvider knownData = ImmutableRatesProvider.builder(marketData.getValuationDate())
                                           .fxRateProvider(MarketDataFxRateProvider.of(marketData))
                                           .timeSeries(timeSeries)
                                           .build();
    return bootstrap(ImmutableList.of(curveGroupDefn), knownData, marketData, refData);
  }

	public ImmutableRatesProvider bootstrap(List<RatesCurveGroupDefinition> allGroupDefns,
			ImmutableRatesProvider knownData, MarketData marketData, ReferenceData refData) {
		// this method effectively takes one CurveGroupDefinition
		// the list is a split of the definition, not multiple independent definitions

		if (!knownData.getValuationDate().equals(marketData.getValuationDate())) {
			throw new IllegalArgumentException(Messages.format("Valuation dates do not match: {} and {}",
					knownData.getValuationDate(), marketData.getValuationDate()));
		}
		Map<Index, Curve> indexCurves = Maps.newHashMap();
    Map<CurveName, Curve> builtCurves = Maps.newHashMap();
    Map<Currency, Curve> discountCurves = Maps.newHashMap();

		for (RatesCurveGroupDefinition groupDefn : allGroupDefns) {
			if (groupDefn.getEntries().isEmpty()) {
				continue;
			}
			for (CurveDefinition def : groupDefn.getCurveDefinitions()) {
				groupDefn.findEntry(def.getName()).ifPresent(entry -> {
					entry.getIndices().stream().filter(index -> index instanceof FloatingRateIndex)
							.map(index -> (FloatingRateIndex) index).forEach(index -> {
								bootstrap(def, knownData, marketData, refData, groupDefn.getName(), index)
										.ifPresent(curve -> {
                      indexCurves.put(index, curve);
                      builtCurves.put(curve.getName(), curve);
                    });
							});
				});
			}

      groupDefn.getEntries().forEach(entry -> {
        entry.getDiscountCurrencies().forEach(currency -> {
          groupDefn.findDiscountCurveName(currency).ifPresent(
              curveName -> {
                discountCurves.put(currency, builtCurves.get(curveName));
              }
          );
        });
      });
		}

		return ImmutableRatesProvider.builder(marketData.getValuationDate())
               .indexCurves(indexCurves)
               .discountCurves(discountCurves)
               .fxRateProvider(knownData.getFxRateProvider())
               .build();
	}

	private Optional<Curve> bootstrap(CurveDefinition def, ImmutableRatesProvider knownData, MarketData marketData,
			ReferenceData refData, CurveGroupName curveGroupName, FloatingRateIndex index) {
		double quantity = 1d;
		List<CfetsRateCurveBootstrapCashFlows> payments = extractedPayments(def, marketData, refData, quantity);
		return Optional.of(bootstrap(marketData.getValuationDate(), quantity, payments, CurveId.of(curveGroupName, def.getName()), index));
	}

	private InterpolatedNodalCurve bootstrap(LocalDate anchorDate, double quantity,
			List<CfetsRateCurveBootstrapCashFlows> payments, CurveId curveId, FloatingRateIndex index) {
		Collections.sort(payments);
		DayCount dayCount = index.getDayCount();

    DoubleArray xValues = DoubleArray.of(payments.size(), i -> {
      CfetsRateCurveBootstrapCashFlows payment = payments.get(i);
      return index.getDayCount().yearFraction(anchorDate, payment.getEndDate());
    }),
        yValues = DoubleArray.of(payments.size(), i -> {
      CfetsRateCurveBootstrapCashFlows payment = payments.get(i);
      return payment.getRate();
    });

		InterpolatedNodalCurve knownCurve = InterpolatedNodalCurve.builder().interpolator(CurveInterpolators.LINEAR)
				.extrapolatorLeft(CurveExtrapolators.FLAT).extrapolatorRight(CurveExtrapolators.FLAT)
				.metadata(DefaultCurveMetadata.builder().curveName(curveId.getCurveName())
						.xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.ZERO_RATE).dayCount(dayCount).build())
				.xValues(xValues).yValues(yValues).build();

		for (int i = 0; i < payments.size(); i++) {
			knownCurve = bootstrap(anchorDate, quantity, payments.get(i), knownCurve, index, i);
		}
		return knownCurve;
	}

	private InterpolatedNodalCurve bootstrap(LocalDate anchorDate,
      double quantity,
			CfetsRateCurveBootstrapCashFlows payment,
      InterpolatedNodalCurve knownCurve,
      FloatingRateIndex index,
			int nodeAt) {

    final double spotDateYearFraction = index.getDayCount().yearFraction(anchorDate, payment.getSpotDate()),
        endDateYearFraction = index.getDayCount().yearFraction(anchorDate, payment.getEndDate());

    DoubleArray root = rootFinder.findRoot(rates -> {
      double rate = rates.get(0);
      InterpolatedNodalCurve tempCurve = adjustNodeAt(knownCurve, endDateYearFraction, rate, nodeAt);

      DiscountFactors dfs = DiscountFactors.of(Currency.CNY, anchorDate, tempCurve);
      double spotDatePv = 0d;
      double spotDateDf = dfs.discountFactor(spotDateYearFraction);
      for (CfetsRateCurveBootstrapCashFlow cashFlow : payment.getPayments()) {
        spotDatePv += cashFlow.getPaymentAmount() * dfs.discountFactor(
            index.getDayCount().yearFraction(anchorDate, cashFlow.getPaymentDate())) / spotDateDf;
      }
      return DoubleArray.of(spotDatePv - quantity);
    }, DoubleArray.of(payment.getRate()));

    return adjustNodeAt(knownCurve, endDateYearFraction, root.get(0), nodeAt);
  }

	private InterpolatedNodalCurve adjustNodeAt(InterpolatedNodalCurve knownCurve, double xValue, double yValue, int nodeIndex) {
    DoubleArray knownXValues = knownCurve.getXValues(), knownYValues = knownCurve.getYValues();
    DoubleArray xValues = knownXValues.with(nodeIndex, xValue), yValues = knownYValues.with(nodeIndex, yValue);
    return InterpolatedNodalCurve.builder().interpolator(knownCurve.getInterpolator())
               .extrapolatorLeft(knownCurve.getExtrapolatorLeft()).extrapolatorRight(knownCurve.getExtrapolatorRight())
               .metadata(knownCurve.getMetadata()).xValues(xValues).yValues(yValues).build();
  }

  private List<CfetsRateCurveBootstrapCashFlows> extractedPayments(CurveDefinition def, MarketData marketData, ReferenceData refData, double quantity) {
    ImmutableList<Trade> trades =
        def.getNodes().stream().map(node -> node.trade(quantity, marketData, refData)).collect(toImmutableList());
    LocalDate anchorDate = marketData.getValuationDate();
    List<CfetsRateCurveBootstrapCashFlows> productCashFlows = Lists.newArrayList();

    for (Trade trade : trades) {
      if (trade.getClass().equals(SwapTrade.class)) {
        SwapTrade swapTrade = (SwapTrade) trade;
        ResolvedSwap swap = swapTrade.resolve(refData).getProduct();
        ImmutableList<ResolvedSwapLeg> legs = swap.getLegs(SwapLegType.FIXED);
        if (legs.size() != 1) {
          continue;
        }
        ResolvedSwapLeg fixedLeg = legs.get(0);
        LocalDate spotDate = fixedLeg.getStartDate();
        AtomicReference<Double> rate = new AtomicReference<>(0.02d);
        ImmutableList<CfetsRateCurveBootstrapCashFlow> payments = fixedLeg.getPaymentPeriods().stream().filter(period -> period instanceof  RatePaymentPeriod)
                                                                      .map(period -> (RatePaymentPeriod)period).map(period -> {
              double payment = swapPaymentPeriodPricer.forecastValue(period, null);
              LocalDate paymentDate = period.getPaymentDate();
              if (period.getEndDate().equals(fixedLeg.getEndDate())) {
                return CfetsRateCurveBootstrapCashFlow.builder().paymentAmount(payment + period.getNotional()).paymentDate(paymentDate).build();
              } else {
                return CfetsRateCurveBootstrapCashFlow.builder().paymentAmount(payment).paymentDate(paymentDate).build();
              }
        }).collect(toImmutableList());

        swapTrade.getProduct().getLeg(fixedLeg.getPayReceive()).ifPresent(leg -> {
          rate.set(
              ((FixedRateCalculation) ((RateCalculationSwapLeg) leg).getCalculation()).getRate().getInitialValue());
        });

        DayCount dayCount = ((RatePaymentPeriod) fixedLeg.getPaymentPeriods().get(0)).getDayCount();
        productCashFlows.add(CfetsRateCurveBootstrapCashFlows.builder().payments(payments).spotDate(spotDate).rate(rate.get()).build());
      } else if (trade.getClass().equals(TermDepositTrade.class)) {
        TermDepositTrade depositTrade = (TermDepositTrade)trade;
        ResolvedTermDeposit deposit = depositTrade.resolve(refData).getProduct();
        productCashFlows.add(
            CfetsRateCurveBootstrapCashFlows.builder().spotDate(deposit.getStartDate())
                .payments(ImmutableList.of(CfetsRateCurveBootstrapCashFlow.builder()
                                               .paymentAmount(deposit.getInterest() + deposit.getNotional())
                                               .paymentDate(deposit.getEndDate()).build())).rate(deposit.getRate()).build());

      } else {
        continue; //Other trades are not supported yet.
      }
    }
    return productCashFlows;
  }
}
