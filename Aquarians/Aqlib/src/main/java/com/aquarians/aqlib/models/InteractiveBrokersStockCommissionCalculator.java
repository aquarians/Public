package com.aquarians.aqlib.models;

// Based on https://www.interactivebrokers.com/en/index.php?f=1590&p=stocks1
public class InteractiveBrokersStockCommissionCalculator implements CommissionCalculator {

    @Override
    public double computeCommission(double quantity, double price) {
        // USD 0.005 per share
        double commission = Math.abs(quantity) * 0.005;

        // Minimum Per Order: $1
        if (commission < 1.0) {
            commission = 1.0;
        }

        // Maximum Per Order: 0.5% of trade value
        double maxCommission = (price * Math.abs(quantity)) * 0.005;
        if (commission > maxCommission) {
            commission = maxCommission;
        }

        return commission;
    }
}
