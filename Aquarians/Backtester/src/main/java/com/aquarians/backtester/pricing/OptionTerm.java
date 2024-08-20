/*
    MIT License

    Copyright (c) 2020 Mihai Bunea

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
*/

package com.aquarians.backtester.pricing;

import com.aquarians.aqlib.*;
import com.aquarians.aqlib.math.DefaultProbabilityFitter;
import com.aquarians.aqlib.models.BlackScholes;
import com.aquarians.aqlib.models.PricingResult;

import java.util.*;

public class OptionTerm implements Comparable<OptionTerm> {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(OptionTerm.class.getSimpleName());

    private static final double DEEP_OTM_DEVS = 0.75;

    private final PricingModule owner;
    public final Day maturity;
    public final int daysToExpiry;
    public final double yf;
    public TreeMap<Double, OptionPair> strikes = new TreeMap<>();
    public TreeMap<Double, OptionPair> backupStrikes = new TreeMap<>();

    public OptionTerm(PricingModule owner, Day today, Day maturity) {
        this.owner = owner;
        this.maturity = maturity;
        daysToExpiry = Util.maturity(today, maturity);
        yf = Util.yearFraction(daysToExpiry);
    }

    public Instrument getOption(String code) {
        for (Map.Entry<Double, OptionPair> entry  : strikes.entrySet()) {
            OptionPair pair = entry.getValue();
            if ((null != pair.call) && (pair.call.getCode().equals(code))) {
                return pair.call;
            }

            if ((null != pair.put) && (pair.put.getCode().equals(code))) {
                return pair.put;
            }
        }

        return null;
    }

    @Override
    public int compareTo(OptionTerm that) {
        return this.maturity.compareTo(that.maturity);
    }

    public void add(Instrument instrument) {
        if (instrument.getType().equals(Instrument.Type.OPTION)) {
            OptionPair pair = strikes.get(instrument.getStrike());
            if (null == pair) {
                pair = new OptionPair(instrument.getStrike());
                strikes.put(instrument.getStrike(), pair);
            }

            pair.set(instrument);
        }
    }

    public TreeMap<Double, OptionPair> getStrikes() {
        return strikes;
    }

    public double getYearFraction() {
        return yf;
    }

    public OptionPair getClosestStrike(double strike) {
        OptionPair selectedPair = null;

        Double minDistance = null;
        for (Map.Entry<Double, OptionPair> entry : strikes.entrySet()) {
            OptionPair pair = entry.getValue();
            double distance = Math.abs(strike - pair.strike);
            if ((null == minDistance) || (distance < minDistance)) {
                selectedPair = pair;
                minDistance = distance;
            }
        }

        return selectedPair;
    }

    public List<Instrument> getOtmOptions(boolean calls, double forward) {
        List<Instrument> options = new ArrayList<>(strikes.size());

        if (calls) {
            for (Map.Entry<Double, OptionPair> entry : strikes.entrySet()) {
                Double strike = entry.getKey();
                if (strike < forward) {
                    continue;
                }

                OptionPair pair = entry.getValue();
                if (null == pair.call) {
                    continue;
                }

                options.add(pair.call);
            }
        } else {
            for (Map.Entry<Double, OptionPair> entry : strikes.descendingMap().entrySet()) {
                Double strike = entry.getKey();
                if (strike > forward) {
                    continue;
                }

                OptionPair pair = entry.getValue();
                if (null == pair.put) {
                    continue;
                }

                options.add(pair.put);
            }
        }

        return options;
    }

    public OptionPair getOtmPair(boolean isCall, double forward) {
        if (isCall) {
            for (Map.Entry<Double, OptionPair> entry : strikes.entrySet()) {
                Double strike = entry.getKey();
                if (strike < forward) {
                    continue;
                }

                return entry.getValue();
            }
        } else {
            for (Map.Entry<Double, OptionPair> entry : strikes.descendingMap().entrySet()) {
                Double strike = entry.getKey();
                if (strike > forward) {
                    continue;
                }

                return entry.getValue();
            }
        }

        return null;
    }

