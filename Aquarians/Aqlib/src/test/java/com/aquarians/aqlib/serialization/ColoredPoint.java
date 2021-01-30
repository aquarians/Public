/*
    MIT License

    Copyright (c) 2016 Mihai Bunea

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

package com.aquarians.aqlib.serialization;

/**
 * Version 3 of the application introduced color, but as a hierarchical concept rather than
 * composition (adding a new mamber to the Point class).
 */
public class ColoredPoint extends Point {

    public static final String TYPE = "ColoredPoint";
    public static final ColoredPoint PROTOTYPE = new ColoredPoint();

    // 16 bit R+G+B+transparency or whatever
    private long color;

    // Protected constructor needed by serialization
    protected ColoredPoint() {
    }

    public ColoredPoint(double x, double y) {
        super(x, y);
    }

    public ColoredPoint(double x, double y, double z) {
        super(x, y, z);
    }

    public ColoredPoint(double x, double y, double z, long color) {
        super(x, y, z);
        this.color = color;
    }

    @Override
    public void draw(StringBuilder buffer) {
        // Version 3
        buffer.append("Point(" + getX() + "," + getY() + "," + getZ() + "," + color + ")\n");
    }

    @Override
    public AqSerializable createInstance() {
        return new ColoredPoint();
    }

    @Override
    public void getTypeHierarchy(StringBuilder types) {
        types.append(TYPE).append(",");
        super.getTypeHierarchy(types);
    }

    // Boilerplate code
    @Override
    public void writeTo(WriteArchive archive) {
        // Superclass must go first
        super.writeTo(archive);
        // Then subclass
        archive.beginTransaction(TYPE);
        internalWriteTo(archive);
        archive.endTransaction();
    }

    // Boilerplate code
    @Override
    public void readFrom(ReadArchive archive) {
        // Superclass must go first
        super.readFrom(archive);
        // Then subclass
        if (archive.hasMoreData(TYPE)) {
            archive.beginTransaction(TYPE);
            internalReadFrom(archive);
            archive.endTransaction();
        }
    }

    private void internalWriteTo(WriteArchive archive) {
        // Since version 1 of this class (version 3 of the application)
        archive.writeLong("color", color);
    }

    private void internalReadFrom(ReadArchive archive) {
        // Since version 1 of this class (version 3 of the application)
        color = archive.readLong("color");
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        if (!(other instanceof ColoredPoint)) {
            return false;
        }

        ColoredPoint that = (ColoredPoint) other;
        return (this.color == that.color);
    }

}
