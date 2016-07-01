/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opengamma.strata.basics.schedule.SchedulePeriod;

/**
 * Test {@link ValueSchedule}.
 */
@Test
public class ValueScheduleTest {

  private static double TOLERANCE = 1.0E-10;
  private static ValueStep STEP1 = ValueStep.of(date(2014, 6, 30), ValueAdjustment.ofReplace(2000d));
  private static ValueStep STEP2 = ValueStep.of(date(2014, 7, 30), ValueAdjustment.ofReplace(3000d));

  private static SchedulePeriod PERIOD1 = SchedulePeriod.of(date(2014, 1, 1), date(2014, 2, 1));
  private static SchedulePeriod PERIOD2 = SchedulePeriod.of(date(2014, 2, 1), date(2014, 3, 1));
  private static SchedulePeriod PERIOD3 = SchedulePeriod.of(
      date(2014, 3, 1), date(2014, 4, 1), date(2014, 3, 2), date(2014, 4, 1));
  private static ImmutableList<SchedulePeriod> PERIODS = ImmutableList.of(PERIOD1, PERIOD2, PERIOD3);

  //-------------------------------------------------------------------------
  public void test_constant_ALWAYS_0() {
    ValueSchedule test = ValueSchedule.ALWAYS_0;
    assertEquals(test.getInitialValue(), 0d, TOLERANCE);
    assertEquals(test.getSteps(), ImmutableList.of());
  }

  public void test_constant_ALWAYS_1() {
    ValueSchedule test = ValueSchedule.ALWAYS_1;
    assertEquals(test.getInitialValue(), 1d, TOLERANCE);
    assertEquals(test.getSteps(), ImmutableList.of());
  }

  //-------------------------------------------------------------------------
  public void test_of_int() {
    ValueSchedule test = ValueSchedule.of(10000d);
    assertEquals(test.getInitialValue(), 10000d, TOLERANCE);
    assertEquals(test.getSteps(), ImmutableList.of());
  }

  public void test_of_intStepsArray() {
    ValueSchedule test = ValueSchedule.of(10000d, STEP1, STEP2);
    assertEquals(test.getInitialValue(), 10000d, TOLERANCE);
    assertEquals(test.getSteps(), ImmutableList.of(STEP1, STEP2));
  }

  public void test_of_intStepsArray_empty() {
    ValueSchedule test = ValueSchedule.of(10000d, new ValueStep[0]);
    assertEquals(test.getInitialValue(), 10000d, TOLERANCE);
    assertEquals(test.getSteps(), ImmutableList.of());
  }

