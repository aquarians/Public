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

import com.aquarians.aqlib.ApplicationModule;
import com.aquarians.aqlib.Ref;

public class GuiModule implements ApplicationModule {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(GuiModule.class);

    public static final String NAME = "GUI";

    private MainFrame mainFrame;

    public GuiModule() {
        if (logger.isDebugEnabled()) logger.debug("Module " + NAME + " : created");
    }

    @Override
    public void init() {
        // Create the GUI
        final Ref<Boolean> created = new Ref(false);
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        createAndShowGUI();
                        created.value = true;
                    } catch (Exception ex) {
                        logger.warn(ex.getMessage(), ex);
                    }
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }

        if (!created.value) {
            throw new RuntimeException("Failed to create GUI");
        }
    }

    @Override
    public void cleanup() {
        // Destroy the main frame
        mainFrame.cleanup();
        mainFrame.setVisible(false);
        mainFrame.dispose();
    }

    private void createAndShowGUI() throws Exception {
        // Set look and feel
        javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());

        // Create the main frame
        mainFrame = new MainFrame();
        mainFrame.init();

        // Show the GUI
        mainFrame.setVisible(true);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
