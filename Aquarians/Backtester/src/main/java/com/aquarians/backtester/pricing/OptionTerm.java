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
import com.aquarians.aqlib.models.VolatilitySurface;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OptionTerm implements Comparable<OptionTerm> {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(OptionTerm.class.getSimpleName());

    private static final double DEEP_OTM_DEVS = 0.75;

    public final Day maturity;
    public final int daysToExpiry;
    public final double yf;
    public TreeMap<Double, OptionPair> strikes = new TreeMap<>();
    public TreeMap<Double, OptionPair> backupStrikes = new TreeMap<>();

    public OptionTerm(Day today, Day maturity) {
        this.maturity = maturity;
        daysToExpiry = today.countTradingDays(maturity);
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

    private void computeModelError(PricingModel model, Ref<Double> totalError, double tolerance, Instrument instrument, double dstrike) {
        if (null == instrument) {
            return;
        }

        PricingResult result = model.price(instrument);
        if ((null == result) || (null == result.price)) {
            return;
        }

        Double spot = model.getSpot();
        if (null == spot) {
            return;
        }

        Double error = null;
        if ((null != instrument.getBidPrice()) && (result.price < instrument.getBidPrice())) {
            error = instrument.getBidPrice() - result.price;
        } else if ((null != instrument.getAskPrice()) && (result.price > instrument.getAskPrice())) {
            error = result.price - instrument.getAskPrice();
        } else {
            return;
        }

        if (tolerance > 0.0) {
            // Price a theoretical ATM option
            Instrument atmOption = new Instrument(Instrument.Type.OPTION, null, true, maturity, spot);
            PricingResult pricing = model.price(atmOption);
            // Ignore error if smaller than tolerance threshold
            if ((pricing.pnlDev != null) && (error < tolerance * pricing.pnlDev)) {
                return;
            }
        }

        totalError.value += (error * dstrike) / spot;
    }

    public void computeModelError(PricingModel model, Ref<Double> totalError, double tolerance) {
        Double prevStrike = null;
        for (Map.Entry<Double, OptionPair> entry : strikes.entrySet()) {
            OptionPair pair = entry.getValue();
            double dstrike = (prevStrike != null) ? prevStrike - pair.strike : 0.0;
            computeModelError(model, totalError, tolerance, pair.put, dstrike);
            computeModelError(model, totalError, tolerance, pair.call, dstrike);
            prevStrike = pair.strike;
        }
    }

    // The option with the highest extrinsic value
    public Instrument getBestOption(double forward, boolean isCall, boolean isBid) {
        Instrument bestOption = null;
        Double bestPrice = null;

        for (Map.Entry<Double, OptionPair> entry  : strikes.entrySet()) {
            OptionPair pair = entry.getValue();

            Instrument option = isCall ? pair.call : pair.put;
            if (null == option) {
                continue;
            }

            Double price = isBid ? option.getBidPrice() : option.getAskPrice();
            if (null == price) {
                continue;
            }

            // Subtract the intrinsic value
            double sign = isCall ? 1.0 : -1.0;
            double intrinsic = Math.max(sign * (forward - option.getStrike()), 0.0);
            double extrinsic = price - intrinsic;
            if (extrinsic < Util.MINIMUM_PRICE) {
                continue;
            }

            if ((null == bestPrice) || (extrinsic > bestPrice)) {
                bestOption = option;
                bestPrice = extrinsic;
            }
        }

        return bestOption;
    }

    private static final class ParityForwardFinder {
        Double forward = null;
        Double min_distance = null;

        void process(Instrument call, Instrument put, Double strike) {
            Double call_price = (call != null) ? call.getPrice() : null;
            Double put_price = (put != null) ? put.getPrice() : null;
            if ((null == call_price) || (null == put_price)) {
                return;
            }

            double distance = Math.abs(call_price - put_price);
            if ((null == min_distance) || (distance < min_distance)) {
                min_distance = distance;
                forward = call_price - put_price + strike; // put-call parity
            }
        }
    }

    public Double computeParityForwardPrice() {
        // Find strike with call and put closest in value
        List<OptionPair> pairs = new ArrayList<>(this.strikes.values());

        // First try the same strike (if both call and put liquidity is available)
        ParityForwardFinder finder = new ParityForwardFinder();
        for (OptionPair pair : pairs) {
            finder.process(pair.call, pair.put, pair.strike);
        }
        if (finder.forward != null) {
            return finder.forward;
        }

        // Try adjacent strikes
        finder.min_distance = null;
        OptionPair prev_pair = null;
        for (OptionPair curr_pair : pairs) {
            if (prev_pair != null) {
                double strike = (curr_pair.strike + prev_pair.strike) / 2.0;
                finder.process(curr_pair.call, prev_pair.put, strike);
                finder.process(prev_pair.call, curr_pair.put, strike);
            }
            prev_pair = curr_pair;
        }
        return finder.forward;
    }

    public Double computeParityForwardPrice(double interestRate) {
        List<OptionPair> pairs = new ArrayList<>(strikes.size());

        // Select valid pairs
        for (Map.Entry<Double, OptionPair> entry : strikes.entrySet()) {
            OptionPair pair = entry.getValue();
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

        forwards.compute();
        return forwards.getMean();
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

    private static void collectPrice(OptionPair pair, OptionAccessor optionAccessor, PriceAccessor priceAccessor, List<Instrument> instruments) {
        Instrument option = optionAccessor.getOption(pair);
        if (null == option) {
            return;
        }

        Double price = priceAccessor.getPrice(option);
        if (null == price) {
            return;
        }

        instruments.add(option);
    }

    public void validatePrices() {
        List<Instrument> callBids = new ArrayList<>();
        List<Instrument> callAsks = new ArrayList<>();
        List<Instrument> putBids = new ArrayList<>();
        List<Instrument> putAsks = new ArrayList<>();

        // Call prices must increase with increasing strike price
        for (Map.Entry<Double, OptionPair> entry : strikes.entrySet()) {
            collectPrice(entry.getValue(), pair -> pair.call, option -> option.getBidPrice(), callBids);
            collectPrice(entry.getValue(), pair -> pair.call, option -> option.getAskPrice(), callAsks);
        }

        // Put prices must increase with decreasing strike price
        for (Map.Entry<Double, OptionPair> entry : strikes.descendingMap().entrySet()) {
            collectPrice(entry.getValue(), pair -> pair.put, option -> option.getBidPrice(), putBids);
            collectPrice(entry.getValue(), pair -> pair.put, option -> option.getAskPrice(), putAsks);
        }

        // Get the longest increasing sequences
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

    public double getTotalParityArbitrage(PricingModel model) {
        double total = 0.0;
        Double spot = model.getSpot();
        if (null == spot) {
            return total;
        }

        Double prevStrike = null;
        for (OptionPair pair : strikes.values()) {
            Instrument instrument = new Instrument(Instrument.Type.PARITY, null, null, maturity, pair.strike);
            PricingResult result = model.price(instrument);
            if ((result != null) && (result.price > 0.0)) {
                double dstrike = (prevStrike != null) ? (pair.strike - prevStrike) : 0.0;
                total += (result.price * dstrike) / spot;
            }

            prevStrike = pair.strike;
        }

        return total;
    }

    public double getTotalOptionArbitrage(PricingModel model) {
        Ref<Double> totalError = new Ref<>(0.0);
        computeModelError(model, totalError, 0.0);
        return totalError.value;
    }

}
