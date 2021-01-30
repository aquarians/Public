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

package com.aquarians.backtester.gui;

import com.aquarians.aqlib.Day;
import org.jfree.data.xy.XYSeries;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VolatilityTableModel extends DefaultTableModel {

    public VolatilityTableModel() {
    }

    private Map<Day, Map<Double, Double>> terms;
    private Day selectedMaturity;

    public void setNewData(Map<Day, Map<Double, Double>> terms, Day maturity) {
        this.terms = terms;
        selectedMaturity = maturity;
        fireTableDataChanged();
        fireTableStructureChanged();
    }

    public void selectMaturity(Day maturity) {
        if ((null != selectedMaturity) && (selectedMaturity.equals(maturity))) {
            return;
        }

        selectedMaturity = maturity;
        fireTableDataChanged();
        fireTableStructureChanged();
    }

    public void getStrikeVols(XYSeries series) {
        if (null == selectedMaturity) {
            return;
        }

        Map<Double, Double> strikeVols = terms.get(selectedMaturity);
        if (null == strikeVols) {
            return;
        }

        for (Map.Entry<Double, Double> entry : strikeVols.entrySet()) {
            series.add(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public int getColumnCount() {
        if (null == selectedMaturity) {
            return 0;
        }

        Map<Double, Double> strikeVols = terms.get(selectedMaturity);
        if (null == strikeVols) {
            return 0;
        }

        return strikeVols.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        Map<Double, Double> strikeVols = terms.get(selectedMaturity);
        List<Double> strikes = new ArrayList<>(strikeVols.keySet());
        double strike = strikes.get(columnIndex);
        return VolatilityFrame.VOLATILITY_FORMAT.format(strike);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Map<Double, Double> strikeVols = terms.get(selectedMaturity);
        List<Double> vols = new ArrayList<>(strikeVols.values());
        double vol = vols.get(columnIndex);
        return VolatilityFrame.VOLATILITY_FORMAT.format(vol);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public Day getSelectedMaturity() {
        return selectedMaturity;
    }
}