    public Instrument getOtmOption(boolean isCall, double forward) {
        if (isCall) {
            for (Map.Entry<Double, OptionPair> entry : strikes.entrySet()) {
                Double strike = entry.getKey();
                if (strike < forward) {
                    continue;
                }

                OptionPair pair = entry.getValue();
                if (null == pair.call) {
                    continue;
                }

                return pair.call;
            }
        } else {
            for (Map.Entry<Double, OptionPair> entry : strikes.descendingMap().entrySet()) {
                Double strike = entry.getKey();
                if (strike > forward) {
                    continue;
                }

                OptionPair pair = entry.getValue();
                if (null == pair.put) {
                    continue;
                }

                return pair.put;
            }
        }

        return null;
    }

    public Instrument getAtmOption(boolean isCall, double forward) {
        Instrument prev = null;
        if (isCall) {
            for (Map.Entry<Double, OptionPair> entry : strikes.entrySet()) {
                Double strike = entry.getKey();
                if (strike < forward) {
                    continue;
                }

                OptionPair pair = entry.getValue();
                if (null == pair.call) {
                    continue;
                }

                if (Math.abs(strike - forward) < Util.MINIMUM_PRICE) {
                    return pair.call;
                }

                if (null != prev) {
                    return prev;
                }

                prev = pair.call;
            }
        } else {
            for (Map.Entry<Double, OptionPair> entry : strikes.descendingMap().entrySet()) {
                Double strike = entry.getKey();
                OptionPair pair = entry.getValue();

                if ((Math.abs(strike - forward) < Util.MINIMUM_PRICE) && (null != pair.put)) {
                    return pair.put;
                }

                if (strike < forward) {
                    break;
                }

                if (null != pair.put) {
                    prev = pair.put;
                }
            }
        }

        return prev;
    }

    public Double computeParityForwardPrice(double interestRate, boolean doForwardSanityCheck) {
        List<OptionPair> pairs = new ArrayList<>(strikes.size());

        // Select valid pairs
        for (OptionPair pair : strikes.values()) {
            if (pair.hasFullSpread()) {
                pairs.add(pair);
            }
        }
        if (0 == pairs.size()) {
            return null;
        }

        // Find the strike closest to the forward price
        // C - P = F - K therefore for F = K we have C = P
        OptionPair forwardPair = null;
        Double minDistance = null;
        for (OptionPair pair : pairs) {
            double distance = Math.abs(pair.call.getPrice() - pair.put.getPrice());
            if ((null == minDistance) || (distance < minDistance)) {
                minDistance = distance;
                forwardPair = pair;
            }
        }

        // Compute ATM implied volatility using the forward strike as approximation for forward price
        Instrument atmOption = (forwardPair.call.getPrice() < forwardPair.put.getPrice()) ? forwardPair.call : forwardPair.put;
        BlackScholes pricer = new BlackScholes(atmOption.isCall(), atmOption.getStrike(), atmOption.getStrike(), yf, interestRate, 0.0, 0.0);
        pricer.setBlack(true); // Use the Black model (dividend yield already contained in the forward price)
        Double atmVol = pricer.impliedVolatility(atmOption.getPrice());
        if (null == atmVol) {
            return null;
        }

        // Compute standard deviation of the forward price log return at expiration
        double dev = atmVol * Math.sqrt(yf);

        // Take an average of the forward price implied by the put-call parity
        DefaultProbabilityFitter forwards = new DefaultProbabilityFitter(pairs.size());
        for (OptionPair pair : pairs) {
            // Filter out deep out of the money strikes
            double ret = Math.log(pair.strike / forwardPair.strike);
            if (Math.abs(ret) > dev * DEEP_OTM_DEVS) {
                continue;
            }

            double factor = Math.exp(interestRate * yf);

            // C - P = (F - K) * e^(-r*T)
            double impliedForward = pair.strike + (pair.call.getPrice() - pair.put.getPrice()) * factor;
            forwards.addSample(impliedForward);
        }

        if (forwards.size() < 1) {
            return null;
        }

        forwards.compute();
        double forward = forwards.getMean();

        // Minimum quality requirements for the forward calculation
        if (doForwardSanityCheck && (!forwardSanityCheck(forward, interestRate))) {
            return null;
        }

        return forward;
    }

