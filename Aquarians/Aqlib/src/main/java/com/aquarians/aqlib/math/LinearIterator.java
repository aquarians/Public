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

public class LinearIterator implements NumberIterator {

    private final double start;
    private final double end;
    private final int count;

    private final double step;
    private int position;

    public LinearIterator(double start, double end, int count) {
        this.start = start;
        this.end = end;
        count = Math.max(count, 1);
        step = (end - start) / count;
        this.count = (start != end) ? count : 0;
        position = 0;
    }

    public boolean isFirst() {
        return 0 == position;
    }

    public boolean isLast() {
        return count == position;
    }

    public double getStep() {
        return step;
    }

    public boolean hasNext() {
        return position > count ? false : true;
    }

    @Override
    public Double next() {
        double value = start + position * step;
        position++;
        return value;
    }

    @Override
    public void remove() {}

    @Override
    public void reset() {
        position = 0;
    }
}
