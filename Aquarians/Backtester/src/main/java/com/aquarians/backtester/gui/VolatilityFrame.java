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
import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.models.VolatilitySurface;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.pricing.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class VolatilityFrame extends MdiFrame implements PricingListener {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(VolatilityFrame.class);

    public static final String NAME = "Volatility";
    private static final String VOLATILITY_NAME = "Implied";

    public static final DecimalFormat VOLATILITY_FORMAT = new DecimalFormat("###.##");

    private final PricingModule pricingModule;

    private JComboBox<Day> termsCombo;
    private XYSeriesCollection dataset;
    private JFreeChart chart;
    private VolatilityTableModel model;
    private JTable table;

    public VolatilityFrame(MainFrame owner) {
        super(NAME, owner);
        pricingModule = (PricingModule) Application.getInstance().getModule(Application.buildModuleName(PricingModule.NAME, 1));
    }

    @Override
    public void processPricingUpdate() {
        final Map<Day, Map<Double, Double>> terms = extractTerms();
        String underlier = (null != pricingModule.getUnderlier()) ? pricingModule.getUnderlier().code : "Underlier";
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    updateModel(underlier, terms);
                } catch (Exception ex) {
                    logger.warn(ex.getMessage(), ex);
                }
            }
        });
    }

    @Override
    public void init() {
        pricingModule.addListener(this);

        JPanel container = new JPanel();
        add(container);

        container.setLayout(new BorderLayout());

        initNorthPane(container);
        initCentralPane(container);
        initSouthPane(container);

        super.init();

        processPricingUpdate();
    }

    @Override
    public void cleanup() {
        pricingModule.removeListener(this);
    }

    private void initNorthPane(JPanel parent) {
        termsCombo = new JComboBox<>();

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        panel.add(new JLabel("Term: "));
        panel.add(termsCombo);

        parent.add(panel, BorderLayout.NORTH);

        termsCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                processTermsComboItemStateChanged(e);
            }
        });
    }

    private void processTermsComboItemStateChanged(ItemEvent event) {
        int selected = termsCombo.getSelectedIndex();
        if (selected < 0) {
            return;
        }

        Day selectedMaturity = termsCombo.getItemAt(selected);
        if ((null != model.getSelectedMaturity()) && (model.getSelectedMaturity().equals(selectedMaturity))) {
            return;
        }

        // vol table
        model.selectMaturity(selectedMaturity);

        // chart
        XYSeries series = new XYSeries(VOLATILITY_NAME);
        model.getStrikeVols(series);
        dataset.removeAllSeries();
        dataset.addSeries(series);
        adjustRange(chart, series);
    }

    private void initSouthPane(JPanel parent) {
        model = new VolatilityTableModel();
        table = new JTable(model);

        JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        scrollPane.setPreferredSize(new Dimension(320, 80));

        parent.add(scrollPane, BorderLayout.SOUTH);
    }

    private void initCentralPane(JPanel parent) {
        dataset = new XYSeriesCollection();
        chart = createChart(dataset, "Underlier", "Strike", "Volatility");
        ChartPanel chartPanel = new ChartPanel(chart);

        parent.add(chartPanel, BorderLayout.CENTER);
    }

    private Map<Day, Map<Double, Double>> extractTerms() {
        Map<Day, Map<Double, Double>> terms = new TreeMap<>();

        VolatilitySurface surface = pricingModule.getVolatilitySurface();
        if (null == surface) {
            return terms;
        }

        TreeMap<Day, OptionTerm> pricingTerms = pricingModule.getOptionTerms();
        for (Map.Entry<Day, OptionTerm> termEntry : pricingTerms.entrySet()) {
            OptionTerm pricingTerm = termEntry.getValue();

            Map<Double, Double> guiVols = new TreeMap<>();
            for (Double strike : pricingTerm.getStrikes().keySet()) {
                Double vol = surface.getVolatility(pricingTerm.daysToExpiry, strike);
                if (null == vol) {
                    continue;
                }

                guiVols.put(strike, vol * 100.0);
            }

            terms.put(pricingTerm.maturity, guiVols);
        }

        return terms;
    }

    private void updateModel(String underlier, Map<Day, Map<Double, Double>> terms) {
        chart.setTitle(underlier);

        // term
        List<Day> maturities = new ArrayList<>(terms.keySet());
        int selected = termsCombo.getSelectedIndex();
        if (selected < 0) {
            selected = 0; // Select first by default
        }
        Day selectedMaturity = (maturities.size() > 0) ? maturities.get(selected) : null;

        // vol table
        model.setNewData(terms, selectedMaturity);

        // combo
        termsCombo.removeAllItems();
        for (Day maturity : maturities) {
            termsCombo.addItem(maturity);
        }
        if (maturities.size() > 0) {
            termsCombo.setSelectedIndex(selected);
        }

        // chart
        XYSeries series = new XYSeries(VOLATILITY_NAME);
        model.getStrikeVols(series);
        dataset.removeAllSeries();
        dataset.addSeries(series);
        adjustRange(chart, series);
    }

    @Override
    public String getName() {
        return NAME;
    }


}