    // Check there is no parity arbitrage around ATM
    private boolean forwardSanityCheck(double forward, double interestRate) {
        if (owner.getValidateForward() < 1) {
            return true;
        }

        double totalRate = interestRate + owner.getBorrowRate();
        double borrow = forward * (Math.exp(totalRate * yf) - 1.0);

        // Order the strikes by distance from ATM
        List<Double> orderedStrikes = new ArrayList<>(strikes.keySet());
        Collections.sort(orderedStrikes, new DistanceFromATMComparator(forward));

        for (int i = 0; i < Math.min(owner.getValidateForward(), strikes.size()); i++) {
            Double strike = orderedStrikes.get(i);
            OptionPair pair = strikes.get(strike);
            double pnl = pair.getParityArbitragePnl(forward, borrow);
            if (pnl > Util.ZERO) {
                return false;
            }
        }

        return true;
    }

    private static final class DistanceFromATMComparator implements Comparator<Double> {
        private final double forward;

        private DistanceFromATMComparator(double forward) {
            this.forward = forward;
        }

        @Override
        public int compare(Double leftStrike, Double rightStrike) {
            return Double.compare(Math.abs(leftStrike - forward), Math.abs(rightStrike - forward));
        }
    }

    // Computes lower and upper bound for forward price
    public Pair<Double, Double> computeParityForwardPriceBounds() {
        TreeSet<Double> lowerBounds = new TreeSet<>();
        TreeSet<Double> upperBounds = new TreeSet<>();

        // Cost of borrow
        double borrowRate = owner.getInterestRate(owner.getToday()) + owner.getBorrowRate();
        double borrowCost = (owner.getSpotPrice() != null) ? owner.getSpotPrice() * (Math.exp(borrowRate * yf) - 1.0) : 0.0;

        for (OptionPair pair : strikes.values()) {
            if (!pair.hasFullSpread()) {
                continue;
            }

            Double lowerBound = Util.getParitySpotLowerBound(pair.call, pair.put);
            if (lowerBound != null) {
                lowerBounds.add(lowerBound - borrowCost);
            }

            Double upperBound = Util.getParitySpotUpperBound(pair.call, pair.put);
            if (upperBound != null) {
                upperBounds.add(upperBound + borrowCost);
            }
        }

        Double lowerBound = lowerBounds.size() > 0 ? lowerBounds.last() : null;
        Double upperBound = upperBounds.size() > 0 ? upperBounds.first() : null;
        return new Pair<>(lowerBound, upperBound);
    }

    public boolean hasParityViolation() {
        Pair<Double, Double> bounds = computeParityForwardPriceBounds();
        Double lowerBound = bounds.getKey();
        Double upperBound = bounds.getValue();

        boolean isViolation = Util.doubleGE(lowerBound, upperBound);
        logger.debug("PARITY Underlier " + owner.getUnderlier().code + " Day=" + owner.getToday() +
                " Term=" + maturity +
                " LowerBound=" + Util.format(lowerBound) +
                " UpperBound=" + Util.format(upperBound) +
                " Violation=" + Util.format(isViolation));

        return isViolation;
    }

    interface OptionAccessor {
        Instrument getOption(OptionPair pair);
    }

    interface PriceAccessor {
        Double getPrice(Instrument option);
    }

    interface PriceSetter {
        void setPrice(Instrument option, double price);
    }

