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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * Support for decoding from binary format.
 */
public class BinaryReadArchive implements ReadArchive {

    private static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("UTC");

    private final AqByteArrayInputStream buffer;
    private final DataInputStream stream;
    private final ObjectFactory factory;

    private final LinkedList<Integer> positions = new LinkedList<>();
    private final LinkedList<Integer> sizes = new LinkedList<>();

    public BinaryReadArchive(byte[] data, ObjectFactory factory) {
        buffer = new AqByteArrayInputStream(data);
        stream = new DataInputStream(buffer);
        this.factory = factory;
    }

    public static BinaryReadArchive readFromFile(String filename, ObjectFactory factory) {
        BinaryReadArchive archive = null;
        try {

            FileInputStream is = null;
            try {
                File file = new File(filename);
                is = new FileInputStream(file);
                byte[] bytes = new byte[(int) file.length()];
                is.read(bytes);
                archive = new BinaryReadArchive(bytes, factory);
            } finally {
                if (is != null) {
                    is.close();
                }
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }

        return archive;
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
    public boolean hasMoreData(String name) {
        try {
            return (stream.available() > 0);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public byte readByte(String name) {
        try {
            return stream.readByte();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public int readInt(String name) {
        try {
            return stream.readInt();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public Long readLong(String name) {
        try {
            byte flag = stream.readByte();
            if (flag != 0) {
                return stream.readLong();
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public float readFloat(String name) {
        try {
            return stream.readFloat();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public Double readDouble(String name) {
        try {
            byte flag = stream.readByte();
            if (flag != 0) {
                return stream.readDouble();
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        }

    @Override
    public Boolean readBoolean(String name) {
        try {
            byte flag = stream.readByte();
            if (flag != 0) {
                return (stream.readByte() != 0);
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public String readString(String name) {
        try {
            byte[] value = readBytes(name);
            if (null != value) {
                return new String(value, "UTF-8");
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public AqCalendar readTimestamp(String name) {
        String text = readString(name);
        if (text != null) {
            return AqCalendar.parseCalendar(text, AqCalendar.DEFAULT_FORMAT);
        } else {
            return null;
        }
    }

    @Override
    public Day readDay(String name) {
        String text = readString(name);
        if (text != null) {
            return new Day(text);
        } else {
            return null;
        }
    }

    @Override
    public byte[] readBytes(String name) {
        try {
            byte flag = stream.readByte();
            if (flag != 0) {
                int size = stream.readInt();
                byte[] data = new byte[size];
                readData(data);
                return data;
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private void readData(byte[] data) {
        // The loop is for when the stream might originate from a socket
        int off = 0;
        int len = data.length;
        while (len > 0) {
            int count = 0;
            try {
                count = stream.read(data, off, len);
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
            if (count <= 0) {
                throw new RuntimeException("End of stream");
            }
            off += count;
            len -= count;
        }
    }

    /**
     * Current position in the input stream
     * @return the position in the input stream
     */
    public int getPosition() {
        return buffer.getPos();
    }

    @Override
    public void beginTransaction(String name) {
        // Remember position and size of following data
        int size = 0;
        try {
            size = stream.readInt();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        int position = getPosition();

        sizes.add(size);
        positions.add(position);
    }

    @Override
    public void endTransaction() {
        int position = positions.removeLast();
        int size = sizes.removeLast();

        // Skip the bytes that weren't read
        int read = getPosition() - position;
        int remaining = size - read;
        if (remaining > 0) {
            skip(remaining);
        }
    }

    /**
     * Skips count bytes from the input stream
     * @param count number of bytes to skip
     */
    public void skip(int count) {
        try {
            stream.skip(count);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public AqSerializable readObject(String name) {
        // Prepare for loading object
        beginTransaction(name);

        // Read null flag
        AqSerializable instance = null;
        byte flag = readByte(name);
        if (flag != 0) {
            // Read type hierarchy
            String[] types = readString(name).split(",");

            // Create object instance
            for (String type : types) {
                instance = factory.createObject(type);
                if (null != instance) {
                    break;
                }
            }

            if (null == instance) {
                throw new RuntimeException("Unknown hierarchy: " + types);
            }

            // Ask object to read it's data
            instance.readFrom(this);
        }

        // Commit load
        endTransaction();

        return instance;
    }

}
