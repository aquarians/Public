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

import com.aquarians.aqlib.Util;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;

public abstract class MdiFrame extends JInternalFrame {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(MdiFrame.class);

    private final MainFrame owner;

    public MdiFrame(String title, MainFrame owner) {
        super(title,
                true, // resizable
                true, // closable
                true, // maximizable
                true); // iconifiable

        this.owner = owner;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addInternalFrameListener(new MyInternalFrameAdapter());
    }

    public Dimension getMinimumSize() {
        Dimension minSize = super.getMinimumSize();
        if ((null != minSize) || (minSize.getWidth() == 0) || (minSize.getHeight() == 0)) {
            return minSize;
        }

        return new Dimension(320, 200);
    }

    public void init() {
        pack();

        Dimension minSize = getMinimumSize();
        if (null != minSize) {
            setMinimumSize(minSize);
        }

        setVisible(true);
    }

    private final class MyInternalFrameAdapter extends InternalFrameAdapter {
        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            cleanup();
            super.internalFrameClosed(e);
        }
    }

    public void cleanup() {

    }

    public abstract String getName();

    protected JFreeChart createChart(XYDataset dataset, String ctitle, String xtitle, String ytitle) {
        // Create the chart...
        final JFreeChart chart = ChartFactory.createXYLineChart(
                ctitle, // chart title
                xtitle, // x axis label
                ytitle, // y axis label
                dataset, // data
                PlotOrientation.VERTICAL,
                true, // include legend
                true, // tooltips
                false // urls
        );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        chart.setBackgroundPaint(Color.white);

        // get a reference to the plot for further customisation...
        final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        return chart;
    }

    protected JFreeChart createTimeChart(XYDataset dataset, String ctitle, String xtitle, String ytitle) {
        // Create the chart
        final JFreeChart chart = ChartFactory.createTimeSeriesChart(
                ctitle, // chart title
                xtitle, // x axis label
                ytitle, // y axis label
                dataset, // data
                true, // include legend
                true, // tooltips
                false // urls
        );

        // Change background color
        chart.setBackgroundPaint(Color.white);

        // Finished
        return chart;
    }

    protected static void adjustRange(JFreeChart chart, XYSeries series) {
        Double xmin = null;
        Double xmax = null;
        Double ymin = null;
        Double ymax = null;
        for (int i = 0; i < series.getItemCount(); i++) {
            Double x = (Double) series.getX(i);
            Double y = (Double) series.getY(i);
            if ((null == xmin) || (x < xmin)) {
                xmin = x;
            }
            if ((null == ymin) || (y < ymin)) {
                ymin = y;
            }
            if ((null == xmax) || (x > xmax)) {
                xmax = x;
            }
            if ((null == ymax) || (y > ymax)) {
                ymax = y;
            }
        }

        if ((null == xmin) || (null == ymin)) {
            return;
        }

        XYPlot xyPlot = (XYPlot) chart.getPlot();

        double dx = xmax - xmin;
        double dx_extra = xmax / 10.0;
        if (dx > Util.ZERO) {
            dx_extra = dx / 10.0;
        }
        xyPlot.getDomainAxis().setRange(xmin - dx_extra, xmax + dx_extra);

        double dy = ymax - ymin;
        double dy_extra = ymax / 10.0;
        if (dy > Util.ZERO) {
            dy_extra = dy / 10.0;
        }
        xyPlot.getRangeAxis().setRange(ymin - dy_extra, ymax + dy_extra);
    }

}