    private static void collectPrice(Instrument option, PriceAccessor priceAccessor, List<Instrument> instruments) {
        if (null == option) {
            return;
        }

        Double price = priceAccessor.getPrice(option);
        if ((null == price) || (price < Util.ZERO)) {
            return;
        }

        instruments.add(option);
    }

    public void validatePrices() {
        // Do an initial filtering to be able to calculate the forward
        validatePricesFirstPass();
        // Use calculated forward to do another filtering
        validatePricesSecondPass();
        // Delete strikes that have no liquidity
        deleteEmptyStrikes();
        // Check for parity violations, don't do nothing, just log
        hasParityViolation();
    }

    private void deleteEmptyStrikes() {
        List<Double> emptyStrikes = new ArrayList<>();
        for (OptionPair pair : strikes.values()) {
            if (pair.isEmpty()) {
                emptyStrikes.add(pair.strike);
            }
        }

        for (Double strike : emptyStrikes) {
            strikes.remove(strike);
        }
    }

    public void validatePricesFirstPass() {
        // Collect here prices in decreasing order (highest first, lowest last)
        List<Instrument> callBids = new ArrayList<>();
        List<Instrument> callAsks = new ArrayList<>();
        List<Instrument> putBids = new ArrayList<>();
        List<Instrument> putAsks = new ArrayList<>();

        // Call prices decrease with increasing strike price
        for (Map.Entry<Double, OptionPair> entry : strikes.entrySet()) {
            OptionPair pair = entry.getValue();
            collectPrice(pair.call, option -> option.getBidPrice(), callBids);
            collectPrice(pair.call, option -> option.getAskPrice(), callAsks);
        }

        // Put prices decrease with decreasing strike price
        for (Map.Entry<Double, OptionPair> entry : strikes.descendingMap().entrySet()) {
            OptionPair pair = entry.getValue();
            collectPrice(pair.put, option -> option.getBidPrice(), putBids);
            collectPrice(pair.put, option -> option.getAskPrice(), putAsks);
        }

        // Get the longest decreasing sequences
        List<Integer> callBidsLDS = findLDS(callBids, option -> option.getBidPrice());
        List<Integer> callAsksLDS = findLDS(callAsks, option -> option.getAskPrice());
        List<Integer> putBidsLDS = findLDS(putBids, option -> option.getBidPrice());
        List<Integer> putAsksLDS = findLDS(putAsks, option -> option.getAskPrice());

        // Rebuild the prices with validated values
        TreeMap<Double, OptionPair> newStrikes = createStrikes();
        setPrices(newStrikes, callBids, callBidsLDS, pair -> pair.call, option -> option.getBidPrice(), (option, price) -> option.setBidPrice(price));
        setPrices(newStrikes, callAsks, callAsksLDS, pair -> pair.call, option -> option.getAskPrice(), (option, price) -> option.setAskPrice(price));
        setPrices(newStrikes, putBids, putBidsLDS, pair -> pair.put, option -> option.getBidPrice(), (option, price) -> option.setBidPrice(price));
        setPrices(newStrikes, putAsks, putAsksLDS, pair -> pair.put, option -> option.getAskPrice(), (option, price) -> option.setAskPrice(price));

        // Make a backup of the original prices
        backupStrikes();

        // Copy the validated prices over the old ones
        replacePrices(newStrikes);
    }

    private void replacePrices(TreeMap<Double, OptionPair> newStrikes) {
        for (OptionPair oldPair : strikes.values()) {
            OptionPair newPair = newStrikes.get(oldPair.strike);
            if (oldPair.call != null) {
                oldPair.call.setBidPrice(newPair.call.getBidPrice());
                oldPair.call.setAskPrice(newPair.call.getAskPrice());
            }
            if (oldPair.put != null) {
                oldPair.put.setBidPrice(newPair.put.getBidPrice());
                oldPair.put.setAskPrice(newPair.put.getAskPrice());
            }
        }
    }

    // An instrument from whose prices was extracted the intrinsic value, thus leaving the extrinsic one
    private static final class ExtrinsicInstrument extends Instrument {
        final Instrument originalInstrument;
        final double intrinsicValue;

