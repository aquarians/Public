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
 * Interface to be implemented by serializable objects.
 * The serialization is format-agnostic (could be encoded as binary, XML, JSON etc).
 * And the serialization is also language-agnostic: the encoder could be a Java application and the decoder a C++ one.
 * Offers two-way compatibility:
 *  a) Version 2 of an application would de-serialize data written by version 1.
 *     Some data would get defaults. For instance version 1 had "circles" and in
 *     version 2, the concept of "color" was introduced. When reading a version 1
 *     circle from a version 2 application, it would get a default color.
 *
 *  b) Conversely, version 1 of an application would de-serialize data written by version 2.
 *     Some data would be ignored. On the example from point a), when reading a version 2
 *     circle from a version 1 application, the color would be ignored.
 */
public interface AqSerializable {

    /**
     * Prototype-method: create a new object of this type.
     * @return an empty (default constructor) object of the same class as this prototype.
     */
    AqSerializable createInstance();

    /**
     * Return a list of type identifiers, from the top to the bottom of the hierarchy.
     * Needed for ensuring older versions of the application stay compatible with newer ones.
     * For instance version 1 had "circles" and in version 2, the concept of "colored circle" was introduced.
     * Given the original class Circle one way would be to add the color as a new member variable.
     * Alternatively, a new class ColoredCircle extending Circle might be introduced.
     * If version 2 would only write "ColoredCircle" as type info, this would break
     * compatibility as there's no class named "ColoredCircle" in version 1.
     * But if it writes "ColoredCircle,Circle" as type info, then a version 2 application would
     * start trying to deserialize a ColoredCircle and succeed. A version 1 application
     * would look for ColoredCircle, not find it, but continue with Circle and succeed.
     * @param types list of type identifiers, from the top to the bottom of the hierarchy.
     */
    void getTypeHierarchy(StringBuilder types);

    /**
     * Write object data to given format-agnostic archive (could be binary, XML, JSON etc)
     * @param archive archive to write this object's data to
     */
    void writeTo(WriteArchive archive);

    /**
     * Read object data from given format-agnostic archive (could be binary, XML, JSON etc)
     * @param archive archive to read this object's data from
     */
    void readFrom(ReadArchive archive);
}