  public void test_of_intStepsList() {
    ValueSchedule test = ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2));
    assertEquals(test.getInitialValue(), 10000d, TOLERANCE);
    assertEquals(test.getSteps(), ImmutableList.of(STEP1, STEP2));
  }

  public void test_of_intStepsList_empty() {
    ValueSchedule test = ValueSchedule.of(10000d, Lists.newArrayList());
    assertEquals(test.getInitialValue(), 10000d, TOLERANCE);
    assertEquals(test.getSteps(), ImmutableList.of());
  }

  public void test_builder_validEmpty() {
    ValueSchedule test = ValueSchedule.builder().build();
    assertEquals(test.getInitialValue(), 0d, TOLERANCE);
    assertEquals(test.getSteps(), ImmutableList.of());
  }

  public void test_builder_validFull() {
    ValueSchedule test = ValueSchedule.builder().initialValue(2000d).steps(ImmutableList.of(STEP1, STEP2)).build();
    assertEquals(test.getInitialValue(), 2000d, TOLERANCE);
    assertEquals(test.getSteps(), ImmutableList.of(STEP1, STEP2));
  }

  //-------------------------------------------------------------------------
  public void test_resolveValues_dateBased() {
    ValueStep step1 = ValueStep.of(date(2014, 2, 1), ValueAdjustment.ofReplace(300d));
    ValueStep step2 = ValueStep.of(date(2014, 3, 1), ValueAdjustment.ofReplace(400d));
    // no steps
    ValueSchedule test0 = ValueSchedule.of(200d, ImmutableList.of());
    assertEquals(test0.resolveValues(PERIODS), ImmutableList.of(200d, 200d, 200d));
    // step1
    ValueSchedule test1a = ValueSchedule.of(200d, ImmutableList.of(step1));
    assertEquals(test1a.resolveValues(PERIODS), ImmutableList.of(200d, 300d, 300d));
    // step2
    ValueSchedule test1b = ValueSchedule.of(200d, ImmutableList.of(step2));
    assertEquals(test1b.resolveValues(PERIODS), ImmutableList.of(200d, 200d, 400d));
    // step1 and step2
    ValueSchedule test2 = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertEquals(test2.resolveValues(PERIODS), ImmutableList.of(200d, 300d, 400d));
  }

  public void test_resolveValues_dateBased_matchAdjusted() {
    ValueStep step1 = ValueStep.of(date(2014, 2, 1), ValueAdjustment.ofReplace(300d));
    ValueStep step2 = ValueStep.of(date(2014, 3, 2), ValueAdjustment.ofReplace(400d));
    // no steps
    ValueSchedule test0 = ValueSchedule.of(200d, ImmutableList.of());
    assertEquals(test0.resolveValues(PERIODS), ImmutableList.of(200d, 200d, 200d));
    // step1
    ValueSchedule test1a = ValueSchedule.of(200d, ImmutableList.of(step1));
    assertEquals(test1a.resolveValues(PERIODS), ImmutableList.of(200d, 300d, 300d));
    // step2
    ValueSchedule test1b = ValueSchedule.of(200d, ImmutableList.of(step2));
    assertEquals(test1b.resolveValues(PERIODS), ImmutableList.of(200d, 200d, 400d));
    // step1 and step2
    ValueSchedule test2 = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertEquals(test2.resolveValues(PERIODS), ImmutableList.of(200d, 300d, 400d));
  }

  public void test_resolveValues_indexBased() {
    ValueStep step1 = ValueStep.of(1, ValueAdjustment.ofReplace(300d));
    ValueStep step2 = ValueStep.of(2, ValueAdjustment.ofReplace(400d));
    // no steps
    ValueSchedule test0 = ValueSchedule.of(200d, ImmutableList.of());
    assertEquals(test0.resolveValues(PERIODS), ImmutableList.of(200d, 200d, 200d));
    // step1
    ValueSchedule test1a = ValueSchedule.of(200d, ImmutableList.of(step1));
    assertEquals(test1a.resolveValues(PERIODS), ImmutableList.of(200d, 300d, 300d));
    // step2
    ValueSchedule test1b = ValueSchedule.of(200d, ImmutableList.of(step2));
    assertEquals(test1b.resolveValues(PERIODS), ImmutableList.of(200d, 200d, 400d));
    // step1 and step2
    ValueSchedule test2 = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertEquals(test2.resolveValues(PERIODS), ImmutableList.of(200d, 300d, 400d));
  }

  public void test_resolveValues_indexBased_duplicateDefinitionValid() {
    ValueStep step1 = ValueStep.of(1, ValueAdjustment.ofReplace(300d));
    ValueStep step2 = ValueStep.of(1, ValueAdjustment.ofReplace(300d));
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertEquals(test.resolveValues(PERIODS), ImmutableList.of(200d, 300d, 300d));
  }

  public void test_resolveValues_indexBased_duplicateDefinitionInvalid() {
    ValueStep step1 = ValueStep.of(1, ValueAdjustment.ofReplace(300d));
    ValueStep step2 = ValueStep.of(1, ValueAdjustment.ofReplace(400d));
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertThrowsIllegalArg(() -> test.resolveValues(PERIODS));
  }

  public void test_resolveValues_dateBased_indexZeroValid() {
    ValueStep step = ValueStep.of(date(2014, 1, 1), ValueAdjustment.ofReplace(300d));
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step));
    assertEquals(test.resolveValues(PERIODS), ImmutableList.of(300d, 300d, 300d));
  }

  public void test_resolveValues_indexBased_indexTooBig() {
    ValueStep step = ValueStep.of(3, ValueAdjustment.ofReplace(300d));
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step));
    assertThrowsIllegalArg(() -> test.resolveValues(PERIODS));
  }

  public void test_resolveValues_dateBased_dateInvalid() {
    ValueStep step = ValueStep.of(date(2014, 4, 1), ValueAdjustment.ofReplace(300d));
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step));
    assertThrowsIllegalArg(() -> test.resolveValues(PERIODS));
  }

  //-------------------------------------------------------------------------
  public void equals() {
    ValueSchedule a1 = ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2));
    ValueSchedule a2 = ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2));
    ValueSchedule b = ValueSchedule.of(5000d, Lists.newArrayList(STEP1, STEP2));
    ValueSchedule c = ValueSchedule.of(10000d, Lists.newArrayList(STEP1));
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2)));
  }

  public void test_serialization() {
    assertSerialization(ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2)));
  }

}