        public ExtrinsicInstrument(Instrument originalInstrument, double intrinsicValue) {
            super(originalInstrument.getType(),
                    originalInstrument.getCode(),
                    originalInstrument.isCall(),
                    originalInstrument.getMaturity(),
                    originalInstrument.getStrike());
            this.originalInstrument = originalInstrument;
            this.intrinsicValue = intrinsicValue;

            this.setBidPrice(getBidPrice());
            this.setAskPrice(getAskPrice());
        }

        public Instrument getOriginalInstrument() {
            return originalInstrument;
        }

        public Double getBidPrice() {
            Double bidPrice = originalInstrument.getBidPrice();
            if (null == bidPrice) {
                return null;
            }

            return bidPrice - intrinsicValue;
        }

        public Double getAskPrice() {
            Double askPrice = originalInstrument.getAskPrice();
            if (null == askPrice) {
                return null;
            }

            return askPrice - intrinsicValue;
        }
    }

    public static ExtrinsicInstrument createExtrinsicInstrument(Instrument instrument, double intrinsicValue) {
        if (null == instrument) {
            return null;
        }

        return new ExtrinsicInstrument(instrument, intrinsicValue);
    }

    public void validatePricesSecondPass() {
        // No sanity check on forward validity at this point, that's only used in pricing calculations
        Double forward = computeParityForwardPrice(owner.getInterestRate(owner.getToday()), false);
        if (null == forward) {
            return;
        }

        // Rebuild the prices
        TreeMap<Double, OptionPair> newStrikes = createStrikes();

        // Collect here extrinsic prices in decreasing order (highest first, lowest last)
        // An option price = extrinsic + intrinsic, where intrinsic = |Fwd - Strike|
        List<Instrument> callBids = new ArrayList<>();
        List<Instrument> callAsks = new ArrayList<>();
        List<Instrument> putBids = new ArrayList<>();
        List<Instrument> putAsks = new ArrayList<>();

        // OTM calls and ITM puts, prices decrease with increasing strike price
        for (Map.Entry<Double, OptionPair> entry : strikes.entrySet()) {
            OptionPair pair = entry.getValue();
            double intrinsicValue = pair.strike - forward;
            if (intrinsicValue < 0.0) {
                continue;
            }

            collectPrice(pair.call, option -> option.getBidPrice(), callBids);
            collectPrice(pair.call, option -> option.getAskPrice(), callAsks);
            collectPrice(createExtrinsicInstrument(pair.put, intrinsicValue), option -> option.getBidPrice(), putBids);
            collectPrice(createExtrinsicInstrument(pair.put, intrinsicValue), option -> option.getAskPrice(), putAsks);
        }

        // Copy prices as long as the extrinsic value keeps decreasing
        // We want asks to be strictly decreasing (don't want to pay the same price for less insurance)
        setPrices(newStrikes, callBids, pair -> pair.call, option -> option.getBidPrice(), (option, price) -> option.setBidPrice(price), false);
        setPrices(newStrikes, callAsks, pair -> pair.call, option -> option.getAskPrice(), (option, price) -> option.setAskPrice(price), true);
        setPrices(newStrikes, putBids, pair -> pair.put, option -> option.getBidPrice(), (option, price) -> option.setBidPrice(price), false);
        setPrices(newStrikes, putAsks, pair -> pair.put, option -> option.getAskPrice(), (option, price) -> option.setAskPrice(price), true);

        // Now cover the other direction
        callBids = new ArrayList<>();
        callAsks = new ArrayList<>();
        putBids = new ArrayList<>();
        putAsks = new ArrayList<>();

        // ITM calls and OTM puts, prices decrease with decreasing strike price
        for (Map.Entry<Double, OptionPair> entry : strikes.descendingMap().entrySet()) {
            OptionPair pair = entry.getValue();
            double intrinsicValue = forward - pair.strike;
            if (intrinsicValue < 0.0) {
                continue;
            }

            collectPrice(createExtrinsicInstrument(pair.call, intrinsicValue), option -> option.getBidPrice(), callBids);
            collectPrice(createExtrinsicInstrument(pair.call, intrinsicValue), option -> option.getAskPrice(), callAsks);
            collectPrice(pair.put, option -> option.getBidPrice(), putBids);
            collectPrice(pair.put, option -> option.getAskPrice(), putAsks);
        }

        setPrices(newStrikes, callBids, pair -> pair.call, option -> option.getBidPrice(), (option, price) -> option.setBidPrice(price), false);
        setPrices(newStrikes, callAsks, pair -> pair.call, option -> option.getAskPrice(), (option, price) -> option.setAskPrice(price), true);
        setPrices(newStrikes, putBids, pair -> pair.put, option -> option.getBidPrice(), (option, price) -> option.setBidPrice(price), false);
        setPrices(newStrikes, putAsks, pair -> pair.put, option -> option.getAskPrice(), (option, price) -> option.setAskPrice(price), true);

        // Copy the validated prices over the old ones
        replacePrices(newStrikes);
    }

