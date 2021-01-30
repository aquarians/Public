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

import com.aquarians.aqlib.serialization.ReadArchive;
import com.aquarians.aqlib.serialization.XmlReadArchive;
import com.aquarians.aqlib.serialization.XmlWriteArchive;
import com.aquarians.backtester.Application;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

public class MainFrame extends javax.swing.JFrame
                       implements ActionListener {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(MainFrame.class);

    private static final String FILE_MENU_SAVE_LAYOUT = "Save Layout";
    private static final String VIEW_MENU_MARKET_DATA_CONTROL = "Market Data Control";
    private static final String VIEW_MENU_OPTIONS = "Options";
    private static final String VIEW_MENU_VOLATILITY = "Volatility";
    private static final String GUI_CONFIG = "GUI.Config";

    private JDesktopPane desktopPane;

    public MainFrame() {
        javax.swing.JFrame.setDefaultLookAndFeelDecorated(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new MyWindowAdapter());

        desktopPane = new JDesktopPane();
        getContentPane().add(desktopPane);
        desktopPane.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        desktopPane.setBackground(Color.WHITE);

        setJMenuBar(createMenuBar());
    }

    public void init() {
        ReadArchive archive = null;
        File file = new File(Application.getInstance().getProperties().getProperty(GUI_CONFIG));
        if (file.exists()) {
            try {
                archive = new XmlReadArchive(file, null);
                loadLayout(archive);
            } catch (Exception ex) {
                logger.warn(ex.getMessage(), ex);
            }
        } else {
            setSize(800, 600);
        }
    }

    public void cleanup() {
        JInternalFrame[] frames = desktopPane.getAllFrames();
        for (int i = 0; i < frames.length; i++) {
            JInternalFrame internalFrame = frames[i];
            if (!(internalFrame instanceof MdiFrame)) {
                continue;
            }

            MdiFrame mdiFrame = (MdiFrame) internalFrame;
            mdiFrame.cleanup();
        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenu viewsMenu = new JMenu("Views");

        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.setMnemonic(KeyEvent.VK_V);

        menuBar.add(fileMenu);
        menuBar.add(viewsMenu);

        setupFileMenu(fileMenu);
        setupViewsMenu(viewsMenu);

        return menuBar;
    }

    private void setupFileMenu(JMenu menu) {
        JMenuItem saveLayoutItem = new JMenuItem(FILE_MENU_SAVE_LAYOUT);
        saveLayoutItem.setMnemonic(KeyEvent.VK_S);
        saveLayoutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
        saveLayoutItem.setActionCommand(FILE_MENU_SAVE_LAYOUT);
        saveLayoutItem.addActionListener(this);

        menu.add(saveLayoutItem);
    }

    private void setupViewsMenu(JMenu menu) {
        JMenuItem marketDataControlItem = new JMenuItem(VIEW_MENU_MARKET_DATA_CONTROL);
        marketDataControlItem.setMnemonic(KeyEvent.VK_M);
        marketDataControlItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.ALT_MASK));
        marketDataControlItem.setActionCommand(VIEW_MENU_MARKET_DATA_CONTROL);
        marketDataControlItem.addActionListener(this);
        menu.add(marketDataControlItem);

        JMenuItem optionTermsItem = new JMenuItem(VIEW_MENU_OPTIONS);
        optionTermsItem.setMnemonic(KeyEvent.VK_O);
        optionTermsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK));
        optionTermsItem.setActionCommand(VIEW_MENU_OPTIONS);
        optionTermsItem.addActionListener(this);
        menu.add(optionTermsItem);

        JMenuItem volatilityItem = new JMenuItem(VIEW_MENU_VOLATILITY);
        volatilityItem.setMnemonic(KeyEvent.VK_V);
        volatilityItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK));
        volatilityItem.setActionCommand(VIEW_MENU_VOLATILITY);
        volatilityItem.addActionListener(this);
        menu.add(volatilityItem);
    }

    private class MyWindowAdapter extends java.awt.event.WindowAdapter {
        public void windowClosing(java.awt.event.WindowEvent e) {
            if (logger.isDebugEnabled()) logger.debug("windowClosing enter");

            try {
                Application.getInstance().requestStop();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }

            if (logger.isDebugEnabled()) logger.debug("windowClosing leave");
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            handleActionPerformed(event);
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
        }
    }

    private void handleActionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals(FILE_MENU_SAVE_LAYOUT)) {
            fileSaveLayout();
        } else if (event.getActionCommand().equals(VIEW_MENU_MARKET_DATA_CONTROL)) {
            viewMarketDataControl();
        } else if (event.getActionCommand().equals(VIEW_MENU_OPTIONS)) {
            viewOptionTerms();
        } else if (event.getActionCommand().equals(VIEW_MENU_VOLATILITY)) {
            viewVolatility();
        }
    }

    private void fileSaveLayout() {
        XmlWriteArchive archive = new XmlWriteArchive();
        saveLayout(archive);
        archive.store(Application.getInstance().getProperties().getProperty(GUI_CONFIG));
    }

    private void saveLayout(XmlWriteArchive archive) {
        Dimension size = getSize();
        archive.writeInt("width", size.width);
        archive.writeInt("height", size.height);

        JInternalFrame[] frames = desktopPane.getAllFrames();
        archive.writeInt("frames", frames.length);
        for (int i = 0; i < frames.length; i++) {
            JInternalFrame internalFrame = frames[i];
            if (!(internalFrame instanceof MdiFrame)) {
                continue;
            }

            MdiFrame mdiFrame = (MdiFrame) internalFrame;
            archive.writeString("frame." + i + ".name", mdiFrame.getName());
            archive.writeInt("frame." + i + ".x", mdiFrame.getX());
            archive.writeInt("frame." + i + ".y", mdiFrame.getY());
            archive.writeInt("frame." + i + ".width", mdiFrame.getWidth());
            archive.writeInt("frame." + i + ".height", mdiFrame.getHeight());
        }
    }

    private void loadLayout(ReadArchive archive) {
        int width = 800;
        int height = 600;
        if (archive != null) {
            width = archive.readInt("width");
            height = archive.readInt("height");
        }
        setSize(width, height);

        if (archive.hasMoreData("frames")) {
            int frames = archive.readInt("frames");
            for (int i = 0; i < frames; i++) {
                String name = archive.readString("frame." + i + ".name");
                MdiFrame frame = createFrame(name);
                if (null == frame) {
                    continue;
                }

                frame.init();
                int x = archive.readInt("frame." + i + ".x");
                int y = archive.readInt("frame." + i + ".y");
                width = archive.readInt("frame." + i + ".width");
                height = archive.readInt("frame." + i + ".height");
                frame.setLocation(x, y);
                frame.setSize(width, height);
                desktopPane.add(frame);
            }
        }
    }

    private MdiFrame createFrame(String name) {
        if (name.equals(MarketDataControlFrame.NAME)) {
            return new MarketDataControlFrame(this);
        } else if (name.equals(OptionsFrame.NAME)) {
            return new OptionsFrame(this);
        } else if (name.equals(VolatilityFrame.NAME)) {
            return new VolatilityFrame(this);
        }
        return null;
    }

    private void viewMarketDataControl() {
        MarketDataControlFrame frame = new MarketDataControlFrame(this);
        frame.init();
        desktopPane.add(frame);
    }

    private void viewOptionTerms() {
        OptionsFrame frame = new OptionsFrame(this);
        frame.init();
        desktopPane.add(frame);
    }

    private void viewVolatility() {
        VolatilityFrame frame = new VolatilityFrame(this);
        frame.init();
        desktopPane.add(frame);
    }
}
