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
import com.aquarians.aqlib.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads data from XML.
 */
public class XmlReadArchive implements ReadArchive {

    protected final ObjectFactory factory;
    protected Element root;
    private Map<String, Element> children = new TreeMap<>();
    private final LinkedList<Map<String, Element>> stack = new LinkedList<>();

    public XmlReadArchive(File file, ObjectFactory factory) {
        this.factory = factory;

        try {
            DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = xmlFactory.newDocumentBuilder();
            Document document = builder.parse(file);
            setRoot(document.getDocumentElement());
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }

        root.normalize();
    }

    public XmlReadArchive(String xml, ObjectFactory factory) {
        this.factory = factory;

        try {
            DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = xmlFactory.newDocumentBuilder();
            ByteArrayInputStream input =  new ByteArrayInputStream(xml.getBytes("UTF-8"));
            Document document = builder.parse(input);
            setRoot(document.getDocumentElement());
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }

        root.normalize();
    }

    private void setRoot(Element element) {
        root = element;

        NodeList list = root.getChildNodes();
        int length = list.getLength();
        for (int i = 0; i < length; i++) {
            Node node = list.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element child = (Element) node;
            children.put(child.getTagName(), child);
        }
    }

    @Override
    public void close() {

    }

    @Override
    public boolean hasMoreData(String name) {
        Element element = findElement(name);
        return (element != null);
    }

    private Element findElement(String name) {
        return children.get(name);
    }

    @Override
    public byte readByte(String name) {
        Element element = findElement(name);
        return Byte.parseByte(element.getTextContent());
    }

    @Override
    public int readInt(String name) {
        Element element = findElement(name);
        return Integer.parseInt(element.getTextContent());
    }

    @Override
    public Long readLong(String name) {
        Element element = findElement(name);
        if (!Util.safeEquals(element.getAttribute("null"), "true")) {
            return Long.parseLong(element.getTextContent());
        } else {
            return null;
        }
    }

    @Override
    public float readFloat(String name) {
        Element element = findElement(name);
        return Float.parseFloat(element.getTextContent());
    }

    @Override
    public Double readDouble(String name) {
        Element element = findElement(name);
        if (!Util.safeEquals(element.getAttribute("null"), "true")) {
            return Double.parseDouble(element.getTextContent());
        } else {
            return null;
        }
    }

    @Override
    public Boolean readBoolean(String name) {
        Element element = findElement(name);
        if (!Util.safeEquals(element.getAttribute("null"), "true")) {
            return Boolean.parseBoolean(element.getTextContent());
        } else {
            return null;
        }
    }

    @Override
    public String readString(String name) {
        Element element = findElement(name);
        if (!Util.safeEquals(element.getAttribute("null"), "true")) {
            return element.getTextContent();
        } else {
            return null;
        }
    }

    @Override
    public AqCalendar readTimestamp(String name) {
        String text = readString(name);
        if (null != text) {
            return AqCalendar.parseCalendar(text, AqCalendar.DEFAULT_FORMAT);
        } else {
            return null;
        }
    }

    @Override
    public Day readDay(String name) {
        String text = readString(name);
        if (null != text) {
            return new Day(text);
        } else {
            return null;
        }
    }

    @Override
    public byte[] readBytes(String name) {
        Element element = findElement(name);
        if (!Util.safeEquals(element.getAttribute("null"), "true")) {
            return DatatypeConverter.parseBase64Binary(element.getTextContent());
        } else {
            return null;
        }
    }

    @Override
    public void beginTransaction(String name) {
        Element element = findElement(name);
        stack.add(children);
        setRoot(element);
    }

    @Override
    public void endTransaction() {
        Element element = (Element) root.getParentNode();
        root = element;
        children = stack.removeLast();
    }

    @Override
    public AqSerializable readObject(String name) {
        // Prepare for loading object
        beginTransaction(name);

        AqSerializable instance = null;
        if (!Util.safeEquals(root.getAttribute("null"), "true")) {
            // Read type hierarchy
            String[] types = root.getAttribute("type").split(",");

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