    // Iterative function to find the indexes of longest decreasing subsequence of a given array
    private static List<Integer> findLDS(List<Instrument> options, PriceAccessor accessor) {
        // base case
        if (options == null || options.size() == 0) {
            return new ArrayList<>();
        }

        // `LDS[i]` stores the longest decreasing subsequence of subarray
        // `nums[0…i]` that ends with `nums[i]`
        List<List<Integer>> LDS = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            LDS.add(new ArrayList<>());
        }

        // `LDS[0]` denotes longest decreasing subsequence ending at `nums[0]`
        LDS.get(0).add(0);

        // start from the second array element
        for (int i = 1; i < options.size(); i++) {
            Instrument ioption = options.get(i);

            // do for each element in subarray `nums[0…i-1]`
            for (int j = 0; j < i; j++) {
                Instrument joption = options.get(j);

                // find longest decreasing subsequence that ends with `nums[j]`
                // where `options[j]` is more than the current element `nums[i]`
                if (accessor.getPrice(joption) >= accessor.getPrice(ioption) && LDS.get(j).size() > LDS.get(i).size()) {
                    LDS.set(i, new ArrayList<>(LDS.get(j)));
                }
            }

            // include `options[i]` in `LDS[i]`
            LDS.get(i).add(i);
        }

        // `j` will contain an index of LDS
        int j = 0;
        for (int i = 0; i < options.size(); i++) {
            if (LDS.get(j).size() < LDS.get(i).size()) {
                j = i;
            }
        }

