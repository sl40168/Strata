/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import java.time.LocalDate;
import java.time.Period;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * A market convention for Overnight-Overnight swap trades.
 * <p>
 * This defines the market convention for an Overnight-Overnight single currency swap.
 * The convention is formed by combining two swap leg conventions in the same currency.
 * <p>
 * To manually create a convention, see {@link ImmutableOvernightOvernightSwapConvention}.
 * To register a specific convention, see {@code OvernightOvernightSwapConvention.ini}.
 */
public interface OvernightOvernightSwapConvention extends SingleCurrencySwapConvention, Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static OvernightOvernightSwapConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the convention to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<OvernightOvernightSwapConvention> extendedEnum() {
    return OvernightOvernightSwapConventions.ENUM_LOOKUP;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the spread leg.
   * 
   * @return the spread leg convention
   */
  public abstract OvernightRateSwapLegConvention getSpreadLeg();

  /**
   * Gets the market convention of the flat leg.
   * 
   * @return the flat leg convention
   */
  public abstract OvernightRateSwapLegConvention getFlatLeg();

  //-------------------------------------------------------------------------
  /**
   * Creates a spot-starting trade based on this convention.
   * <p>
   * This returns a trade based on the specified tenor. For example, a tenor
   * of 5 years creates a swap starting on the spot date and maturing 5 years later.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the rate of the flat leg is received from the counterparty,
   * with the rate of the spread leg being paid. If selling the swap, the opposite occurs.
   *
   * @param tradeDate  the date of the trade
   * @param tenor  the tenor of the swap
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param spread  the spread, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  @Override
  public default SwapTrade createTrade(
      LocalDate tradeDate,
      Tenor tenor,
      BuySell buySell,
      double notional,
      double spread,
      ReferenceData refData) {

    // override for Javadoc
    return SingleCurrencySwapConvention.super.createTrade(tradeDate, tenor, buySell, notional, spread, refData);
  }

  /**
   * Creates a forward-starting trade based on this convention.
   * <p>
   * This returns a trade based on the specified period and tenor. For example, a period of
   * 3 months and a tenor of 5 years creates a swap starting three months after the spot date
   * and maturing 5 years later.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the rate of the flat leg is received from the counterparty,
   * with the rate of the spread leg being paid. If selling the swap, the opposite occurs.
   *
   * @param tradeDate  the date of the trade
   * @param periodToStart  the period between the spot date and the start date
   * @param tenor  the tenor of the swap
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param spread  the spread, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  @Override
  public default SwapTrade createTrade(
      LocalDate tradeDate,
      Period periodToStart,
      Tenor tenor,
      BuySell buySell,
      double notional,
      double spread,
      ReferenceData refData) {

    // override for Javadoc
    return SingleCurrencySwapConvention.super.createTrade(tradeDate, periodToStart, tenor, buySell, notional, spread, refData);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the rate of the flat leg is received from the counterparty,
   * with the rate of the spread leg being paid. If selling the swap, the opposite occurs.
   *
   * @param tradeDate  the date of the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param spread  the spread, typically derived from the market
   * @return the trade
   */
  @Override
  public default SwapTrade toTrade(
      LocalDate tradeDate,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double spread) {

    // override for Javadoc
    return SingleCurrencySwapConvention.super.toTrade(tradeDate, startDate, endDate, buySell, notional, spread);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the rate of the flat leg is received from the counterparty,
   * with the rate of the spread leg being paid. If selling the swap, the opposite occurs.
   *
   * @param tradeInfo  additional information about the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param spread  the spread, typically derived from the market
   * @return the trade
   */
  @Override
  public abstract SwapTrade toTrade(
      TradeInfo tradeInfo,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double spread);

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this convention.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
