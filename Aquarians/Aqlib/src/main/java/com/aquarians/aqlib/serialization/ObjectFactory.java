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
 * Factory for serializable objects, intended for newer-reads-older
 * and older-reads-newer compatibility.
 */
public interface ObjectFactory {

    /**
     * Registers a prototype for a serializable object.
     * @param type A type identifier, which should NEVER be changed, in order to ensure compatibility with older versions.
     * @param prototype Instance of an object identified by given type
     */
    void registerPrototype(String type, AqSerializable prototype);

    /**
     * Returns a new object of the requested type or null if the type identifier is unknown.
     * @param type type identifier, not necessary the Java class name
     * @return new instance of object of requested type or null if the type identifier is unknown.
     */
    AqSerializable createObject(String type);

}
