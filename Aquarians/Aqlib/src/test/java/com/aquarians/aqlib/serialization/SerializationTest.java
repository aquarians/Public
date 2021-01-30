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

import org.junit.Assert;
import org.junit.BeforeClass;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class SerializationTest {

    // Version 1 and 2 of the application (just the Point class, no ColoredPoint yet)
    protected static ObjectFactory oldFactory;

    // Version 1 and 2 of the application (just the Point class, no ColoredPoint yet)
    protected static ObjectFactory newFactory;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        oldFactory = new DefaultObjectFactory();
        oldFactory.registerPrototype(OriginalPoint.TYPE, OriginalPoint.PROTOTYPE);

        newFactory = new DefaultObjectFactory();
        newFactory.registerPrototype(Point.TYPE, ColoredPoint.PROTOTYPE);
        newFactory.registerPrototype(ColoredPoint.TYPE, ColoredPoint.PROTOTYPE);
    }

    public abstract WriteArchive createWriteArchive();

    public abstract ReadArchive createReadArchive(ObjectFactory factory, WriteArchive writeArchive);

    // Test version 1 serialized data being deserialized from version 3 application
    protected void testForwardCompatibility() {
        // Serialize in version 1 as a "Point"
        OriginalPoint oldPoint = new OriginalPoint(1, 2);
        WriteArchive writeArchive = createWriteArchive();
        writeArchive.writeObject("point", oldPoint);

        // Deserialize in version 3 as a "ColoredPoint"
        ReadArchive readArchive = createReadArchive(newFactory, writeArchive);
        ColoredPoint newPoint = (ColoredPoint) readArchive.readObject("point");

        // Check that data concides
        Assert.assertTrue(oldPoint.equals(newPoint));
    }

    // Test version 3 serialized data being deserialized from version 1 application
    protected void testBackwardCompatibility() {
        // Serialize in version 3 as a "ColoredPoint"
        ColoredPoint newPoint = new ColoredPoint(1, 2, 3, 4);
        WriteArchive writeArchive = createWriteArchive();
        writeArchive.writeObject("point", newPoint);

        // Deserialize in version 1 as a "Point"
        ReadArchive readArchive = createReadArchive(oldFactory, writeArchive);
        OriginalPoint oldPoint = (OriginalPoint) readArchive.readObject("point");

        // Check that data coincides
        Assert.assertTrue(oldPoint.equals(newPoint));
    }

    // Test speed vs Java serialization
    protected void testSpeed() throws Exception {
        //int SIZE = 500000;
        int SIZE = 5000;
        List<Shape> shapes = new ArrayList<Shape>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            Shape shape = new ColoredPoint(i, i, i, i);
            shapes.add(shape);
        }

        // Java serialization
        long start = System.currentTimeMillis();
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(outputBuffer);
        outputStream.writeObject(shapes);
        long end = System.currentTimeMillis();
        System.out.println("Java serialization elapsed: " + (end - start));

        // Java deserialization
        outputStream.flush();
        ByteArrayInputStream inputBuffer = new ByteArrayInputStream(outputBuffer.toByteArray());
        ObjectInputStream inputStream = new ObjectInputStream(inputBuffer);
        start = System.currentTimeMillis();
        List<Shape> javaShapes = (List<Shape>) inputStream.readObject();
        end = System.currentTimeMillis();
        System.out.println("Java deserialization elapsed: " + (end - start));

        // Test that the Java deserialized data coincides with the serialized one
        Assert.assertEquals(shapes.size(), javaShapes.size());
        for (int i = 0; i < shapes.size(); i++) {
            Shape original = shapes.get(i);
            Shape deserialized = javaShapes.get(i);
            Assert.assertEquals(original, deserialized);
        }

        // Aquarians serialization
        start = System.currentTimeMillis();
        WriteArchive writeArchive = createWriteArchive();
        writeArchive.writeInt("shapes.size", shapes.size());
        for (int i = 0; i < shapes.size(); i++) {
            Shape shape = shapes.get(i);
            writeArchive.writeObject("shape." + i, shape);
        }
        end = System.currentTimeMillis();
        System.out.println(getClass().getSimpleName() + ": Aquarians serialization elapsed: " + (end - start));

        // Aquarians deserialization
        ReadArchive readArchive = createReadArchive(newFactory, writeArchive);
        int size = readArchive.readInt("shapes.size");
        List<Shape> aquariansShapes = new ArrayList<Shape>(size);
        for (int i = 0; i < size; i++) {
            Shape shape = (Shape) readArchive.readObject("shape." + i);
            aquariansShapes.add(shape);
        }
        end = System.currentTimeMillis();
        System.out.println(getClass().getSimpleName() + ": Aquarians deserialization elapsed: " + (end - start));

        // Test that the Aquarians deserialized data coincides with the serialized one
        Assert.assertEquals(shapes.size(), aquariansShapes.size());
        for (int i = 0; i < shapes.size(); i++) {
            Shape original = shapes.get(i);
            Shape deserialized = aquariansShapes.get(i);
            Assert.assertEquals(original, deserialized);
        }
    }

}
