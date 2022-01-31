/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */
package cc.ioctl.util;

import io.github.qauxv.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * @author cinit A simple tool parses Android binary XML
 */
public class BinaryXmlParser {

    public static final short
        RES_NULL_TYPE = 0x0000,
        RES_STRING_POOL_TYPE = 0x0001,
        RES_TABLE_TYPE = 0x0002,

    // Chunk types in RES_XML_TYPE
    RES_XML_TYPE = 0x0003,
        RES_XML_FIRST_CHUNK_TYPE = 0x0100,
        RES_XML_START_NAMESPACE_TYPE = 0x0100,
        RES_XML_END_NAMESPACE_TYPE = 0x0101,
        RES_XML_START_ELEMENT_TYPE = 0x0102,
        RES_XML_END_ELEMENT_TYPE = 0x0103,
        RES_XML_CDATA_TYPE = 0x0104,
        RES_XML_LAST_CHUNK_TYPE = 0x017f,

    // This contains a uint32_t array mapping strings in the string
    // pool back to resource identifiers.  It is optional.
    RES_XML_RESOURCE_MAP_TYPE = 0x0180,

    // Chunk types in RES_TABLE_TYPE
    RES_TABLE_PACKAGE_TYPE = 0x0200,
        RES_TABLE_TYPE_TYPE = 0x0201,
        RES_TABLE_TYPE_SPEC_TYPE = 0x0202;
    public static final int NULL = 0xFFFFFFFF;

