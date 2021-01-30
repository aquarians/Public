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

package com.aquarians.aqlib.math;

import com.aquarians.aqlib.Pair;
import com.aquarians.aqlib.Ref;
import com.aquarians.aqlib.Util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GaussianQuadrature implements Quadrature {

    public static final GaussianQuadrature QUADRATURE_3 = new GaussianQuadrature(
            Util.newArrayList(8.0 / 9.0, 5.0 / 9.0, 5.0 / 9.0),
            Util.newArrayList(0.0, Math.sqrt(3.0 / 5.0), -Math.sqrt(3.0 / 5.0)));

    public static final GaussianQuadrature QUADRATURE_5 = build5PointQuadrature();

    private final List<Double> weights;
    private final List<Double> points;

    protected GaussianQuadrature(List<Double> weights, List<Double> points) {
        if (weights.size() != points.size()) {
            throw new RuntimeException("Size mismatch between weights and points");
        }
        this.weights = weights;
        this.points = points;
    }

    private static GaussianQuadrature build5PointQuadrature() {
        double wp = (322.0 + 13.0 * Math.sqrt(70.0)) / 900.0;
        double wn = (322.0 - 13.0 * Math.sqrt(70.0)) / 900.0;
        List<Double> weights = Util.newArrayList(128.0 / 225.0, wp, wp, wn, wn);
        double xp = Math.sqrt(5.0 - 2.0 * Math.sqrt(10.0 / 7.0)) / 3.0;
        double xn = Math.sqrt(5.0 + 2.0 * Math.sqrt(10.0 / 7.0)) / 3.0;
        List<Double> points = Util.newArrayList(0.0, xp, -xp, xn, -xn);
        return new GaussianQuadrature(weights, points);
    }

    public double area(Function f, double a, double b, int count) {
        double total = 0.0;
        Iterator<Double> it = new LinearIterator(a, b, count);
        Double xprev = null;
        while (it.hasNext()) {
            double xcur = it.next();
            if (null != xprev) {
                total += area(f, xprev, xcur);
            }
            xprev = xcur;
        }
        return total;
    }

    public List<Pair<Double, Double>> getWeightList(double a, double b) {
        List<Pair<Double, Double>> pairs = new ArrayList<>(weights.size());
        double diff = (b - a) / 2.0;
        double sum = (b + a) / 2.0;
        for (int i = 0; i < weights.size(); ++i) {
            double w = weights.get(i) * diff;
            double x = diff * points.get(i) + sum;
            pairs.add(new Pair<>(w, x));
        }
        return pairs;
    }

    @Override
    public double area(Function f, double a, double b) {
        double approx = 0.0;
        double diff = (b - a) / 2.0;
        double sum = (b + a) / 2.0;
        for (int i = 0; i < weights.size(); ++i) {
            double w = weights.get(i) * diff;
            double x = diff * points.get(i) + sum;
            double y = f.value(x);
            approx += w * y;
        }
        return approx;
    }

    public double area(Function f, double a, double b,
                       Ref<List<Double>> xs,
                       Ref<List<Double>> ws,
                       Ref<List<Double>> ys) {
        sample(a, b, xs, ws);

        double approx = 0.0;
        ys.value = new ArrayList<Double>(xs.value.size());
        for (int i = 0; i < xs.value.size(); ++i) {
            double x = xs.value.get(i);
            double w = ws.value.get(i);
            double y = f.value(x);
            approx += w * y;
            ys.value.add(y);
        }
        return approx;
    }


    // Samples interval [a, b] into points [x1, x2, ... xn] of weights [w1, w2, ... wn]
    public void sample(double a, double b, Ref<List<Double>> xs, Ref<List<Double>> ws) {
        int size = weights.size();
        xs.value = new ArrayList<Double>(size);
        ws.value = new ArrayList<Double>(size);
        double diff = (b - a) / 2.0;
        double sum = (b + a) / 2.0;
        for (int i = 0; i < size; ++i) {
            double x = diff * points.get(i) + sum;
            double w = weights.get(i) * diff;
            xs.value.add(x);
            ws.value.add(w);
        }
    }

}
