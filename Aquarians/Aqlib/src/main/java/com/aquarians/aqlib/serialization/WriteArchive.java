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

import com.aquarians.aqlib.AqCalendar;
import com.aquarians.aqlib.Day;

/**
 * Unified way of writing data in a format-agnostic way.
 * The encoding could be binary, XML, JSON etc.
 */
public interface WriteArchive {

    /**
     * Release resources used by this archive
     */
    void close();

    /**
     * Stores a new byte value. May throw RuntimeException on write error.
     * @param name name of the variable to store
     * @param value integer value to store
     */
    void writeByte(String name, byte value);

    /**
     * Stores a new integer value. May throw RuntimeException on write error.
     * @param name name of the variable to store
     * @param value integer value to store
     */
    void writeInt(String name, int value);

    /**
     * Stores a new long value. May throw RuntimeException on write error.
     * @param name name of the variable to store
     * @param value long value to store, may be null
     */
    void writeLong(String name, Long value);

    /**
     * Stores a new float value. May throw RuntimeException on write error.
     * * @param name name of the variable to store
     * * @param name name of the variable to store
     * @param value float value to store
     */
    void writeFloat(String name, float value);

    /**
     * Stores a new double value. May throw RuntimeException on write error.
     * @param name name of the variable to store
     * @param value double value to store, may be null
     */
    void writeDouble(String name, Double value);

    /**
     * Stores a new boolean value. May throw RuntimeException on write error.
     * @param name name of the variable to store
     * @param value boolean value to store, may be null
     */
    void writeBoolean(String name, Boolean value);

    /**
     * Stores a new UTF-8 String value. May throw RuntimeException on write error.
     * @param name name of the variable to store
     * @param value UTF-8 String value to store, may be null
     */
    void writeString(String name, String value);

    /**
     * Stores a new timestamp value. May throw RuntimeException on write error.
     * @param name name of the variable to store
     * @param value UTC timestamp value to store, may be null
     */
    void writeTimestamp(String name, AqCalendar value);

    /**
     * Stores a new dayvalue. May throw RuntimeException on write error.
     * @param name name of the variable to store
     * @param value UTC timestamp value to store, may be null
     */
    void writeDay(String name, Day value);

    /**
     * Stores a new blob value. May throw RuntimeException on write error.
     * @param name name of the variable to store
     * @param data blob data to store, may be null
     */
    void writeBytes(String name, byte[] data);

    /**
     * Prepares for storing some data
     * Data is written as <[size][data]>, but at this time
     * the size is unknown yet. So it reserves the space
     * for storing the size but doesn't commit it yet.
     * @param name name of data which will follow
     */
    void beginTransaction(String name);

    /**
     * Commits the size of data written since the last beginTransaction().
     * Stores the actual written size in the last reserved space.
     */
    void endTransaction();

    /**
     * Stores a new AqSerializable object instance. May throw RuntimeException on write error.re
     * @param name name of the variable to store
     * @param value AqSerializable object instance to store, may be null
     */
    void writeObject(String name, AqSerializable value);

}
