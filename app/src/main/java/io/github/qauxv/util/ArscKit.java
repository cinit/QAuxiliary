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
package io.github.qauxv.util;

import android.annotation.SuppressLint;
import android.content.Context;
import cc.ioctl.util.Reflex;
import io.github.qauxv.config.ConfigManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import me.singleneuron.qn_kernel.data.HostInfo;

@SuppressWarnings("CharsetObjectCanBeUsed")
public class ArscKit {

    public static final int NO_ENTRY = 0xFFFFFFFF;
    public static final short
        RES_NULL_TYPE = 0x0000,
        RES_STRING_POOL_TYPE = 0x0001,
        RES_TABLE_TYPE = 0x0002,
        RES_XML_TYPE = 0x0003;
    // Chunk types in RES_XML_TYPE
    public static final short
        RES_XML_FIRST_CHUNK_TYPE = 0x0100,
        RES_XML_START_NAMESPACE_TYPE = 0x0100,
        RES_XML_END_NAMESPACE_TYPE = 0x0101,
        RES_XML_START_ELEMENT_TYPE = 0x0102,
        RES_XML_END_ELEMENT_TYPE = 0x0103,
        RES_XML_CDATA_TYPE = 0x0104,
        RES_XML_LAST_CHUNK_TYPE = 0x017f;
    // This contains a uint32_t array mapping strings in the string
    // pool back to resource identifiers.  It is optional.
    public static final short
        RES_XML_RESOURCE_MAP_TYPE = 0x0180;
    // Chunk types in RES_TABLE_TYPE
    public static final short
        RES_TABLE_PACKAGE_TYPE = 0x0200,
        RES_TABLE_TYPE_TYPE = 0x0201,
        RES_TABLE_TYPE_SPEC_TYPE = 0x0202,
        RES_TABLE_LIBRARY_TYPE = 0x0203;
    // Type of the data value.
    public static final byte
        // The 'data' is either 0 or 1, specifying this resource is either
        // undefined or empty, respectively.
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
    // The 'data' holds a dynamic ResTable_ref, which needs to be
    // resolved before it can be used like a TYPE_REFERENCE.
    TYPE_DYNAMIC_REFERENCE = 0x07,
    // The 'data' holds an attribute resource identifier, which needs to be resolved
    // before it can be used like a TYPE_ATTRIBUTE.
    TYPE_DYNAMIC_ATTRIBUTE = 0x08,

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
    private static final String CACHED_RES_ID_NAME_PREFIX = "cached_res_id_name_";
    private static final String CACHED_RES_ID_CODE_PREFIX = "cached_res_id_code_";

