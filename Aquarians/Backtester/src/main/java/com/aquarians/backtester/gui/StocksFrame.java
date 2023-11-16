package com.aquarians.backtester.gui;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Pair;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.records.StockPriceRecord;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class StocksFrame extends MdiFrame {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(StocksFrame.class);

    public static final String NAME = "Stocks";

    private final DatabaseModule databaseModule;

    private JTextField codeField;
    private TimeSeriesCollection dataset;
    private JFreeChart chart;

    public StocksFrame(MainFrame owner) {
        super(NAME, owner);
        databaseModule = (DatabaseModule) Application.getInstance().getModule(Application.buildModuleName(DatabaseModule.NAME, 0));
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

        panel.add(new JLabel("Stock Code: "));

        codeField = new JTextField(10);
        panel.add(codeField);

        panel.add(new JSeparator());

        JButton loadButton = new JButton("Load");
        panel.add(loadButton);

        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String code = codeField.getText();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            loadStock(code);
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
        ChartPanel chartPanel = new ChartPanel(chart);

        parent.add(chartPanel, BorderLayout.CENTER);
    }

    private void loadStock(String code) {
        logger.debug("Loading stock: " + code);

        Long id = databaseModule.getProcedures().underlierSelect.execute(code);
        if (null == id) {
            logger.debug("Stock not found: " + code);
            return;
        }

        Pair<Day, Day> datePair = databaseModule.getProcedures().stockPricesSelectMinMaxDate.execute(id);
        Day from = datePair.getKey();
        Day to = datePair.getValue();
        if ((null == from) || (null == to)) {
            logger.debug("Stock data interval not found for: " + code);
        }

        List<StockPriceRecord> records =  databaseModule.getProcedures().stockPricesSelect.execute(id, from, to);
        logger.debug("Found " + records.size() + " records for stock " + code + " from " + from + " to " + to);

        final TimeSeries series = new TimeSeries(code);
        for (StockPriceRecord record : records) {
            org.jfree.data.time.Day day = new org.jfree.data.time.Day(record.day.getDay(), record.day.getMonth(), record.day.getYear());
            series.add(day, record.close);
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

}
