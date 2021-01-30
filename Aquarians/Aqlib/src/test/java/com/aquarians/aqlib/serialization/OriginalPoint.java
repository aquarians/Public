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
 * Original version of the Point class, from version 1.
 * Because this is an unit test, we can't have two classes called "Point"
 * with different implementations, as we would have in version 1 JAR
 * and version 2 JAR.
 */
public class OriginalPoint implements Shape {

    public static final String TYPE = "Point";
    public static final OriginalPoint PROTOTYPE = new OriginalPoint();

    // These members were existing since version 1 of Point class
    private double x;
    private double y;

    // Protected constructor needed by serialization
    protected OriginalPoint() {
    }

    // Version 1 constructor
    // The place where the "immutable" members can be set up
    public OriginalPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public AqSerializable createInstance() {
        return new OriginalPoint();
    }

    @Override
    public void getTypeHierarchy(StringBuilder types) {
        types.append(TYPE);
    }

    // Boilerplate code
    @Override
    public void writeTo(WriteArchive archive) {
        archive.beginTransaction(TYPE);
        internalWriteTo(archive);
        archive.endTransaction();
    }

    // Boilerplate code
    @Override
    public void readFrom(ReadArchive archive) {
        archive.beginTransaction(TYPE);
        internalReadFrom(archive);
        archive.endTransaction();
    }

    @Override
    public void draw(StringBuilder buffer) {
        // Version 1
        buffer.append("Point(" + x + "," + y + ")\n");
    }

    private void internalWriteTo(WriteArchive archive) {
        // Since version 1
        archive.writeDouble("x", x);
        archive.writeDouble("y", y);
    }

    private void internalReadFrom(ReadArchive archive) {
        // Since version 1
        x = archive.readDouble("x");
        y = archive.readDouble("y");
    }

    // Not the Object's equals but a custom one
    public boolean equals(Point that) {
        return (this.x == that.getX()) && (this.y == that.getY());
    }

}
