package com.opengamma.strata.examples.china;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.*;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.*;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.*;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.math.impl.linearalgebra.SVDecompositionCommons;
import com.opengamma.strata.math.impl.rootfinding.newton.BaseNewtonVectorRootFinder;
import com.opengamma.strata.math.impl.rootfinding.newton.BroydenVectorRootFinder;
import com.opengamma.strata.math.impl.rootfinding.newton.NewtonDefaultVectorRootFinder;
import com.opengamma.strata.pricer.DiscountFactors;

public class CfetsBootStrapResolver {
    static final double EPS = 1e-6;
    static final double TOLERANCE = 1e-10;
    static final int MAXSTEPS = 100;
    static final HolidayCalendarId HOLIDAY_CALENDAR_ID = HolidayCalendarIds.CNBE;

    private final BaseNewtonVectorRootFinder rootFinder = new NewtonDefaultVectorRootFinder(TOLERANCE, TOLERANCE,
            MAXSTEPS, new SVDecompositionCommons());
//    private final BaseNewtonVectorRootFinder rootFinder = new BroydenVectorRootFinder(TOLERANCE, TOLERANCE, MAXSTEPS);
    private final ReferenceData referenceData;

    public CfetsBootStrapResolver(ReferenceData referenceData) {
        this.referenceData = referenceData;
    }

