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
 * The Point class was added in version 1 of the application and it contained 2D info: x and y coordinates.
 * In version 2 of the application, a new coordinate z was introduced, so the point can represent 3D locations.
 */
public class Point implements Shape {

    public static final String TYPE = "Point";

    // These members were existing since version 1 of Point class
    private double x;
    private double y;

    // This member was added in version 2 of Point class
    private double z;

    // Protected constructor needed by serialization
    protected Point() {
    }

    // Version 1 constructor
    // The place where the "immutable" members can be set up
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // Version 2 constructor
    // The place where the "immutable" members can be set up
    public Point(double x, double y, double z) {
        this(x, y);
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    @Override
    public AqSerializable createInstance() {
        return new Point();
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
        //buffer.append("Point(" + x + "," + y + ")\n");
        // Version 2
        //buffer.append("Point(" + x + "," + y + "," + z + ")\n");
    }

    private void internalWriteTo(WriteArchive archive) {
        // Since version 1
        archive.writeDouble("x", x);
        archive.writeDouble("y", y);
        // Since version 2
        archive.writeDouble("z", z);
    }

    private void internalReadFrom(ReadArchive archive) {
        // Since version 1
        x = archive.readDouble("x");
        y = archive.readDouble("y");
        // Since version 2
        if (archive.hasMoreData("z")) {
            z = archive.readDouble("z");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Point)) {
            return false;
        }

        Point that = (Point) other;
        return (this.x == that.x) &&
                (this.y == that.y) &&
                (this.z == that.z);
    }
}