    //FailureZero
    public static int getIdentifier(Context ctx, String type, String name, boolean allowSearch) {
        if (name == null) {
            return 0;
        }
        if (name.contains("@")) {
            String[] arr = name.split("@");
            name = arr[arr.length - 1];
        }
        if (type == null && name.contains("/")) {
            String[] arr = name.split("/");
            type = arr[0];
            name = arr[arr.length - 1];
        }
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException ignored) {
        }
        if (ctx == null) {
            ctx = HostInfo.getHostInfo().getApplication();
        }
        String pkg = ctx.getPackageName();
        int ret = ctx.getResources().getIdentifier(name, type, pkg);
        if (ret != 0) {
            return ret;
        }
        //ResId is obfuscated, try to get it from cache.
        ConfigManager cache = ConfigManager.getCache();
        ret = cache.getIntOrDefault(CACHED_RES_ID_NAME_PREFIX + type + "/" + name, 0);
        int oldcode = cache.getIntOrDefault(CACHED_RES_ID_CODE_PREFIX + type + "/" + name, -1);
        int currcode = HostInfo.getHostInfo().getVersionCode32();
        if (ret != 0 && (oldcode == currcode)) {
            return ret;
        }
        //parse thr ARSC to find it.
        if (!allowSearch) {
            return 0;
        }
        ret = enumArsc(pkg, type, name);
        if (ret != 0) {
            cache.putInt(CACHED_RES_ID_NAME_PREFIX + type + "/" + name, ret);
            cache.putInt(CACHED_RES_ID_CODE_PREFIX + type + "/" + name, currcode);
            try {
                cache.save();
            } catch (IOException e) {
                Log.e(e);
            }
        }
        return ret;
    }

    private static int enumArsc(String pkgname, String type, String name) {
        Enumeration<URL> urls = null;
        try {
            urls = (Enumeration<URL>) Reflex.invokeVirtual(Initiator.getHostClassLoader(),
                "findResources", "resources.arsc", String.class);
        } catch (Throwable e) {
            Log.e(e);
        }
        if (urls == null) {
            Log.e(new RuntimeException(
                "Error! Enum<URL<resources.arsc>> == null, loader = " + Initiator
                    .getHostClassLoader()));
            return 0;
        }
        InputStream in;
        byte[] buf = new byte[4096];
        byte[] content;
        int ret = 0;
        ArrayList<String> rets = new ArrayList<String>();
        while (urls.hasMoreElements()) {
            try {
                in = urls.nextElement().openStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int ii;
                while ((ii = in.read(buf)) != -1) {
                    baos.write(buf, 0, ii);
                }
                in.close();
                content = baos.toByteArray();
                ret = seekInArscByFileName(content, pkgname, type, name);
                if (ret != 0) {
                    return ret;
                }
            } catch (IOException e) {
                Log.e(e);
            }
        }
        //404
        return 0;
    }

    //FailureZero
    private static int seekInArscByFileName(byte[] buf, String pkgName, String type, String name) {
        try {
            int p = 0, p2;
            int chunkSize = readLe32(buf, p + 4);
            int headerSize = readLe16(buf, p + 2);
            if (buf.length < chunkSize) {
                throw new IllegalArgumentException("Truncated data");
            }
            p += headerSize;
            int targetStrIdx = findInterestedStringIndex(buf, p, "/" + name + ".");
            if (targetStrIdx == -1) {
                return 0;
            }
            p += getChunkSize(buf, p);
            if (readLe16(buf, p) != RES_TABLE_PACKAGE_TYPE) {
                throw new IllegalArgumentException("Excepted RES_TABLE_PACKAGE_TYPE, got " + Integer
                    .toHexString(readLe16(buf, p)));
            }
            int pkgId = readLe32(buf, p + 8);
            String currPkgName;
            try {
                currPkgName = new String(buf, p + 12, 256, "UTF-16LE").replace("\0", "");
            } catch (UnsupportedEncodingException e) {
                currPkgName = new String(buf, p + 12, 256).replace("\0", "");
            }
            p2 = p + 12 + 256;
            int typeStrOff = readLe32(buf, p2);
            int lastPublicType = readLe32(buf, p2 + 4);
            int keyStrOff = readLe32(buf, p2 + 8);
            int lastPublicKey = readLe32(buf, p2 + 12);
            @SuppressLint("UseSparseArrays") HashMap<Integer, String> typeStrPool = new HashMap<>();
            parseStringPool(buf, p + typeStrOff, typeStrPool);
            p2 = p + keyStrOff + getChunkSize(buf, p + keyStrOff);
            while (p2 < buf.length) {
                int chunkType = readLe16(buf, p2);
                headerSize = readLe16(buf, p2 + 2);
                chunkSize = readLe32(buf, p2 + 4);
                if (chunkType == RES_TABLE_TYPE_TYPE) {
                    int typeId = getTypeChunkTypeId(buf, p2);
                    if (typeStrPool.get(typeId - 1).equals(type)) {
                        int ret = seekStrByIndex(buf, p, p2 - p, targetStrIdx);
                        if (ret != -1) {
                            return (pkgId << 24) | (typeId << 16) | ret;
                        }
                    }
                }
                p2 += chunkSize;
            }
        } catch (Throwable e) {
            Log.e(e);
        }
        return 0;
    }

    private static int parseSpec(byte[] buf, int p, HashMap<Integer, String> strPool) {
        int chunkType = readLe16(buf, p);
        int headerSize = readLe16(buf, p + 2);
        int chunkSize = readLe32(buf, p + 4);
        if (chunkType != RES_TABLE_TYPE_SPEC_TYPE) {
            throw new IllegalArgumentException(
                "Excepted RES_TABLE_TYPE_SPEC_TYPE, got " + Integer.toHexString(chunkType));
        }
        int typeId = buf[p + 8];
        int entryCount = readLe32(buf, p + 12);
        System.out.printf("TypeId %d has %d entries.\n", typeId, entryCount);
        return chunkSize;
    }

    private static int parseType(byte[] buf, int pp, int pt) {
        int p = pp + pt;
        int chunkType = readLe16(buf, p);
        int headerSize = readLe16(buf, p + 2);
        int chunkSize = readLe32(buf, p + 4);
        if (chunkType != RES_TABLE_TYPE_TYPE) {
            throw new IllegalArgumentException(
                "Excepted RES_TABLE_TYPE_TYPE, got " + Integer.toHexString(chunkType));
        }
        int typeId = buf[p + 8];
        int flags = buf[p + 9];
        int entryCount = readLe32(buf, p + 12);
        int entriesStart = readLe32(buf, p + 16);
        System.out.printf("TypeId %d has %d entries with flag %x.\n", typeId, entryCount, flags);
        int cfgSize = readLe32(buf, p + 20);
        int entryOffStart = p + 20 + cfgSize;
        for (int i = 0; i < entryCount; i++) {
            int off = readLe32(buf, entryOffStart + 4 * i);
            if (off != -1) {
                System.out.printf("Type %d entry %d has entry offset at %d\n", typeId, i, off);
                int entrySize = readLe16(buf, p + entriesStart + off);
                int entryFlags = readLe16(buf, p + entriesStart + off + 2);
                int keyIndex = readLe32(buf, p + entriesStart + off + 4);
                int dataSize = readLe16(buf, p + entriesStart + off + 8);
                byte type = buf[p + entriesStart + off + 11];
                int dataValue = readLe16(buf, p + entriesStart + off + 12);
                nop();
            }
        }
        return chunkSize;
    }

    public static int getTypeChunkTypeId(byte[] buf, int p) {
        int chunkType = readLe16(buf, p);
        if (chunkType != RES_TABLE_TYPE_TYPE) {
            throw new IllegalArgumentException(
                "Excepted RES_TABLE_TYPE_TYPE, got " + Integer.toHexString(chunkType));
        }
        return buf[p + 8];
    }

    private static int seekStrByIndex(byte[] buf, int pp, int pt, int strIndex) {
        int p = pp + pt;
        int chunkType = readLe16(buf, p);
        int headerSize = readLe16(buf, p + 2);
        int chunkSize = readLe32(buf, p + 4);
        if (chunkType != RES_TABLE_TYPE_TYPE) {
            throw new IllegalArgumentException(
                "Excepted RES_TABLE_TYPE_TYPE, got " + Integer.toHexString(chunkType));
        }
        int typeId = buf[p + 8];
        int flags = buf[p + 9];
        int entryCount = readLe32(buf, p + 12);
        int entriesStart = readLe32(buf, p + 16);
        int cfgSize = readLe32(buf, p + 20);
        int entryOffStart = p + 20 + cfgSize;
        for (int i = 0; i < entryCount; i++) {
            int off = readLe32(buf, entryOffStart + 4 * i);
            if (off != -1) {
                int entrySize = readLe16(buf, p + entriesStart + off);
                int entryFlags = readLe16(buf, p + entriesStart + off + 2);
                int keyIndex = readLe32(buf, p + entriesStart + off + 4);
                int dataSize = readLe16(buf, p + entriesStart + off + 8);
                byte type = buf[p + entriesStart + off + 11];
                int dataValue = readLe16(buf, p + entriesStart + off + 12);
                if (type == TYPE_STRING && dataValue == strIndex) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int getChunkSize(byte[] buf, int p) {
        return readLe32(buf, p + 4);
    }

    public static int parseStringPool(byte[] buf, int p, HashMap<Integer, String> strPool) {
        int chunkType = readLe16(buf, p);
        if (chunkType != RES_STRING_POOL_TYPE) {
            throw new IllegalArgumentException(
                "Excepted RES_STRING_POOL_TYPE, got " + Integer.toHexString(chunkType));
        }
        int headerSize = readLe16(buf, p + 2);
        int chunkSize = readLe32(buf, p + 4);
        int stringCount = readLe32(buf, p + 8);
        int styleCount = readLe32(buf, p + 12);
        int stringFlags = readLe32(buf, p + 16);
        int stringsStart = readLe32(buf, p + 20);
        int stylesStart = readLe32(buf, p + 24);
        boolean UTF8_FLAG = 0 != ((1 << 8) & stringFlags);
        //start string offset array
        for (int i = 0; i < stringCount; i++) {
            int strpos = readLe32(buf, p + 28 + i * 4);
            int len;
            String str;
            if (UTF8_FLAG) {
                int[] _len_ret = new int[1];
                int charCount = readUtf8_len(buf, p + stringsStart + strpos, _len_ret);
                int blen2 = readUtf8_len(buf, p + stringsStart + strpos + charCount, _len_ret);
                len = _len_ret[0];
                try {
                    str = new String(buf, p + stringsStart + strpos + charCount + blen2, len,
                        "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    str = new String(buf, p + stringsStart + strpos + charCount + blen2, len);
                }
            } else {
                int[] _len_ret = new int[1];
                int blen2 = readUtf16_len(buf, p + stringsStart + strpos, _len_ret);
                len = _len_ret[0] * 2;
                try {
                    str = new String(buf, p + stringsStart + strpos + blen2, len, "UTF-16LE");
                } catch (UnsupportedEncodingException e) {
                    str = new String(buf, p + stringsStart + strpos + blen2, len);
                }
            }
            strPool.put(i, str);
        }
        return chunkSize;
    }

    public static int findInterestedStringIndex(byte[] buf, int p, String target) {
        int chunkType = readLe16(buf, p);
        if (chunkType != RES_STRING_POOL_TYPE) {
            throw new IllegalArgumentException(
                "Excepted RES_STRING_POOL_TYPE, got " + Integer.toHexString(chunkType));
        }
        int headerSize = readLe16(buf, p + 2);
        int chunkSize = readLe32(buf, p + 4);
        int stringCount = readLe32(buf, p + 8);
        int styleCount = readLe32(buf, p + 12);
        int stringFlags = readLe32(buf, p + 16);
        int stringsStart = readLe32(buf, p + 20);
        int stylesStart = readLe32(buf, p + 24);
        boolean UTF8_FLAG = 0 != ((1 << 8) & stringFlags);
        //start string offset array
        for (int i = 0; i < stringCount; i++) {
            int strpos = readLe32(buf, p + 28 + i * 4);
            int len;
            String str;
            if (UTF8_FLAG) {
                int[] _len_ret = new int[1];
                int charCount = readUtf8_len(buf, p + stringsStart + strpos, _len_ret);
                int blen2 = readUtf8_len(buf, p + stringsStart + strpos + charCount, _len_ret);
                len = _len_ret[0];
                try {
                    str = new String(buf, p + stringsStart + strpos + charCount + blen2, len,
                        "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    str = new String(buf, p + stringsStart + strpos + charCount + blen2, len);
                }
            } else {
                int[] _len_ret = new int[1];
                int blen2 = readUtf16_len(buf, p + stringsStart + strpos, _len_ret);
                len = _len_ret[0] * 2;
                try {
                    str = new String(buf, p + stringsStart + strpos + blen2, len, "UTF-16LE");
                } catch (UnsupportedEncodingException e) {
                    str = new String(buf, p + stringsStart + strpos + blen2, len);
                }
            }
            if (str.contains(target)) {
                return i;
            }
        }
        return -1;
    }

    //Return the size 1 or 2
    public static int readUtf8_len(byte[] src, int p, int[] ret) {
        byte b = src[p];
        if ((0x80 & b) != 0) {
            ret[0] = (b & 0x7f) << 8 | (src[p + 1] & 0xFF);
            return 2;
        } else {
            ret[0] = b & 0xFF;
            return 1;
        }
    }

    //Return the size 2 or 4
    public static int readUtf16_len(byte[] src, int p, int[] ret) {
        int s = readLe16(src, p);
        if ((0x8000 & s) != 0) {
            ret[0] = (s & 0x7FFF) << 16 | (readLe16(src, p + 2) & 0xFFFF);
            return 4;
        } else {
            ret[0] = s & 0xFFFF;
            return 2;
        }
    }

    public static int readLe32(byte[] xml, int pos) {
        return (xml[pos]) & 0xff | (xml[pos + 1] << 8) & 0x0000ff00
            | (xml[pos + 2] << 16) & 0x00ff0000 | ((xml[pos + 3] << 24) & 0xff000000);
    }

    public static short readLe16(byte[] xml, int pos) {
        return (short) ((xml[pos]) & 0xff | (xml[pos + 1] << 8) & 0xff00);
    }

    public static void nop() {
    }

}