        // return LDS
        return LDS.get(j);
    }

    private void setPrices(TreeMap<Double, OptionPair> pairs,
                           List<Instrument> options,
                           List<Integer> pricesLDS,
                           OptionAccessor optionAccessor,
                           PriceAccessor priceAccessor,
                           PriceSetter priceSetter) {
        Double prevPrice = null;
        for (Integer i : pricesLDS) {
            Instrument option = options.get(i);
            OptionPair pair = pairs.get(option.getStrike());
            double currPrice = priceAccessor.getPrice(option);
            if ((prevPrice != null) && (currPrice > prevPrice)) {
                throw new RuntimeException("Prices not in decreasing order");
            }
            Instrument target = optionAccessor.getOption(pair);
            priceSetter.setPrice(target, currPrice);
            prevPrice = currPrice;
        }
    }

    private void setPrices(TreeMap<Double, OptionPair> pairs,
                           List<Instrument> options,
                           OptionAccessor optionAccessor,
                           PriceAccessor priceAccessor,
                           PriceSetter priceSetter,
                           boolean strictlyDecreasing) {
        Double prevPrice = null;
        for (Instrument option : options) {
            OptionPair pair = pairs.get(option.getStrike());
            double currPrice = priceAccessor.getPrice(option);
            if ((prevPrice != null) && (strictlyDecreasing ? (currPrice >= prevPrice) : (currPrice > prevPrice))) {
                break;
            }

            Instrument target = optionAccessor.getOption(pair);

            // When we copy prices from calculation objects we still want the original values
            Instrument source = option;
            if (option instanceof ExtrinsicInstrument) {
                source = ((ExtrinsicInstrument) option).getOriginalInstrument();
            }

            double sourcePrice = priceAccessor.getPrice(source);
            priceSetter.setPrice(target, sourcePrice);

            prevPrice = currPrice;
        }
    }

    private TreeMap<Double, OptionPair> createStrikes() {
        TreeMap<Double, OptionPair> pairs = new TreeMap<>();

        for (OptionPair oldPair : strikes.values()) {

            OptionPair newPair = new OptionPair(oldPair.strike);
            pairs.put(newPair.strike, newPair);

            if (oldPair.call != null) {
                newPair.call = oldPair.call.clone();
                newPair.call.setBidPrice(null);
                newPair.call.setAskPrice(null);
            }

            if (oldPair.put != null) {
                newPair.put = oldPair.put.clone();
                newPair.put.setBidPrice(null);
                newPair.put.setAskPrice(null);
            }
        }

        return pairs;
    }

    private void backupStrikes() {
        backupStrikes = new TreeMap<>();
        for (OptionPair pair : strikes.values()) {
            backupStrikes.put(pair.strike, pair.clone());
        }
    }

    public void restoreBackup() {
        strikes = backupStrikes;
    }

    public double getMaxParityArbitrageReturn(PricingModel model) {
        double maxRet = 0.0;
        Double spot = model.getSpot();
        if (null == spot) {
            return maxRet;
        }

        for (OptionPair pair : strikes.values()) {
            Instrument instrument = new Instrument(Instrument.Type.PARITY, null, null, maturity, pair.strike);
            PricingResult result = model.price(instrument);
            if ((result != null) && (result.price > 0.0)) {
                double ret = Math.log((result.price + spot) / spot);
                double annualizedRet = ret / yf;
                maxRet = Math.max(maxRet, annualizedRet);
            }
        }

        logger.debug("ParityArbitrage und=" + owner.getUnderlier().code + " day=" + owner.getToday() +
                " mat=" + maturity + " ret=" + Util.format(maxRet * 100.0));
        return maxRet;
    }

    private double getOptionArbitrageReturn(PricingModel model, Instrument option) {
        if (null == option) {
            return 0.0;
        }

        PricingResult result = model.price(option);
        if ((null == result) || (null == result.price)) {
            return 0.0;
        }

        Double spot = model.getSpot();
        if (null == spot) {
            return 0.0;
        }

        Pair<Double, Double> pnls = owner.getExpectedPnl(option, result.price);

        double bidPnl = pnls.getKey();
        if (bidPnl > 0.0) {
            // Annualized return
            return Math.log((bidPnl + spot) / spot) / yf;
        }

        double askPnl = pnls.getValue();
        if (askPnl > 0.0) {
            // Annualized return
            return Math.log((askPnl + spot) / spot) / yf;
        }

        return 0.0;
    }

    public double getMaxOptionArbitrageReturn(PricingModel model) {
        double maxRet = 0.0;

        for (OptionPair pair : strikes.values()) {
            maxRet = Math.max(maxRet, getOptionArbitrageReturn(model, pair.put));
            maxRet = Math.max(maxRet, getOptionArbitrageReturn(model, pair.call));
        }

        return maxRet;
    }

    public void validateSpread() {
        List<Double> incompleteStrikes = new ArrayList<>();
        for (OptionPair pair : strikes.values()) {
            if (!pair.hasFullSpread()) {
                incompleteStrikes.add(pair.strike);
            }
        }

        for (Double strike : incompleteStrikes) {
            strikes.remove(strike);
        }
    }

}
