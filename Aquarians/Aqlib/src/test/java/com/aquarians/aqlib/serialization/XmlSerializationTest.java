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

import org.junit.Test;

public class XmlSerializationTest extends SerializationTest {

    @Override
    public WriteArchive createWriteArchive() {
        return new XmlWriteArchive();
    }

    @Override
    public ReadArchive createReadArchive(ObjectFactory factory, WriteArchive writeArchive) {
        XmlWriteArchive xmlWriteArchive = (XmlWriteArchive) writeArchive;
        return new XmlReadArchive(xmlWriteArchive.toString(), factory);
    }

    @Test
    public void testXmlForwardCompatibility() {
        testForwardCompatibility();
    }

    @Test
    public void testXmlBackwardCompatibility() {
        testBackwardCompatibility();
    }

    @Test
    public void testXmlSpeed() throws Exception {
        testSpeed();
    }

}
