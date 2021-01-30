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
import com.aquarians.backtester.Application;
import com.aquarians.backtester.marketdata.MarketDataControl;
import com.aquarians.backtester.marketdata.MarketDataListener;
import com.aquarians.backtester.marketdata.MarketDataModule;
import org.jdesktop.swingx.JXDatePicker;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Calendar;
import java.util.Locale;

public class MarketDataControlFrame extends MdiFrame implements MarketDataControl.Listener {

    public static final String NAME = "Market Data Control";

    private JXDatePicker currentDayPicker;
    private JComboBox<String> playbackModeCombo;
    private JButton startButton;
    private JButton nextButton;
    private JButton stopButton;
    private JButton resetButton;

    public MarketDataControlFrame(MainFrame owner) {
        super(NAME, owner);
    }

    @Override
    public void init() {
        initLayout();
        super.init();
        MarketDataControl.getInstance().setListener(this);
    }

    @Override
    public void cleanup() {
        MarketDataControl.getInstance().resetListener();
    }

    @Override
    public String getName() {
        return NAME;
    }

    private void initLayout() {
        JPanel parent = new JPanel();
        add(parent);

        JPanel container = new JPanel();
        container.setBorder(new EmptyBorder(10, 10, 10, 10));
        parent.add(container);

        container.setLayout(new GridBagLayout());

        addStartDay(container);
        addEndDay(container);
        addCurrentDay(container);
        addPlaybackMode(container);
        addSeparator(container);
        addControlButtons1(container);
        addControlButtons2(container);

        startButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                startButtonClicked();
            }
        });

        nextButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                nextButtonClicked();
            }
        });

        resetButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                resetButtonClicked();
            }
        });

        stopButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                stopButtonClicked();
            }
        });

        currentDayPicker.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentDayChanged();
            }
        });

        updateGUI();
    }

    private void updateGUI() {
        if (MarketDataControl.getInstance().isStartRequested()) {
            // Playback in progress
            currentDayPicker.setEnabled(false);
            playbackModeCombo.setEnabled(false);
            startButton.setEnabled(false);
            nextButton.setEnabled(MarketDataControl.getInstance().getPlaybackMode().equals(MarketDataControl.PlaybackMode.SingleStep));
            resetButton.setEnabled(false);
            stopButton.setEnabled(true);
        } else {
            // Playback not running
            currentDayPicker.setEnabled(true);
            playbackModeCombo.setEnabled(true);
            startButton.setEnabled(true);
            nextButton.setEnabled(false);
            resetButton.setEnabled(!MarketDataControl.getInstance().getCurrentDay().equals(MarketDataControl.getInstance().getStartDay()));
            stopButton.setEnabled(false);
        }
    }

    private void addStartDay(JPanel container) {
        JLabel label = new JLabel("Start Day:");
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.weightx = 0.25;
        container.add(label, labelConstraints);

        JTextField text = new JTextField(MarketDataControl.getInstance().getStartDay().toString());
        text.setEnabled(false);

        GridBagConstraints textConstraints = new GridBagConstraints();
        textConstraints.gridx = 1;
        textConstraints.gridy = 0;
        textConstraints.fill = GridBagConstraints.HORIZONTAL;
        textConstraints.weightx = 0.75;
        container.add(text, textConstraints);
    }

    private void addEndDay(JPanel container) {
        JLabel label = new JLabel("End Day:");
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 1;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.weightx = 0.25;
        container.add(label, labelConstraints);

        JTextField text = new JTextField(MarketDataControl.getInstance().getEndDay().toString());
        text.setEnabled(false);

        GridBagConstraints textConstraints = new GridBagConstraints();
        textConstraints.gridx = 1;
        textConstraints.gridy = 1;
        textConstraints.fill = GridBagConstraints.HORIZONTAL;
        textConstraints.weightx = 0.75;
        container.add(text, textConstraints);
    }

    private void addCurrentDay(JPanel container) {
        JLabel label = new JLabel("Current Day:");
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 2;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.weightx = 0.25;
        container.add(label, labelConstraints);

        currentDayPicker = new JXDatePicker();
        currentDayPicker.setLocale(Locale.US);
        currentDayPicker.setFormats(Day.DEFAULT_FORMAT);
        currentDayPicker.setDate(MarketDataControl.getInstance().getCurrentDay().toCalendar().getTime());
        currentDayPicker.getEditor().setEditable(true);
        currentDayPicker.getMonthView().setForeground(Color.BLACK);

        GridBagConstraints pickerConstraints = new GridBagConstraints();
        pickerConstraints.gridx = 1;
        pickerConstraints.gridy = 2;
        pickerConstraints.fill = GridBagConstraints.HORIZONTAL;
        pickerConstraints.weightx = 0.75;
        container.add(currentDayPicker, pickerConstraints);
    }

    private void addPlaybackMode(JPanel container) {
        JLabel label = new JLabel("Playback Mode:");
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 3;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.weightx = 0.25;
        container.add(label, labelConstraints);

        String[] playbackModes = new String[MarketDataControl.PlaybackMode.values().length];
        int selectedPlaybackMode = -1;
        for (int i = 0; i < playbackModes.length; i++) {
            MarketDataControl.PlaybackMode mode = MarketDataControl.PlaybackMode.values()[i];
            playbackModes[i] = mode.caption;
            if (mode.equals(MarketDataControl.getInstance().getPlaybackMode())) {
                selectedPlaybackMode = i;
            }
        }

        playbackModeCombo = new JComboBox<>(playbackModes);
        playbackModeCombo.setSelectedIndex(selectedPlaybackMode);
        playbackModeCombo.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playbackModeChanged();
            }
        });

        GridBagConstraints comboConstraints = new GridBagConstraints();
        comboConstraints.gridx = 1;
        comboConstraints.gridy = 3;
        comboConstraints.fill = GridBagConstraints.HORIZONTAL;
        comboConstraints.weightx = 0.75;
        container.add(playbackModeCombo, comboConstraints);
    }

    private void addSeparator(JPanel container) {
        JLabel separator = new JLabel();
        GridBagConstraints separatorConstraints = new GridBagConstraints();
        separatorConstraints.gridx = 0;
        separatorConstraints.gridy = 4;
        separatorConstraints.gridwidth = 2;
        separatorConstraints.fill = GridBagConstraints.HORIZONTAL;
        separatorConstraints.weightx = 1.0;
        separatorConstraints.ipady = 10;
        container.add(separator, separatorConstraints);
    }

    private void addControlButtons1(JPanel container) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 3));

        GridBagConstraints panelConstraints = new GridBagConstraints();
        panelConstraints.gridx = 1;
        panelConstraints.gridy = 5;
        panelConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelConstraints.weightx = 0.25;
        container.add(panel, panelConstraints);

        startButton = new JButton("Start");
        panel.add(startButton);

        stopButton = new JButton("Stop");
        panel.add(stopButton);
    }

    private void addControlButtons2(JPanel container) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 3));

        GridBagConstraints panelConstraints = new GridBagConstraints();
        panelConstraints.gridx = 1;
        panelConstraints.gridy = 6;
        panelConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelConstraints.weightx = 0.25;
        container.add(panel, panelConstraints);

        nextButton = new JButton("Next");
        panel.add(nextButton);

        resetButton = new JButton("Reset");
        panel.add(resetButton);
    }

    private void startButtonClicked() {
        MarketDataControl.getInstance().requestStart();
    }

    private void playbackModeChanged() {
        int index = playbackModeCombo.getSelectedIndex();
        MarketDataControl.PlaybackMode playbackMode = MarketDataControl.PlaybackMode.values()[index];
        MarketDataControl.getInstance().setPlaybackMode(playbackMode);
    }

    private void stopButtonClicked() {
        MarketDataControl.getInstance().requestStop();
    }

    private void nextButtonClicked() {
        MarketDataControl.getInstance().requestNext();
    }

    private void currentDayChanged() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDayPicker.getDate());
        MarketDataControl.getInstance().setCurrentDay(new Day(calendar));
        updateGUI();
    }

    private void resetButtonClicked() {
        MarketDataControl.getInstance().setCurrentDay(MarketDataControl.getInstance().getStartDay());
        currentDayPicker.setDate(MarketDataControl.getInstance().getCurrentDay().toCalendar().getTime());
        updateGUI();
    }

    @Override
    public void playbackStarted() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateGUI();
            }
        });
    }

    @Override
    public void playbackRunning(Day day) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentDayPicker.setDate(day.toCalendar().getTime());
            }
        });
    }

    @Override
    public void playbackEnded() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateGUI();
            }
        });
    }
}