    public static XmlNode parseXml(String filePath) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return parseXml(bos.toByteArray());

        } catch (Exception e) {
            Log.e("parse xml error:" + e);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static XmlNode parseXml(byte[] xml) {
        XmlNode root = new XmlNode();
        int[] pos = {0};
        short type, headerSize;
        int size;
        int xmlSize = xml.length;
        ArrayList<String> stringPool = null;
        Stack<XmlNode> stack = new Stack<>();
        XmlNode node;
        loop:
        while (pos[0] < xmlSize) {
            type = readLe16(xml, pos[0]);
            headerSize = readLe16(xml, pos[0] + 2);
            size = readLe32(xml, pos[0] + 4);
            s:
            switch (type) {
                case RES_XML_TYPE:
                    if (size > xml.length) {
                        throw new RuntimeException("Corrupted AXML data");
                    }
                    xmlSize = size;
                    pos[0] += headerSize;
                    break s;
                case RES_STRING_POOL_TYPE:
                    int stringCount = readLe32(xml, pos[0] + 8);
                    int style_num = readLe32(xml, pos[0] + 12);
                    int flag = readLe32(xml, pos[0] + 16);
                    int stringStart = readLe32(xml, pos[0] + 20);
                    int style_start = readLe32(xml, pos[0] + 24);
                    boolean uft8 = (flag & (1 << 8)) != 0;
                    int strpos;
                    String str;
                    int len;
                    stringPool = new ArrayList<>();
                    for (int i = 0; i < stringCount; i++) {
                        strpos = readLe32(xml, pos[0] + 28 + i * 4);
                        if (uft8) {
                            len = xml[pos[0] + stringStart + strpos];
                            str = new String(xml, pos[0] + stringStart + strpos + 2, len, StandardCharsets.UTF_8);
                        } else {
                            len = readLe16(xml, pos[0] + stringStart + strpos);
                            str = new String(xml, pos[0] + stringStart + strpos + 2, len, StandardCharsets.UTF_16);
                        }
                        stringPool.add(i, str);
                    }
                    pos[0] += size;
                    break s;
                case RES_XML_START_NAMESPACE_TYPE:
                    pos[0] += size;
                    break s;
                case RES_XML_END_NAMESPACE_TYPE:
                    pos[0] += size;
                    break loop;
                case RES_XML_START_ELEMENT_TYPE:
                    if (stack.empty()) {
                        node = root;
                    } else {
                        node = new XmlNode();
                    }
                    node.lineNumber = readLe32(xml, pos[0] + 8);
                    int comm_index = readLe32(xml, pos[0] + 12);
                    if (comm_index != NULL) {
                        node.comment = stringPool.get(comm_index);
                    }
                    int ns_index = readLe32(xml, pos[0] + 16);
                    if (ns_index != NULL) {
                        node.namespace = stringPool.get(ns_index);
                    }
                    int name_index = readLe32(xml, pos[0] + 20);
                    node.name = stringPool.get(name_index);
                    short attributeStart = readLe16(xml, pos[0] + 24);
                    short attributeSize = readLe16(xml, pos[0] + 26);
                    short attributeCount = readLe16(xml, pos[0] + 28);
                    //3*readLe16(xml,pos[0]+28);next:36
                    int ni;
                    int consumed = 0;
                    short ressize;
                    String name;
                    if (attributeCount > 0) {
                        node.attributes = new HashMap<>();
                    }
                    for (int i = 0; i < attributeCount; i++) {
                        //4nsi
                        ni = readLe32(xml, pos[0] + 16 + attributeStart + consumed + 4);
                        name = stringPool.get(ni);
                        //4rawstr
                        ressize = readLe16(xml, pos[0] + 16 + attributeStart + consumed + 12);
                        XmlNode.Res r = new XmlNode.Res();
                        r.dataType = xml[pos[0] + 16 + attributeStart + consumed + 15];
                        r.data = readLe32(xml, pos[0] + 16 + attributeStart + consumed + 16);
                        if (r.dataType == XmlNode.Res.TYPE_STRING) {
                            r.str = stringPool.get(r.data);
                        }
                        consumed += ressize + 12;
                        node.attributes.put(name, r);
                    }
                    stack.push(node);
                    pos[0] += size;
                    break s;
                case RES_XML_CDATA_TYPE:
                    if (stack.isEmpty()) {
                        node = root;
                    } else {
                        node = stack.peek();
                    }
                    XmlNode.Res r = node.cdata = new XmlNode.Res();
                    r.dataType = xml[pos[0] + 16 + 3];
                    r.data = readLe16(xml, pos[0] + 16 + 4);
                    if (r.dataType == XmlNode.Res.TYPE_STRING) {
                        r.str = stringPool.get(r.data);
                    }
                    pos[0] += size;
                    break s;
                case RES_XML_END_ELEMENT_TYPE:
                    if (stack.size() > 0) {
                        node = stack.pop();
                        XmlNode parent;
                        if (!stack.empty()) {
                            parent = stack.peek();
                            if (parent.elements == null) {
                                parent.elements = new ArrayList<>();
                            }
                            parent.elements.add(node);
                        }
                    }
                    pos[0] += size;
                    break s;
                default:
                    pos[0] += size;
                    break s;
            }
        }
        return root;
    }

    public static int readLe32(byte[] xml, int pos) {
        int i =
            (xml[pos]) & 0xff | (xml[pos + 1] << 8) & 0x0000ff00 | (xml[pos + 2] << 16) & 0x00ff0000
                | ((xml[pos + 3] << 24) & 0xff000000);
        return i;
    }

    public static short readLe16(byte[] xml, int pos) {
        return (short) ((xml[pos]) & 0xff | (xml[pos + 1] << 8) & 0xff00);
    }

    public static class XmlNode {

        public String name;
        public Map<String, Res> attributes;
        public ArrayList<XmlNode> elements;
        public int lineNumber = -1;
        public String comment;
        public String namespace;
        public Res cdata;

        public static class Res {

            public static final byte
                TYPE_NULL = 0x00,
            // The 'data' holds a ResTable_ref, a reference to another resource
            // table entry.
            TYPE_REFERENCE = 0x01,
            // The 'data' holds an attribute resource identifier.
            TYPE_ATTRIBUTE = 0x02,
            // The 'data' holds an index into the containing resource table's
            // global value string pool.
            TYPE_STRING = 0x03,
            // The 'data' holds a single-precision floating point number.
            TYPE_FLOAT = 0x04,
            // The 'data' holds a complex number encoding a dimension value,
            // such as "100in".
            TYPE_DIMENSION = 0x05,
            // The 'data' holds a complex number encoding a fraction of a
            // container.
            TYPE_FRACTION = 0x06,
            // Beginning of integer flavors...
            TYPE_FIRST_INT = 0x10,
            // The 'data' is a raw integer value of the form n..n.
            TYPE_INT_DEC = 0x10,
            // The 'data' is a raw integer value of the form 0xn..n.
            TYPE_INT_HEX = 0x11,
            // The 'data' is either 0 or 1, for input "false" or "true" respectively.
            TYPE_INT_BOOLEAN = 0x12,
            // Beginning of color integer flavors...
            TYPE_FIRST_COLOR_INT = 0x1c,
            // The 'data' is a raw integer value of the form #aarrggbb.
            TYPE_INT_COLOR_ARGB8 = 0x1c,
            // The 'data' is a raw integer value of the form #rrggbb.
            TYPE_INT_COLOR_RGB8 = 0x1d,
            // The 'data' is a raw integer value of the form #argb.
            TYPE_INT_COLOR_ARGB4 = 0x1e,
            // The 'data' is a raw integer value of the form #rgb.
            TYPE_INT_COLOR_RGB4 = 0x1f,
            // ...end of integer flavors.
            TYPE_LAST_COLOR_INT = 0x1f,
            // ...end of integer flavors.
            TYPE_LAST_INT = 0x1f;
            public static final int
                COMPLEX_UNIT_SHIFT = 0,
                COMPLEX_UNIT_MASK = 0xf,
            // TYPE_DIMENSION: Value is raw pixels.
            COMPLEX_UNIT_PX = 0,
            // TYPE_DIMENSION: Value is Device Independent Pixels.
            COMPLEX_UNIT_DIP = 1,
            // TYPE_DIMENSION: Value is a Scaled device independent Pixels.
            COMPLEX_UNIT_SP = 2,
            // TYPE_DIMENSION: Value is in points.
            COMPLEX_UNIT_PT = 3,
            // TYPE_DIMENSION: Value is in inches.
            COMPLEX_UNIT_IN = 4,
            // TYPE_DIMENSION: Value is in millimeters.
            COMPLEX_UNIT_MM = 5,
            // TYPE_FRACTION: A basic fraction of the overall size.
            COMPLEX_UNIT_FRACTION = 0,
            // TYPE_FRACTION: A fraction of the parent size.
            COMPLEX_UNIT_FRACTION_PARENT = 1,
            // Where the radix information is, telling where the decimal place
            // appears in the mantissa.  This give us 4 possible fixed point
            // representations as defined below.
            COMPLEX_RADIX_SHIFT = 4,
                COMPLEX_RADIX_MASK = 0x3,
            // The mantissa is an integral number -- i.e., 0xnnnnnn.0
            COMPLEX_RADIX_23p0 = 0,
            // The mantissa magnitude is 16 bits -- i.e, 0xnnnn.nn
            COMPLEX_RADIX_16p7 = 1,
            // The mantissa magnitude is 8 bits -- i.e, 0xnn.nnnn
            COMPLEX_RADIX_8p15 = 2,
            // The mantissa magnitude is 0 bits -- i.e, 0x0.nnnnnn
            COMPLEX_RADIX_0p23 = 3,
            // Where the actual value is.  This gives us 23 bits of
            // precision.  The top bit is the sign.
            COMPLEX_MANTISSA_SHIFT = 8,
                COMPLEX_MANTISSA_MASK = 0xffffff;
            public byte dataType;
            public int data;
            public String str;
        }
    }
}
