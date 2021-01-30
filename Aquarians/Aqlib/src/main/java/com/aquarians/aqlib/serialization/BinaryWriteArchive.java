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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;

/**
 * Support for encoding to binary format.
 */
public class BinaryWriteArchive implements WriteArchive {


    private final AqByteArrayOutputStream buffer;
    private final DataOutputStream stream;
    private final LinkedList<Integer> positions = new LinkedList<>();

    public BinaryWriteArchive() {
        buffer = new AqByteArrayOutputStream();
        stream = new DataOutputStream(buffer);
    }

    public void saveToFile(String filename) {
        try {
            OutputStream os = new FileOutputStream(new File(filename));
            os.write(toByteArray());
            os.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public byte[] toByteArray() {
        try {
            stream.flush();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        return buffer.toByteArray();
    }

    @Override
    public void close() {
        try {
            stream.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void writeByte(String name, byte value) {
        try {
            stream.writeByte(value);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void writeInt(String name, int value) {
        try {
            stream.writeInt(value);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void writeLong(String name, Long value) {
        try {
            if (null != value) {
                stream.writeByte(1);
                stream.writeLong(value);
            } else {
                stream.writeByte(0);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void writeFloat(String name, float value) {
        try {
            stream.writeFloat(value);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void writeDouble(String name, Double value) {
        try {
            if (null != value) {
                stream.writeByte(1);
                stream.writeDouble(value);
            } else {
                stream.writeByte(0);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void writeBoolean(String name, Boolean value) {
        try {
            if (null != value) {
                stream.writeByte(1);
                stream.writeByte(value ? 1 : 0);
            } else {
                stream.writeByte(0);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void writeString(String name, String value) {
        try {
            if (null != value) {
                writeBytes(name, value.getBytes("UTF-8"));
            } else {
                writeBytes(name, null);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void writeTimestamp(String name, AqCalendar value) {
        if (null != value) {
            writeString(name, value.toString());
        } else {
            writeString(name, null);
        }
    }

    @Override
    public void writeDay(String name, Day value) {
        if (null != value) {
            writeString(name, value.toString());
        } else {
            writeString(name, null);
        }
    }

    @Override
    public void writeBytes(String name, byte[] data) {
        try {
            if (null != data) {
                stream.writeByte(1);
                stream.writeInt(data.length);
                stream.write(data);
            } else {
                stream.writeByte(0);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void beginTransaction(String name) {
        // Remember position
        positions.add(buffer.size());
        // Reserve an int for the yet unknown size of the following data
        try {
            stream.writeInt(0);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void endTransaction() {
        // Compute size of data written since last beginTransaction
        int position = positions.removeLast();
        int size = buffer.size() - position - 4;
        // Commit to the position of the size integer
        buffer.writeInt(position, size);
    }

    @Override
    public void writeObject(String name, AqSerializable value) {
        // Prepare for saving object
        beginTransaction(name);

        if (null != value) {
            // Store not null flag
            writeByte(name, (byte) 1);

            // Store type
            StringBuilder typesBuilder = new StringBuilder();
            value.getTypeHierarchy(typesBuilder);
            writeString(name, typesBuilder.toString());

            // Store data
            value.writeTo(this);

        } else {
            // Store null flag
            writeByte(name, (byte) 0);
        }

        // Commit save
        endTransaction();
    }

}
