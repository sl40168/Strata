/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fx;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.scenario.MultiCurrencyValuesArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.data.scenario.ValuesArray;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.fx.DiscountingFxSingleTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fx.ResolvedFxSingleTrade;

/**
 * Test {@link FxSingleTradeCalculationFunction}.
 */
@Test
public class FxSingleTradeCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount USD_M1600 = CurrencyAmount.of(USD, -1_600);
  private static final FxSingle PRODUCT = FxSingle.of(GBP_P1000, USD_M1600, date(2015, 6, 30));
  public static final FxSingleTrade TRADE = FxSingleTrade.builder().product(PRODUCT).build();
  public static final ResolvedFxSingleTrade RTRADE = TRADE.resolve(REF_DATA);
  private static final CurveId DISCOUNT_CURVE_GBP_ID = CurveId.of("Default", "Discount-GBP");
  private static final CurveId DISCOUNT_CURVE_USD_ID = CurveId.of("Default", "Discount-USD");
  static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP_ID, USD, DISCOUNT_CURVE_USD_ID),
      ImmutableMap.of());
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP);
  private static final LocalDate VAL_DATE = TRADE.getProduct().getPaymentDate().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    FxSingleTradeCalculationFunction function = new FxSingleTradeCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsExactly(GBP, USD);
    assertThat(reqs.getValueRequirements()).isEqualTo(
        ImmutableSet.of(DISCOUNT_CURVE_GBP_ID, DISCOUNT_CURVE_USD_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEmpty();
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(GBP);
  }

  public void test_simpleMeasures() {
    FxSingleTradeCalculationFunction function = new FxSingleTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    DiscountingFxSingleTradePricer pricer = DiscountingFxSingleTradePricer.DEFAULT;
    MultiCurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider);
    double expectedParSpread = pricer.parSpread(RTRADE, provider);
    MultiCurrencyAmount expectedCurrencyExp = pricer.currencyExposure(RTRADE, provider);
    MultiCurrencyAmount expectedCash = pricer.currentCash(RTRADE, provider);
    FxRate expectedForwardFx = pricer.forwardFxRate(RTRADE, provider);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE,
        Measures.PAR_SPREAD,
        Measures.CURRENCY_EXPOSURE,
        Measures.CURRENT_CASH,
        Measures.FORWARD_FX_RATE);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PAR_SPREAD, Result.success(ValuesArray.of(ImmutableList.of(expectedParSpread))))
        .containsEntry(
            Measures.CURRENCY_EXPOSURE, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedCurrencyExp))))
        .containsEntry(
            Measures.CURRENT_CASH, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedCash))))
        .containsEntry(
            Measures.FORWARD_FX_RATE, Result.success(ScenarioArray.of(ImmutableList.of(expectedForwardFx))));
  }

  public void test_pv01() {
    FxSingleTradeCalculationFunction function = new FxSingleTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    DiscountingFxSingleTradePricer pricer = DiscountingFxSingleTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01 = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedBucketedPv01 = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PV01_CALIBRATED_SUM,
        Measures.PV01_CALIBRATED_BUCKETED);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PV01_CALIBRATED_SUM, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv01))))
        .containsEntry(
            Measures.PV01_CALIBRATED_BUCKETED, Result.success(ScenarioArray.of(ImmutableList.of(expectedBucketedPv01))));
  }

  //-------------------------------------------------------------------------
  static ScenarioMarketData marketData() {
    Curve curve1 = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.992);
    Curve curve2 = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.991);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(
            DISCOUNT_CURVE_GBP_ID, curve1,
            DISCOUNT_CURVE_USD_ID, curve2,
            FxRateId.of(GBP, USD), FxRate.of(GBP, USD, 1.62)),
        ImmutableMap.of());
    return md;
  }

}
