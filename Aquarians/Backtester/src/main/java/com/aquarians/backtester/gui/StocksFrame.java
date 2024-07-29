/*
    MIT License

    Copyright (c) 2024 Mihai Bunea

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
import com.aquarians.aqlib.Pair;
import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.math.PriceRecord;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.records.StockPriceRecord;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StocksFrame extends MdiFrame {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(StocksFrame.class);

    public static final String NAME = "Stocks";

    private static final Shape CIRCLE = new Ellipse2D.Double(-3, -3, 6, 6);
    private static final Color LINE = Color.gray;
    private static final int SIZE = 10000;

    private final DatabaseModule databaseModule;

    private JTextField codeField;
    private JTextField startDayField;
    private JTextField endDayField;
    private JCheckBox adjustedCheckBox;
    private JCheckBox pointsCheckBox;
    private TimeSeriesCollection dataset;
    private JFreeChart chart;
    private XYItemRenderer originalRenderer;
    private CustomLineAndShapeRenderer pointsRenderer;
    private ChartPanel chartPanel;

    public StocksFrame(MainFrame owner) {
        super(NAME, owner);
        databaseModule = (DatabaseModule) Application.getInstance().getModule(Application.buildModuleName(DatabaseModule.NAME, 0));

        pointsRenderer = new CustomLineAndShapeRenderer(true, true, SIZE);
        pointsRenderer.setSeriesShape(0, CIRCLE);
        pointsRenderer.setSeriesPaint(0, LINE);
        pointsRenderer.setUseFillPaint(true);
        pointsRenderer.setSeriesShapesFilled(0, true);
        pointsRenderer.setSeriesShapesVisible(0, true);
        pointsRenderer.setUseOutlinePaint(true);
        pointsRenderer.setSeriesOutlinePaint(0, LINE);
    }

    private static final class CustomLineAndShapeRenderer extends XYLineAndShapeRenderer {

        private final List<Color> clut;

        public CustomLineAndShapeRenderer(boolean lines, boolean shapes, int n) {
            super(lines, shapes);
            clut = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                clut.add(Color.getHSBColor((float) i / n, 1, 1));
            }
        }

        @Override
        public Paint getItemFillPaint(int row, int column) {
            return clut.get(column);
        }

    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init() {
        JPanel container = new JPanel();
        add(container);

        container.setLayout(new BorderLayout());

        initNorthPane(container);
        initCentralPane(container);

        super.init();
    }

    private void initNorthPane(JPanel parent) {

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        parent.add(panel, BorderLayout.NORTH);

        panel.add(new JLabel("Stock:"));
        codeField = new JTextField(10);
        panel.add(codeField);

        panel.add(new JSeparator());
        panel.add(new JLabel("Start Day (DD-MMM-YYY):"));
        startDayField = new JTextField(10);
        panel.add(startDayField);

        panel.add(new JSeparator());
        panel.add(new JLabel("End Day:"));
        endDayField = new JTextField(10);
        panel.add(endDayField);

        panel.add(new JSeparator());
        panel.add(new JLabel("Adjusted:"));
        adjustedCheckBox = new JCheckBox();
        panel.add(adjustedCheckBox);

        panel.add(new JSeparator());
        panel.add(new JLabel("Points Chart:"));
        pointsCheckBox = new JCheckBox();
        panel.add(pointsCheckBox);
        pointsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleChartStyle();
            }
        });

        panel.add(new JSeparator());
        JButton loadButton = new JButton("Load");
        panel.add(loadButton);

        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String code = codeField.getText();
                final String startDay = startDayField.getText();
                final String endDay = endDayField.getText();
                final boolean adjusted = adjustedCheckBox.isSelected();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            loadStock(code, startDay, endDay, adjusted);
                        } catch (Exception ex) {
                            logger.warn("Error loading stock", ex);
                        }
                    }
                }).start();
            }
        });
    }

    private void initCentralPane(JPanel parent) {
        dataset = new TimeSeriesCollection();
        chart = createTimeChart(dataset, "Stock Prices", "Day", "Price");
        chartPanel = new ChartPanel(chart);

        originalRenderer = chart.getXYPlot().getRenderer();

        parent.add(chartPanel, BorderLayout.CENTER);
    }

    private void loadStock(String code, String startDay, String endDay, boolean adjusted) {
        logger.debug("Loading stock: " + code);

        Long id = databaseModule.getProcedures().underlierSelect.execute(code);
        if (null == id) {
            logger.debug("Stock not found: " + code);
            return;
        }

        Day from = null;
        Day to = null;
        try {
            from = new Day(startDay);
        } catch (Exception ignored) {}
        try {
            to = new Day(endDay);
        } catch (Exception ignored) {}

        if ((null == from) || (null == to)) {
            Pair<Day, Day> datePair = databaseModule.getProcedures().stockPricesSelectMinMaxDate.execute(id);
            if (null == from) {
                from = datePair.getKey();
            }
            if (null == to) {
                to = datePair.getValue();
            }
            if ((null == from) || (null == to)) {
                logger.debug("Stock data interval not supplied for: " + code);
            }

            final String startDayText = from.toString();
            final String endDayText = to.toString();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    startDayField.setText(startDayText);
                    endDayField.setText(endDayText);
                }
            });
        }

        List<StockPriceRecord> ohlcRecords =  databaseModule.getProcedures().stockPricesSelect.execute(id, from, to);
        List<PriceRecord> records = StockPriceRecord.toPriceRecords(ohlcRecords);
        logger.debug("Found " + records.size() + " records for stock " + code + " from " + from + " to " + to);

        if (adjusted) {
            Map<Day, Double> splits = databaseModule.getProcedures().stockSplitsSelect.execute(id, from, to);
            records = Util.adjustForSplits(records, splits);
        }

        final TimeSeries series = new TimeSeries(code);
        for (PriceRecord record : records) {
            org.jfree.data.time.Day day = new org.jfree.data.time.Day(record.day.getDay(), record.day.getMonth(), record.day.getYear());
            series.add(day, record.price);
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                renderChart(series);
            }
        });
    }

    private void renderChart(TimeSeries series) {
        dataset.removeAllSeries();
        dataset.addSeries(series);
        chart.getXYPlot().setDataset(dataset);
    }

    private void toggleChartStyle() {
        if (pointsCheckBox.isSelected()) {
            chart.getXYPlot().setRenderer(pointsRenderer);
        } else {
            chart.getXYPlot().setRenderer(originalRenderer);
        }

        chartPanel.repaint();
    }

}
