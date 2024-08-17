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

import com.aquarians.aqlib.*;
import com.aquarians.aqlib.models.PricingResult;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.pricing.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OptionsFrame extends MdiFrame implements PricingListener {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(OptionsFrame.class);

    public static final String NAME = "Options";

    public static final DecimalFormat PRICE_FORMAT = new DecimalFormat("###.####");

    private static final String DAY_LABEL = "Day: ";
    private static final String SPOT_LABEL = "Spot: ";
    private static final String FORWARD_LABEL = "Fwd: ";
    private static final String MATURITY_LABEL = "Maturity: ";

    public static final Color ODD_ROW_BACKGROUND_COLOR = new Color(0xCC, 0xCC, 0xCC);
    public static final Color EVEN_ROW_BACKGROUND_COLOR = new Color(0xDD, 0xDD, 0xDD);
    public static final Color VALUE_COLUMN_BACKGROUND_COLOR = new Color(0xCC, 0xCC, 0xFF);
    public static final Color STRIKE_COLUMN_BACKGROUND_COLOR = new Color(0xFF, 0xE4, 0xB5);
    public static final Color SPOT_ROW_BACKGROUND_COLOR = new Color(0xFF, 0xA5, 0x00);
    public static final Color PRICE_HIGHIGHT_BACKGROUND_COLOR = new Color(0xFF, 0x88, 0x77);

    private final PricingModule pricingModule;
    private JComboBox<Day> termsCombo;
    private JLabel dayLabel = new JLabel(DAY_LABEL);
    private JLabel spotLabel = new JLabel(SPOT_LABEL);
    private JLabel forwardLabel = new JLabel(SPOT_LABEL);
    private JLabel maturityLabel = new JLabel(MATURITY_LABEL);
    private OptionsTableModel model;
    private JTable table;
    private Map<Day, Double> forwards = new TreeMap<>();

    public OptionsFrame(MainFrame owner) {
        super(NAME, owner);
        pricingModule = (PricingModule) Application.getInstance().getModule(Application.buildModuleName(PricingModule.NAME, 1));
    }

    @Override
    public void processPricingUpdate() {
        final GuiData data = extractGuiData();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    updateModel(data);
                } catch (Exception ex) {
                    logger.warn(ex.getMessage(), ex);
                }
            }
        });
    }

    private static final class GuiData {
        Day today;
        Double spot;
        Map<Day, List<OptionsTableRow>> terms = new TreeMap<>();
        Map<Day, Double> forwards = new TreeMap<>();
    }

    private GuiData extractGuiData() {
        GuiData data = new GuiData();

        data.today = pricingModule.getToday();
        data.spot = pricingModule.getSpotPrice();

        for (Map.Entry<Day, OptionTerm> entry : pricingModule.getOptionTerms().entrySet()) {
            extractTerm(entry.getKey(), entry.getValue(), data.terms, data.forwards);
        }

        return data;
    }

    private void extractTerm(Day maturity, OptionTerm optionTerm, Map<Day, List<OptionsTableRow>> guiTerms, Map<Day, Double> forwards) {
        List<OptionsTableRow> rows = new ArrayList<>(optionTerm.getStrikes().size() * 2);
        guiTerms.put(maturity, rows);

        // Find the at-the-money strike
        Double spot = pricingModule.getSpotPrice();
        Double atmStrike = null;
        if (spot != null) {
            Double minDistance = null;
            for (double strike : optionTerm.getStrikes().keySet()) {
                double distance = Math.abs(strike - spot);
                if ((null == minDistance) || (distance < minDistance)) {
                    minDistance = distance;
                    atmStrike = strike;
                }
            }
        }

        for (Map.Entry<Double, OptionPair> entry : optionTerm.getStrikes().entrySet()) {
            OptionPair pair = entry.getValue();

            Double callValue = null;
            Double callBid = null;
            Double callAsk = null;
            double callBidPnl = 0.0;
            double callAskPnl = 0.0;
            if (pair.call != null) {
                PricingModel model = pricingModule.getPricingModel();
                PricingResult result = model.price(pair.call);
                if (null != result) {
                    callValue = result.price;
                    Pair<Double, Double> pnls = pricingModule.getExpectedPnl(pair.call, result.price);
                    callBidPnl = pnls.getKey();
                    callAskPnl = pnls.getValue();
                }
                callBid = pair.call.getBidPrice();
                callAsk = pair.call.getAskPrice();
            }

            Double putValue = null;
            Double putBid = null;
            Double putAsk = null;
            double putBidPnl = 0.0;
            double putAskPnl = 0.0;
            if (pair.put != null) {
                PricingResult result = pricingModule.getPricingModel().price(pair.put);
                if (null != result) {
                    putValue = result.price;
                    Pair<Double, Double> pnls = pricingModule.getExpectedPnl(pair.put, result.price);
                    putBidPnl = pnls.getKey();
                    putAskPnl = pnls.getValue();
                }
                putBid = pair.put.getBidPrice();
                putAsk = pair.put.getAskPrice();
            }

            boolean atm = (null != atmStrike) && (Math.abs(atmStrike - pair.strike) < Util.ZERO);

            double parityPrice = 0.0;
            Instrument parityInstrument = new Instrument(Instrument.Type.PARITY, null, null, optionTerm.maturity, pair.strike);
            PricingResult parityResult = pricingModule.getPricingModel().price(parityInstrument);
            if (parityResult != null) {
                parityPrice = parityResult.price;
            }

            OptionsTableRow row = new OptionsTableRow(pair.strike,
                    callValue, callBid, callAsk,
                    putValue, putBid, putAsk,
                    (rows.size() % 2 == 0) ? EVEN_ROW_BACKGROUND_COLOR : ODD_ROW_BACKGROUND_COLOR,
                    atm,
                    parityPrice,
                    callBidPnl, callAskPnl,
                    putBidPnl, putAskPnl);

            rows.add(row);
        }

        Double forward = pricingModule.getForward(optionTerm.daysToExpiry);
        forwards.put(optionTerm.maturity, forward);
    }

    @Override
    public void init() {
        pricingModule.addListener(this);
        initLayout();
        super.init();
        processPricingUpdate();
    }

    @Override
    public void cleanup() {
        pricingModule.removeListener(this);
    }

    @Override
    public String getName() {
        return NAME;
    }

    private void initLayout() {
        JPanel container = new JPanel();
        add(container);

        container.setBorder(new EmptyBorder(10, 10, 10, 10));
        container.setLayout(new GridBagLayout());

        initTopPanel(container);
        initBottomPanel(container);
    }

    private void initTopPanel(JPanel parent) {
        GridBagConstraints topConstraints = new GridBagConstraints();
        topConstraints.gridx = 0;
        topConstraints.gridy = 0;
        topConstraints.weightx = 1.0;
        topConstraints.weighty = 0.0;
        topConstraints.fill = GridBagConstraints.BOTH;

        JPanel topPanel = new JPanel();
        parent.add(topPanel, topConstraints);

        JLabel termsLabel = new JLabel("Term: ");
        termsCombo = new JComboBox<>();

        termsCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                processItemStateChanged(e);
            }
        });

        topPanel.add(termsLabel);
        topPanel.add(termsCombo);

        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(dayLabel);

        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(spotLabel);

        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(forwardLabel);

        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(maturityLabel);
    }

    private void initBottomPanel(JPanel parent) {
        GridBagConstraints bottomConstraints = new GridBagConstraints();
        bottomConstraints.gridx = 0;
        bottomConstraints.gridy = 1;
        bottomConstraints.weightx = 1.0;
        bottomConstraints.weighty = 1.0;
        bottomConstraints.fill = GridBagConstraints.BOTH;

        model = new OptionsTableModel();
        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        parent.add(scrollPane, bottomConstraints);

        table.setDefaultRenderer(Object.class, new OptionsTableCellRenderer(model));

        JTableHeader header = table.getTableHeader();
        header.addMouseListener(new TableHeaderMouseListener());
    }

    private void processItemStateChanged(ItemEvent event) {
        int selected = termsCombo.getSelectedIndex();
        if (selected >= 0) {
            Day selectedMaturity = termsCombo.getItemAt(selected);
            model.selectMaturity(selectedMaturity);
            if (null != model.getDay()) {
                int maturity = model.getDay().countTradingDays(selectedMaturity);
                maturityLabel.setText(MATURITY_LABEL + maturity + " days");
            }

            Double forward = forwards.get(selectedMaturity);
            if (forward != null) {
                forwardLabel.setText(FORWARD_LABEL + Util.format(forward));
            } else {
                forwardLabel.setText(FORWARD_LABEL);
            }
        }
    }

    private void updateModel(GuiData data) {
        this.forwards = data.forwards;

        // day
        if (null != data.today) {
            dayLabel.setText(DAY_LABEL + data.today);
        } else {
            dayLabel.setText(DAY_LABEL);
        }

        // spot
        if (null != data.spot) {
            spotLabel.setText(SPOT_LABEL + Util.format(data.spot));
        } else {
            spotLabel.setText(SPOT_LABEL);
        }

        // term
        int selected = termsCombo.getSelectedIndex();
        if (selected < 0) {
            selected = 0; // Select first by default
        }
        termsCombo.removeAllItems();

        List<Day> maturities = new ArrayList<>(data.terms.keySet());
        for (Day maturity : maturities) {
            termsCombo.addItem(maturity);
        }

        if (maturities.size() > 0) {
            termsCombo.setSelectedIndex(selected);
        }

        Day selectedMaturity = (maturities.size() > 0) ? maturities.get(selected) : null;
        forwardLabel.setText(FORWARD_LABEL);
        if ((null != data.today) && (null != selectedMaturity)) {
            int maturity = data.today.countTradingDays(selectedMaturity);
            maturityLabel.setText(MATURITY_LABEL + maturity + " days");
            Double forward = forwards.get(selectedMaturity);
            if (forward != null) {
                forwardLabel.setText(FORWARD_LABEL + Util.format(forward));
            }
        }

        // options
        model.setDay(data.today);
        model.setTerms(data.terms);
        model.selectMaturity(selectedMaturity);
    }

    class TableHeaderMouseListener extends MouseAdapter {

        public void mouseClicked(MouseEvent event) {
            Point point = event.getPoint();
            int index = table.columnAtPoint(point);
            OptionsTableColumn column = model.getColumn(index);
            if (null == column) {
                return;
            }

            if (column instanceof StrikeColumn) {
                StrikeColumn strikeColumn = (StrikeColumn) column;
                strikeColumn.toggleType();
                model.fireTableStructureChanged();
            }
        }
    }

}
