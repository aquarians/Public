package com.aquarians.aqlib.models;

// Based on https://www.interactivebrokers.com/en/index.php?f=commission&p=options1
public class InteractiveBrokersOptionCommissionCalculator implements CommissionCalculator {

    public static final double CONTRACT_SIZE = 100.0;

    @Override
    public double computeCommission(double quantity, double price) {
        double contracts = Math.abs(quantity) / CONTRACT_SIZE;

        double rate = 0.0;
        double minimum = 0.0;

        if (price >= 0.1) { // Premium >= USD 0.10
            rate = 0.7; // Commissions: USD 0.70 per contract
            minimum = 1.0; // Minimum Per Order: $1
        } else if (price >= 0.05) { // USD 0.05 <= Premium < USD 0.10
            rate = 0.5; // Commissions: USD 0.70 per contract
            minimum = 1.0; // Minimum Per Order: $1
        } else { // Premium < USD 0.05
            rate = 0.25; // Commissions: USD 0.70 per contract
            minimum = 1.0; // Minimum Per Order: $1
        }

        double commission = contracts * rate;
        if (commission < minimum) {
            commission = minimum;
        }

        return commission;
    }
}
