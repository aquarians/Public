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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;

/**
 * Stores data as XML
 */
public class XmlWriteArchive implements WriteArchive {

    private static final int INDENT = 2;

    private final Document document;
    private Element root;

    public XmlWriteArchive() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        document = builder.newDocument();
        root = document.createElement("document");
        document.appendChild(root);
    }

    public void store(String fileName) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(INDENT));
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(new File(fileName));
        try {
            transformer.transform(source, result);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public String toString() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(INDENT));
        DOMSource source = new DOMSource(document);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        try {
            transformer.transform(source, result);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        String text = writer.getBuffer().toString();
        return text;
    }

    @Override
    public void close() {

    }

    @Override
    public void writeByte(String name, byte value) {
        Element element = document.createElement(name);
        root.appendChild(element);
        element.setTextContent(Byte.toString(value));
    }

    @Override
    public void writeInt(String name, int value) {
        Element element = document.createElement(name);
        root.appendChild(element);
        element.setTextContent(Integer.toString(value));
    }

    @Override
    public void writeLong(String name, Long value) {
        Element element = document.createElement(name);
        root.appendChild(element);
        if (null != value) {
            element.setTextContent(Long.toString(value));
        } else {
            element.setAttribute("null", "true");
        }
    }

    @Override
    public void writeFloat(String name, float value) {
        Element element = document.createElement(name);
        root.appendChild(element);
        element.setTextContent(Float.toString(value));
    }

    @Override
    public void writeDouble(String name, Double value) {
        Element element = document.createElement(name);
        root.appendChild(element);
        if (null != value) {
            element.setTextContent(Double.toString(value));
        } else {
            element.setAttribute("null", "true");
        }
    }

    @Override
    public void writeBoolean(String name, Boolean value) {
        Element element = document.createElement(name);
        root.appendChild(element);
        if (null != value) {
            element.setTextContent(Boolean.toString(value));
        } else {
            element.setAttribute("null", "true");
        }
    }

    @Override
    public void writeString(String name, String value) {
        Element element = document.createElement(name);
        root.appendChild(element);
        if (null != value) {
            element.setTextContent(value);
        } else {
            element.setAttribute("null", "true");
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
        Element element = document.createElement(name);
        root.appendChild(element);
        if (null != data) {
            element.setTextContent(DatatypeConverter.printBase64Binary(data));
        } else {
            element.setAttribute("null", "true");
        }
    }

    @Override
    public void beginTransaction(String name) {
        Element element = document.createElement(name);
        root.appendChild(element);
        root = element;
    }

    @Override
    public void endTransaction() {
        Element element = (Element) root.getParentNode();
        root = element;
    }

    @Override
    public void writeObject(String name, AqSerializable value) {
        // Prepare for storing object
        beginTransaction(name);

        if (null != value) {
            // Store type
            StringBuilder typesBuilder = new StringBuilder();
            value.getTypeHierarchy(typesBuilder);
            root.setAttribute("type", typesBuilder.toString());

            // Store data
            value.writeTo(this);
        } else {
            // Store null flag
            root.setAttribute("null", "true");
        }

        // Commit object
        endTransaction();
    }
}
