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

import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OptionsTableModel extends AbstractTableModel {

    private final List<OptionsTableColumn> columns = new ArrayList<>();
    private List<OptionsTableRow> activeRows = new ArrayList<>();
    private Map<Day, List<OptionsTableRow>> terms = new TreeMap<>();
    private Day day;

    public OptionsTableModel() {
        columns.add(new BidColumn(true));
        columns.add(new ValueColumn(true));
        columns.add(new AskColumn(true));
        columns.add(new StrikeColumn());
        columns.add(new BidColumn(false));
        columns.add(new ValueColumn(false));
        columns.add(new AskColumn(false));
    }

    OptionsTableColumn getColumn(int index) {
        if ((index < 0) || (index >= columns.size())) {
            return null;
        }

        return columns.get(index);
    }

    public Color getBackgroundColorAt(int rowIndex, int columnIndex) {
        OptionsTableRow row = activeRows.get(rowIndex);
        OptionsTableColumn column = columns.get(columnIndex);
        return column.getBackgroundColor(row);
    }

    @Override
    public int getRowCount() {
        return activeRows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column).getName();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        OptionsTableRow row = activeRows.get(rowIndex);
        OptionsTableColumn column = columns.get(columnIndex);
        return column.getValue(row);
    }

    public void setTerms(Map<Day, List<OptionsTableRow>> terms) {
        this.terms = terms;
    }

    public void selectMaturity(Day maturity) {
        List<OptionsTableRow> rows = null;
        if (maturity != null) {
            rows = terms.get(maturity);
        }

        if (rows != null) {
            activeRows = rows;
        } else {
            activeRows.clear();
        }

        fireTableDataChanged();
    }

    public void setDay(Day day) {
        this.day = day;
    }

    public Day getDay() {
        return day;
    }
}
