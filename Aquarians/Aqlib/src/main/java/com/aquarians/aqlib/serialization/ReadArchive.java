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
 * Unified way of reading data in a format-agnostic way.
 * The encoding could be binary, XML, JSON etc.
 */
public interface ReadArchive {

    /**
     * Release resources used by this archive
     */
    void close();

    /**
     * In a binary archive, this method checks for end of stream.
     * In an XML archive, this method checks for existence of an element with given name.
     * @return true if there's more data to read, false otherwise.
     */
    boolean hasMoreData(String name);

    /**
     * Reads next stored byte or throws a RuntimeException.
     * @param name name of the variable to read
     * @return value of next stored int, throws a RuntimeException if end of stream.
     */
    byte readByte(String name);

    /**
     * Reads next stored int or throws a RuntimeException.
     * @param name name of the variable to read
     * @return value of next stored int, throws a RuntimeException if end of stream.
     */
    int readInt(String name);

    /**
     * Reads next stored long or throws a RuntimeException.
     * @param name name of the variable to read
     * @return value of next stored long (can be null). Throws a RuntimeException if end of stream.
     */
    Long readLong(String name);

    /**
     * Reads next stored float or throws a RuntimeException.
     * @param name name of the variable to read
     * @return value of next stored float, throws a RuntimeException if end of stream.
     */
    float readFloat(String name);

    /**
     * Reads next stored double or throws a RuntimeException.
     * @param name name of the variable to read
     * @return value of next stored double (can be null), throws a RuntimeException if end of stream.
     */
    Double readDouble(String name);

    /**
     * Reads next stored boolean or throws a RuntimeException.
     * @param name name of the variable to read
     * @return value of next stored boolean (can be null), throws a RuntimeException if end of stream.
     */
    Boolean readBoolean(String name);

    /**
     * Reads next stored UTF-8 string or throws a RuntimeException.
     * @param name name of the variable to read
     * @return value of next stored UTF-8 string, throws a RuntimeException if end of stream.
     */
    String readString(String name);

    /**
     * Reads next stored timestamp or throws a RuntimeException.
     * @param name name of the variable to read
     * @return value of next stored timestamp (can be null), throws a RuntimeException if end of stream.
     */
    AqCalendar readTimestamp(String name);

    /**
     * Reads next stored day or throws a RuntimeException.
     * @param name name of the variable to read
     * @return value of next stored day (can be null), throws a RuntimeException if end of stream.
     */
    Day readDay(String name);

    /**
     * Reads next stored blob or throws a RuntimeException.
     * @param name name of the variable to read
     * @return value of next stored blob (can be null), throws a RuntimeException if end of stream.
     */
    byte[] readBytes(String name);

    /**
     * Called before reading data of unknown size
     * @param name name of data to read
     */
    void beginTransaction(String name);

    /**
     * Called after reading data of previously unknown size
     */
    void endTransaction();

    /**
     * Reads next stored AqSerializable object or throws a RuntimeException.
     * @param name name of the variable to read
     * @return instance of next AqSerializable object (can be null), throws a RuntimeException if end of stream.
     */
    AqSerializable readObject(String name);
}
