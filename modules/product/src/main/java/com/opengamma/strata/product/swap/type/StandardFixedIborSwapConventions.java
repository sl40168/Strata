/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.currency.Currency.*;
import static com.opengamma.strata.basics.date.BusinessDayConventions.*;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.*;
import static com.opengamma.strata.basics.schedule.Frequency.*;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.ResetSchedule;

/**
 * Market standard Fixed-Ibor swap conventions.
 * <p>
 * https://quant.opengamma.io/Interest-Rate-Instruments-and-Market-Conventions.pdf
 */
final class StandardFixedIborSwapConventions {

  // GBLO+USNY calendar
  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);
  // GBLO+CHZU calendar
  private static final HolidayCalendarId GBLO_CHZU = GBLO.combinedWith(CHZU);
  // GBLO+JPTO calendar
  private static final HolidayCalendarId GBLO_JPTO = GBLO.combinedWith(JPTO);

  /**
   * USD(NY) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedIborSwapConvention USD_FIXED_6M_LIBOR_3M =
      ImmutableFixedIborSwapConvention.of(
          "USD-FIXED-6M-LIBOR-3M",
          FixedRateSwapLegConvention.of(USD, THIRTY_U_360, P6M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY)),
          IborRateSwapLegConvention.of(IborIndices.USD_LIBOR_3M));

  /**
   * USD(London) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays yearly with day count 'Act/360'.
   */
  public static final FixedIborSwapConvention USD_FIXED_1Y_LIBOR_3M =
      ImmutableFixedIborSwapConvention.of(
          "USD-FIXED-1Y-LIBOR-3M",
          FixedRateSwapLegConvention.of(USD, ACT_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY)),
          IborRateSwapLegConvention.of(IborIndices.USD_LIBOR_3M));

  //-------------------------------------------------------------------------
  /**
   * EUR(1Y) vanilla fixed vs Euribor 3M swap.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final FixedIborSwapConvention EUR_FIXED_1Y_EURIBOR_3M =
      ImmutableFixedIborSwapConvention.of(
          "EUR-FIXED-1Y-EURIBOR-3M",
          FixedRateSwapLegConvention.of(EUR, THIRTY_U_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)),
          IborRateSwapLegConvention.of(IborIndices.EUR_EURIBOR_3M));

  /**
   * EUR(>1Y) vanilla fixed vs Euribor 6M swap.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final FixedIborSwapConvention EUR_FIXED_1Y_EURIBOR_6M =
      ImmutableFixedIborSwapConvention.of(
          "EUR-FIXED-1Y-EURIBOR-6M",
          FixedRateSwapLegConvention.of(EUR, THIRTY_U_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)),
          IborRateSwapLegConvention.of(IborIndices.EUR_EURIBOR_6M));

  /**
   * EUR(1Y) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final FixedIborSwapConvention EUR_FIXED_1Y_LIBOR_3M =
      ImmutableFixedIborSwapConvention.of(
          "EUR-FIXED-1Y-LIBOR-3M",
          FixedRateSwapLegConvention.of(EUR, THIRTY_U_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)),
          IborRateSwapLegConvention.of(IborIndices.EUR_LIBOR_3M));

  /**
   * EUR(>1Y) vanilla fixed vs LIBOR 6M swap.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final FixedIborSwapConvention EUR_FIXED_1Y_LIBOR_6M =
      ImmutableFixedIborSwapConvention.of(
          "EUR-FIXED-1Y-LIBOR-6M",
          FixedRateSwapLegConvention.of(EUR, THIRTY_U_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)),
          IborRateSwapLegConvention.of(IborIndices.EUR_LIBOR_6M));

  //-------------------------------------------------------------------------
  /**
   * GBP(1Y) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays yearly with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention GBP_FIXED_1Y_LIBOR_3M =
      ImmutableFixedIborSwapConvention.of(
          "GBP-FIXED-1Y-LIBOR-3M",
          FixedRateSwapLegConvention.of(GBP, ACT_365F, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)),
          IborRateSwapLegConvention.of(IborIndices.GBP_LIBOR_3M));

  /**
   * GBP(>1Y) vanilla fixed vs LIBOR 6M swap.
   * The fixed leg pays every 6 months with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention GBP_FIXED_6M_LIBOR_6M =
      ImmutableFixedIborSwapConvention.of(
          "GBP-FIXED-6M-LIBOR-6M",
          FixedRateSwapLegConvention.of(GBP, ACT_365F, P6M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)),
          IborRateSwapLegConvention.of(IborIndices.GBP_LIBOR_6M));

  /**
   * GBP(>1Y) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays every 3 months with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention GBP_FIXED_3M_LIBOR_3M =
      ImmutableFixedIborSwapConvention.of(
          "GBP-FIXED-3M-LIBOR-3M",
          FixedRateSwapLegConvention.of(GBP, ACT_365F, P3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)),
          IborRateSwapLegConvention.of(IborIndices.GBP_LIBOR_3M));

  //-------------------------------------------------------------------------
  /**
   * CHF(1Y) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final FixedIborSwapConvention CHF_FIXED_1Y_LIBOR_3M =
      ImmutableFixedIborSwapConvention.of(
          "CHF-FIXED-1Y-LIBOR-3M",
          FixedRateSwapLegConvention.of(CHF, THIRTY_U_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_CHZU)),
          IborRateSwapLegConvention.of(IborIndices.CHF_LIBOR_3M));

  /**
   * CHF(>1Y) vanilla fixed vs LIBOR 6M swap.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final FixedIborSwapConvention CHF_FIXED_1Y_LIBOR_6M =
      ImmutableFixedIborSwapConvention.of(
          "CHF-FIXED-1Y-LIBOR-6M",
          FixedRateSwapLegConvention.of(CHF, THIRTY_U_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_CHZU)),
          IborRateSwapLegConvention.of(IborIndices.CHF_LIBOR_6M));

  //-------------------------------------------------------------------------
  /**
   * JPY(Tibor) vanilla fixed vs Tibor 3M swap.
   * The fixed leg pays every 6 months with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention JPY_FIXED_6M_TIBORJ_3M =
      ImmutableFixedIborSwapConvention.of(
          "JPY-FIXED-6M-TIBOR-JAPAN-3M",
          FixedRateSwapLegConvention.of(JPY, ACT_365F, P6M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO)),
          IborRateSwapLegConvention.of(IborIndices.JPY_TIBOR_JAPAN_3M));

  /**
   * JPY(LIBOR) vanilla fixed vs LIBOR 6M swap.
   * The fixed leg pays every 6 months with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention JPY_FIXED_6M_LIBOR_6M =
      ImmutableFixedIborSwapConvention.of(
          "JPY-FIXED-6M-LIBOR-6M",
          FixedRateSwapLegConvention.of(JPY, ACT_365F, P6M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_JPTO)),
          IborRateSwapLegConvention.of(IborIndices.JPY_LIBOR_6M));

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardFixedIborSwapConventions() {
  }

  public static final FixedIborSwapConvention CNY_REPO_1W_3M_A365F =
          ImmutableFixedIborSwapConvention.of("CNY_REPO_1W_3M_A365F",
                  FixedRateSwapLegConvention.of(CNY, ACT_365F, P3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CNBE)),
                  IborRateSwapLegConvention.builder().index(IborIndices.CNY_REPO_1W).stubConvention(StubConvention.SHORT_FINAL)
                          .paymentFrequency(P3M).accrualFrequency(P3M).compoundingMethod(CompoundingMethod.NONE).currency(CNY).dayCount(ACT_365F)
                          .resetPeriods(ResetSchedule.builder().resetFrequency(P1W).businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, CNBE)).build())
//                          .fixingDateOffset(DaysAdjustment.ofBusinessDays(-1, CNBE))
                          .build(),
                  DaysAdjustment.ofBusinessDays(1, CNBE, BusinessDayAdjustment.of(FOLLOWING, CNBE)));


  public static final FixedIborSwapConvention CNY_REPO_1W_1M_A365F =
          ImmutableFixedIborSwapConvention.of("CNY_REPO_1W_1M_A365F",
                  FixedRateSwapLegConvention.of(CNY, ACT_365F, P1M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CNBE)),
                  IborRateSwapLegConvention.builder().index(IborIndices.CNY_REPO_1W).stubConvention(StubConvention.SHORT_FINAL)
                          .paymentFrequency(P1M).accrualFrequency(P1M).compoundingMethod(CompoundingMethod.NONE).currency(CNY).dayCount(ACT_365F)
                          .resetPeriods(ResetSchedule.builder().resetFrequency(P1W).businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, CNBE)).build())
//                          .fixingDateOffset(DaysAdjustment.ofBusinessDays(-1, CNBE))
                          .build(),
                  DaysAdjustment.ofBusinessDays(1, CNBE, BusinessDayAdjustment.of(FOLLOWING, CNBE)));
}