    public Curve resolve(LocalDate anchor, List<Node> nodes, DayCount dayCount, HolidayCalendarId calendarId, CurveId curveId) {
        List<Node> curveNodes = new ArrayList<>(nodes);
        Collections.sort(curveNodes);
        InterpolatedNodalCurve knownCurve =
                InterpolatedNodalCurve.builder().interpolator(CurveInterpolators.LINEAR).extrapolatorLeft(CurveExtrapolators.FLAT).extrapolatorRight(CurveExtrapolators.FLAT)
                        .metadata(DefaultCurveMetadata.builder().curveName(curveId.getCurveName()).xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.ZERO_RATE).dayCount(dayCount).build())
                        .xValues(DoubleArray.of(1, 2)).yValues(DoubleArray.of(0.05, 0.05)).build();
        for (int i = 0; i < curveNodes.size(); i++) {
            Node node = curveNodes.get(i);
            knownCurve = this.bootstrap(anchor, knownCurve, node, calendarId, referenceData, i);
        }
        return knownCurve;
    }

    static InterpolatedNodalCurve appendNode(InterpolatedNodalCurve knownCurve, double xValue, double yValue) {
        DoubleArray x = DoubleArray.copyOf(knownCurve.getXValues().toArray()).concat(xValue);
        DoubleArray y = DoubleArray.copyOf(knownCurve.getYValues().toArray()).concat(yValue);
        return InterpolatedNodalCurve.builder().interpolator(knownCurve.getInterpolator())
                .extrapolatorLeft(knownCurve.getExtrapolatorLeft()).extrapolatorRight(knownCurve.getExtrapolatorRight())
                .metadata(knownCurve.getMetadata()).xValues(x).yValues(y).build();
    }

    static InterpolatedNodalCurve appendNodeAt(InterpolatedNodalCurve knownCurve, double xValue, double yValue, int currentIndex) {
        InterpolatedNodalCurve target = null;
        if (currentIndex == 0) {
            target =
                    InterpolatedNodalCurve.builder().interpolator(knownCurve.getInterpolator())
                            .extrapolatorLeft(knownCurve.getExtrapolatorLeft()).extrapolatorRight(knownCurve.getExtrapolatorRight())
                            .metadata(knownCurve.getMetadata()).xValues(DoubleArray.of(xValue, xValue + 1)).yValues(DoubleArray.of(yValue, yValue)).build();
        } else if (currentIndex == 1) {
            target =
                    InterpolatedNodalCurve.builder().interpolator(knownCurve.getInterpolator())
                            .extrapolatorLeft(knownCurve.getExtrapolatorLeft()).extrapolatorRight(knownCurve.getExtrapolatorRight())
                            .metadata(knownCurve.getMetadata()).xValues(DoubleArray.of(knownCurve.getXValues().get(0),
                                    xValue)).yValues(DoubleArray.of(knownCurve.getYValues().get(0), yValue)).build();
        } else {
            target = appendNode(knownCurve, xValue, yValue);
        }
        return target;
    }

    private InterpolatedNodalCurve bootstrap(LocalDate anchor, InterpolatedNodalCurve knownCurve, Node node, HolidayCalendarId calendarId,
                                             ReferenceData refData, int currentIndex) {
        PeriodAdjustment spotPeriodAdjustment =
                PeriodAdjustment.builder().adjustment(BusinessDayAdjustment.builder().calendar(calendarId).convention(BusinessDayConventions.FOLLOWING).build()).additionConvention(PeriodAdditionConventions.NONE)
                        .period(node.getSpotPeriod()).build();
        PeriodAdjustment.Builder paymentPeriodAdjustmentBuilder =
                PeriodAdjustment.builder().adjustment(BusinessDayAdjustment.builder().calendar(calendarId).convention(node.getBdc()).build())
                        .additionConvention(node.getSpotToMature().toTotalMonths() > 0 ? PeriodAdditionConventions.LAST_BUSINESS_DAY : PeriodAdditionConventions.NONE);

        LocalDate spotDate = spotPeriodAdjustment.adjust(anchor, refData);
        LocalDate matureDate = paymentPeriodAdjustmentBuilder.period(node.getSpotToMature()).build().adjust(spotDate, refData);
        DayCount dayCount = knownCurve.getMetadata().getInfo(CurveInfoType.DAY_COUNT);
        double spotDateYearFraction = dayCount.yearFraction(anchor, spotDate), matureYearFraction = dayCount.yearFraction(anchor, matureDate);

        List<CashFlow> cashFlows = new LinkedList<>();
        if (node.getFrequency().getPeriod().equals(node.getSpotToMature())) { // Only one payment
            double  accrualYearFraction = dayCount.yearFraction(spotDate, matureDate);
            double payment = 1 + node.getRate() * accrualYearFraction;
            cashFlows.add(new CashFlow(matureYearFraction, payment));
        } else { // Multi payments
            Period leftPeriod = Period.from(node.spotToMature).minus(node.getFrequency()).normalized();
            LocalDate accrualStartDate = spotDate, accrualEndDate = accrualStartDate;
            double  accrualYearFraction = 0, paymentDateYearFraction = 0;
            while (!(leftPeriod.isNegative() || leftPeriod.isZero())) {
                accrualEndDate = paymentPeriodAdjustmentBuilder.period(node.getFrequency().getPeriod()).build().adjust(accrualStartDate, refData);
                accrualYearFraction = dayCount.yearFraction(accrualStartDate, accrualEndDate);
                paymentDateYearFraction = dayCount.yearFraction(anchor, accrualEndDate);
                cashFlows.add(new CashFlow(paymentDateYearFraction, accrualYearFraction * node.getRate()));
                accrualStartDate = accrualEndDate;
                leftPeriod = leftPeriod.minus(node.getFrequency()).normalized();
            }
            accrualEndDate = matureDate;
            accrualYearFraction = dayCount.yearFraction(accrualStartDate, accrualEndDate);
            paymentDateYearFraction = dayCount.yearFraction(anchor, accrualEndDate);
            cashFlows.add(new CashFlow(paymentDateYearFraction, 1 + accrualYearFraction * node.getRate()));
        }

        CalibrationFun calibrationFun = new CalibrationFun(anchor, knownCurve, new CashFlows(spotDateYearFraction, cashFlows), matureYearFraction, currentIndex);
        DoubleArray root = rootFinder.findRoot(calibrationFun, DoubleArray.of(node.getRate()));
        return appendNodeAt(knownCurve, matureYearFraction, root.get(0), currentIndex);
    }

    public static class CalibrationFun implements Function<DoubleArray, DoubleArray> {

        private final InterpolatedNodalCurve knownCurve;
        private final CashFlows payments;
        private final double targetPointYearFraction;
        private final int currentIndex;
        private final LocalDate anchor;

        public CalibrationFun(LocalDate anchor, InterpolatedNodalCurve knownCurve, CashFlows payments, double targetPointYearFraction, int currentIndex) {
            this.anchor = anchor;
            this.knownCurve = knownCurve;
            this.payments = payments;
            this.targetPointYearFraction = targetPointYearFraction;
            this.currentIndex = currentIndex;
        }

        @Override
        public DoubleArray apply(DoubleArray guess) {
            InterpolatedNodalCurve target = appendNodeAt(this.knownCurve, this.targetPointYearFraction, guess.get(0), this.currentIndex);
            DiscountFactors dfs = DiscountFactors.of(Currency.CNY, this.anchor, target);
            double spotDatePv = 0.0, spotDateDf = dfs.discountFactor(this.payments.spotDateYearFraction);

            for (CashFlow cf : this.payments.getCashFlows()) {
                spotDatePv += cf.getAmount() * dfs.discountFactor(cf.getPaymentDateYearFraction()) / spotDateDf;
            }
            return DoubleArray.of(spotDatePv - 1);
        }


    }

    static class CashFlow implements Comparable<CashFlow> {

        private final double paymentDateYearFraction;
        private final double amount;

        CashFlow(double paymentDateYearFraction, double amount) {
            this.paymentDateYearFraction = paymentDateYearFraction;
            this.amount = amount;
        }

        public double getAmount() {
            return amount;
        }

        public double getPaymentDateYearFraction() {
            return paymentDateYearFraction;
        }

        @Override
        public int compareTo(CashFlow o) {
            return (int) ((this.getPaymentDateYearFraction() - o.getPaymentDateYearFraction()) * 100);
        }
    }

    public static class CashFlows {
        private final double spotDateYearFraction;
        private final List<CashFlow> cashFlows;

        CashFlows(double spotDateYearFraction, List<CashFlow> cashFlows) {
            this.spotDateYearFraction = spotDateYearFraction;
            this.cashFlows = (null == cashFlows || cashFlows.isEmpty()) ? new ArrayList<>(0)
                    : new ArrayList<>(cashFlows);
            Collections.sort(this.cashFlows);
        }

        public List<CashFlow> getCashFlows() {
            return ImmutableList.copyOf(this.cashFlows);
        }

        public double getSpotDateYearFraction() {
            return spotDateYearFraction;
        }
    }

    public static class Node implements  Comparable<Node> {

        private final Period spotPeriod;
        private final Period spotToMature;
        private final BusinessDayConvention bdc;
        private final Frequency frequency;
        private final double rate;

        public Node(Period spotPeriod, Period spotToMature, BusinessDayConvention bdc, Frequency frequency, double rate) {
            this.spotPeriod = spotPeriod;
            this.spotToMature = spotToMature;
            this.bdc = bdc;
            this.frequency = frequency;
            this.rate = rate;
        }

        public Period getSpotPeriod() {
            return spotPeriod;
        }

        public Period getSpotToMature() {
            return spotToMature;
        }

        public BusinessDayConvention getBdc() {
            return bdc;
        }

        public Frequency getFrequency() {
            return frequency;
        }

        public double getRate() {
            return rate;
        }

        public Period getMaturePeriod() {
            return this.spotPeriod.plus(this.getSpotToMature()).normalized();
        }

        @Override
        public int compareTo(Node o) {
            Period minus = this.getMaturePeriod().minus(o.getMaturePeriod()).normalized();
            if (minus.getYears() != 0) {
                return minus.getYears();
            }
            if (minus.getMonths() != 0) {
                return minus.getMonths();
            }
            return minus.getDays();
        }
    }
}
