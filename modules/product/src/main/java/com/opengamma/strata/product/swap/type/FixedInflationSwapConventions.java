/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import java.util.Optional;

import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.PropertySet;
import com.opengamma.strata.collect.io.ResourceConfig;
import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Fixed-Inflation swap conventions.
 */
public final class FixedInflationSwapConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<FixedInflationSwapConvention> ENUM_LOOKUP = ExtendedEnum.of(FixedInflationSwapConvention.class);
  /**
   * The default conventions.
   */
  static final PropertySet DEFAULTS;
  static {
    // do not parse here, to avoid initialization loops
    String name = FixedInflationSwapConvention.class.getSimpleName() + ".ini";
    IniFile config = ResourceConfig.combinedIniFile(name);
    DEFAULTS = config.section("defaultByPriceIndex");
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a convention based on the specified index.
   * <p>
   * This uses a lookup to find the matching convention.
   * 
   * @param index  the index, from which the index name is used to find the matching convention
   * @return the convention, empty if no convention is configured for the index
   */
  public static Optional<FixedInflationSwapConvention> findByIndex(PriceIndex index) {
    return DEFAULTS.findValue(index.getName()).map(FixedInflationSwapConvention::of);
  }

  //-------------------------------------------------------------------------
  /**
   * GBP vanilla fixed vs UK HCIP swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention GBP_FIXED_ZC_GB_HCIP =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.GBP_FIXED_ZC_GB_HCIP.getName());

  /**
   * GBP vanilla fixed vs UK RPI swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention GBP_FIXED_ZC_GB_RPI =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.GBP_FIXED_ZC_GB_RPI.getName());

  /**
   * GBP vanilla fixed vs UK RPIX swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention GBP_FIXED_ZC_GB_RPIX =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.GBP_FIXED_ZC_GB_RPIX.getName());

  /**
   * CHF vanilla fixed vs Switzerland CPI swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention CHF_FIXED_ZC_CH_CPI =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.CHF_FIXED_ZC_CH_CPI.getName());

  /**
   * Euro vanilla fixed vs Europe CPI swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention EUR_FIXED_ZC_EU_AI_CPI =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.EUR_FIXED_ZC_EU_AI_CPI.getName());

  /**
   * Euro vanilla fixed vs Europe (Excluding Tobacco) CPI swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention EUR_FIXED_ZC_EU_EXT_CPI =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.EUR_FIXED_ZC_EU_EXT_CPI.getName());

  /**
   * JPY vanilla fixed vs Japan (Excluding Fresh Food) CPI swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention JPY_FIXED_ZC_JP_CPI =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.JPY_FIXED_ZC_JP_CPI.getName());

  /**
   * USD(NY) vanilla fixed vs US Urban consumers CPI swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention USD_FIXED_ZC_US_CPI =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.USD_FIXED_ZC_US_CPI.getName());

  /**
   * Euro vanilla fixed vs France CPI swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention EUR_FIXED_ZC_FR_CPI =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.EUR_FIXED_ZC_FR_CPI.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FixedInflationSwapConventions() {
  }

}
