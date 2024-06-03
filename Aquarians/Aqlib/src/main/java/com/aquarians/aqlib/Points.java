package com.aquarians.aqlib;

import com.aquarians.aqlib.math.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Discrete function given by a collection of (x,y) points
public class Points implements Function {

    private final List<Double> xs;
    private final List<Double> ys;

    public Points() {
        xs = new ArrayList<>();
        ys = new ArrayList<>();
    }

    public Points(int size) {
        xs = new ArrayList<>(size);
        ys = new ArrayList<>(size);
    }

    public Points(List<Double> xs, List<Double> ys) {
        if (xs.size() != ys.size()) {
            throw new RuntimeException("Size mismatch between xs and ys");
        }
        this.xs = new ArrayList<>(xs);
        this.ys = new ArrayList<>(ys);
    }

    public Points(Map<Double, Double> values) {
        int size = values.size();
        xs = new ArrayList<>(size);
        ys = new ArrayList<>(size);
        for (Map.Entry<Double, Double> entry : values.entrySet()) {
            xs.add(entry.getKey());
            ys.add(entry.getValue());
        }
    }

    public boolean isEmpty() {
        return xs.isEmpty();
    }

    public void clear() {
        xs.clear();
        ys.clear();
    }

    public List<Double> getXs() {
        return xs;
    }

    public void add(double x, double y) {
        xs.add(x);
        ys.add(y);
    }

    static class XY implements Comparable<XY> {
        final double x;
        final double y;

        XY(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(XY that) {
            return Double.compare(this.x, that.x);
        }
    }

    public int size() {
        return xs.size();
    }

    public double x(int index) {
        return xs.get(index);
    }

    public double y(int index) {
        return ys.get(index);
    }

    @Override
    public double value(double x) {
        double y = Util.interpolate(x, xs, ys);
        return y;
    }

    public double boundedValue(double x) {
        double xmin = xs.get(0);
        if (x <= xmin) {
            return ys.get(0);
        }

        double xmax = xs.get(xs.size() - 1);
        if (x >= xmax) {
            return ys.get(ys.size() - 1);
        }

        return value(x);
    }

    public static final class Builder {
        private final Map<Double, Double> values = new TreeMap<Double, Double>();

        public void add(double x, double y) {
            values.put(x, y);
        }

        public Points build() {
            return new Points(values);
        }
    }

    public void save(String file) {
        CsvFileWriter writer = null;
        try {
            writer = new CsvFileWriter(file);
            for (int i = 0; i < size(); i++) {
                writer.write(Util.newArrayList(Util.FOUR_DIGIT_FORMAT.format(x(i)), Util.FOUR_DIGIT_FORMAT.format(y(i))));
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public void load(String file) {
        CsvFileReader reader = null;
        try {
            reader = new CsvFileReader(file);
            String[] line;
            while (null != (line = reader.readRecord())) {
                if (line.length == 2) {
                    double x = Double.parseDouble(line[0]);
                    double y = Double.parseDouble(line[1]);
                    xs.add(x);
                    ys.add(y);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public Points inverse() {
        List<Double> xs = new ArrayList<>(this.xs.size());
        List<Double> ys = new ArrayList<>(this.ys.size());
        for (int i = 0; i < size(); i++) {
            xs.add(y(i));
            ys.add(x(i));
        }

        return new Points(xs, ys);
    }

}
