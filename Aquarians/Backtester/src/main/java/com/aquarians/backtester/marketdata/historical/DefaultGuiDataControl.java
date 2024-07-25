/*
    MIT License

    Copyright (c) 2024 Mihai Bunea

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

package com.aquarians.backtester.marketdata.historical;

import com.aquarians.aqlib.Day;

public class DefaultGuiDataControl implements GuiDataControl {

    @Override
    public boolean isStartRequested() {
        return false;
    }

    @Override
    public PlaybackMode getPlaybackMode() {
        return null;
    }

    @Override
    public void resetListener() {

    }

    @Override
    public void setListener(Listener listener) {

    }

    @Override
    public Day getCurrentDay() {
        return Day.now();
    }

    @Override
    public Day getStartDay() {
        return Day.now();
    }

    @Override
    public Day getEndDay() {
        return Day.now();
    }

    @Override
    public void requestStart() {

    }

    @Override
    public void setPlaybackMode(PlaybackMode playbackMode) {

    }

    @Override
    public void requestStop() {

    }

    @Override
    public void requestNext() {

    }

    @Override
    public void setCurrentDay(Day currentDay) {

    }
}
