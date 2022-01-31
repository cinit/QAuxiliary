package cc.ioctl.util.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class Table<K> implements Serializable, Cloneable {

    public static final Object VOID_INSTANCE;
    public static final byte TYPE_VOID = 0;//should NOT have value

    /* New format!!
     * LE
     * byte   byte*3   int         byte[] byte[]
     * type   reserved length      data   padding
     *        keep 0   with header        eg.4-bytes alignment
     * eg for NULL
     * 00   000000   00000008
     * type reserved length
     */
    public static final byte TYPE_BYTE = 1;
    public static final byte TYPE_BOOL = 2;
    public static final byte TYPE_WCHAR32 = 3;
    public static final byte TYPE_INT = 4;
    public static final byte TYPE_SHORT = 5;
    public static final byte TYPE_LONG = 6;
    public static final byte TYPE_FLOAT = 7;
    public static final byte TYPE_DOUBLE = 8;
    public static final byte TYPE_IRAW = 9;
    public static final byte TYPE_IUTF8 = 10;
    public static final byte TYPE_IUTF32 = 11;
    /**
     * B_TABLE ISTR_table_name [ I_len_types(fields.len) I_len_data(records.len) B_key_type ISTR_key_name (B_field_type
     * I_STR_name)... (B_type_K(THIS-IS-IMPORTANTA!) index (B_type val)...)... ] B_type  ISTR_name       val
     */
    public static final byte TYPE_TABLE = 16;
    /**
     * B_ARRAY ISTR_array_name [ B_type I_reserved I_array_size (B_type val)... ]
     **/
    public static final byte TYPE_ARRAY = 17;
    public static final byte TYPE_MAP = 18;
    public static final byte TYPE_EOF = (byte) 0xFF;//oh,no...it's terrible!

    static {
        Object tmp = null;
        try {
            Method m = Class.class.getMethod("getDeclaredConstructor", Class[].class);
            Constructor<Void> c = (Constructor<Void>) m
                .invoke(Void.class, new Object[]{new Class[0]});
            c.setAccessible(true);
            tmp = c.newInstance();
        } catch (Exception e) {
            //**sigh**
            e.printStackTrace();
            tmp = new Object();
        }
        VOID_INSTANCE = tmp;
    }

    public HashMap<K, Object[]> records;
    public String[] fields;
    public byte[] types;
    public byte keyType;
    public String keyName;

    public Table() {
        init();
    }

    public static ArrayList readArray(DataInputStream in) throws IOException {
        int len = in.readInt();
        byte[] buf = new byte[len];
        in.readFully(buf, 0, len);
        throw new RuntimeException("Stub!");
    }

    public static byte[] readIRaw(DataInputStream in) throws IOException {
        int len = in.readInt();
        byte[] buf = new byte[len];
        in.readFully(buf, 0, len);
        return buf;
    }

    public static String readIStr(DataInputStream in) throws IOException {
        byte[] buf = readIRaw(in);
        return new String(buf);
    }

    public static void writeIRaw(DataOutputStream out, byte[] buf) throws IOException {
        out.writeInt(buf.length);
        out.write(buf);
    }

    public static void writeIStr(DataOutputStream out, String str) throws IOException {
        writeIRaw(out, str.getBytes());
    }

    public static Table readTable(DataInputStream in) throws IOException {
        Table table = new Table();
        int _f = in.readInt(), _r = in.readInt();
        table.fields = new String[_f];
        table.types = new byte[_f];
        int i, ii;
        Object fn;
        Object[] recval;
        table.keyType = in.readByte();
        table.keyName = readIStr(in);
        for (ii = 0; ii < _f; ii++) {
            table.types[ii] = in.readByte();
            table.fields[ii] = readIStr(in);
        }
        byte rtype;
        a:
        for (ii = 0; ii < _r; ii++) {
            fn = readTypeAndObj(in);
            recval = new Object[_f];
            for (i = 0; i < _f; i++) {
                rtype = (byte) in.read();
                switch (rtype) {
                    case TYPE_VOID:
                        recval[i] = null;
                        break;
                    case TYPE_BYTE:
                        recval[i] = (byte) in.read();
                        break;
                    case TYPE_BOOL:
                        recval[i] = in.read() != 0;
                        break;
                    case TYPE_WCHAR32:
                        recval[i] = in.readInt();
                        break;
                    case TYPE_INT:
                        recval[i] = in.readInt();
                        break;
                    case TYPE_SHORT:
                        recval[i] = in.readShort();
                        break;
                    case TYPE_LONG:
                        recval[i] = in.readLong();
                        break;
                    case TYPE_FLOAT:
                        recval[i] = in.readFloat();
                        break;
                    case TYPE_DOUBLE:
                        recval[i] = in.readDouble();
                        break;
                    case TYPE_IUTF8:
                        recval[i] = readIStr(in);
                        break;
                    case TYPE_IRAW:
                        recval[i] = readIRaw(in);
                        break;
                    case TYPE_TABLE:
                        recval[i] = readTable(in);
                        break;
                    case TYPE_EOF:
                        break a;
                    default:
                        throw new IOException(
                            "Unexpected type:" + table.types[i] + ",record_name:\"" + fn + "\"");
                }
            }
            table.records.put(fn, recval);
        }
        return table;
    }

    public static Object readTypeAndObj(DataInputStream in) throws IOException {
        byte rtype = in.readByte();
        switch (rtype) {
            case TYPE_VOID:
                return null;
            case TYPE_BYTE:
                return (byte) in.read();
            case TYPE_BOOL:
                return in.read() != 0;
            case TYPE_WCHAR32:
                return in.readInt();
            case TYPE_INT:
                return in.readInt();
            case TYPE_SHORT:
                return in.readShort();
            case TYPE_LONG:
                return in.readLong();
            case TYPE_FLOAT:
                return in.readFloat();
            case TYPE_DOUBLE:
                return in.readDouble();
            case TYPE_IUTF8:
                return readIStr(in);
            case TYPE_IRAW:
                return readIRaw(in);
            case TYPE_TABLE:
                return readTable(in);
            case TYPE_ARRAY:
                return readArray(in);
            case TYPE_EOF:
                throw new IOException("Unexpected type: TYPE_EOF");
            default:
                throw new IOException("Unexpected type:" + rtype + "\"");
        }
    }

    public static void writeTypeAndObj(DataOutputStream out, byte rtype, Object obj)
        throws IOException {
        out.write(rtype);
        switch (rtype) {
            case TYPE_VOID:
                break;
            case TYPE_BYTE:
                out.writeByte((Byte) obj);
                break;
            case TYPE_BOOL:
                out.writeByte(((Boolean) obj) ? 1 : 0);
                break;
            case TYPE_WCHAR32:
                out.writeInt((Integer) obj);
                break;
            case TYPE_INT:
                out.writeInt((Integer) obj);
                break;
            case TYPE_SHORT:
                out.writeShort((Short) obj);
                break;
            case TYPE_LONG:
                out.writeLong((Long) obj);
                break;
            case TYPE_FLOAT:
                out.writeFloat((Float) obj);
                break;
            case TYPE_DOUBLE:
                out.writeDouble((Double) obj);
                break;
            case TYPE_IUTF8:
                writeIStr(out, (String) obj);
                break;
            case TYPE_IRAW:
                writeIRaw(out, (byte[]) obj);
                break;
            case TYPE_TABLE:
                writeTable(out, (Table) obj);
                break;
            case TYPE_EOF:
            default:
                throw new IOException("Unexpected type:" + rtype + "\"");
        }
    }

    public static void writeRecord(DataOutputStream out, @Nullable String key, Object val)
        throws IOException {
        byte type;
        try {
            Class clz = val.getClass();
            if (Byte.class.equals(clz)) {
                type = TYPE_BYTE;
                out.write(type);
                if (key != null) {
                    writeIStr(out, key);
                }
                out.writeByte((byte) val);
            } else if (Boolean.class.equals(clz)) {
                type = TYPE_BOOL;
                out.write(type);
                if (key != null) {
                    writeIStr(out, key);
                }
                out.writeByte(((boolean) val) ? 1 : 0);
            } else if (Character.class.equals(clz)) {
                type = TYPE_WCHAR32;
                out.writeInt(type);
                if (key != null) {
                    writeIStr(out, key);
                }
                out.writeChar((Integer) val);
            } else if (Integer.class.equals(clz)) {
                type = TYPE_INT;
                out.write(type);
                if (key != null) {
                    writeIStr(out, key);
                }
                out.writeInt((Integer) val);
            } else if (Short.class.equals(clz)) {
                type = TYPE_SHORT;
                out.write(type);
                if (key != null) {
                    writeIStr(out, key);
                }
                out.writeShort((Short) val);
            } else if (Long.class.equals(clz)) {
                type = TYPE_LONG;
                out.write(type);
                if (key != null) {
                    writeIStr(out, key);
                }
                out.writeLong((Long) val);
            } else if (Float.class.equals(clz)) {
                type = TYPE_FLOAT;
                out.write(type);
                if (key != null) {
                    writeIStr(out, key);
                }
                out.writeFloat((Float) val);
            } else if (Double.class.equals(clz)) {
                type = TYPE_DOUBLE;
                out.write(type);
                if (key != null) {
                    writeIStr(out, key);
                }
                out.writeDouble((Double) val);
            } else if (String.class.equals(clz)) {
                type = TYPE_IUTF8;
                out.write(type);
                if (key != null) {
                    writeIStr(out, key);
                }
                writeIStr(out, (String) val);
            } else if (byte[].class.equals(clz)) {
                type = TYPE_IRAW;
                out.write(type);
                if (key != null) {
                    writeIStr(out, key);
                }
                writeIRaw(out, (byte[]) val);
            } else if (Table.class.equals(clz)) {
                type = TYPE_TABLE;
                out.write(type);
                if (key != null) {
                    writeIStr(out, key);
                }
                writeTable(out, (Table) val);
            } else {
                throw new IOException("Unsupported type:" + clz.getName());
            }
        } catch (NullPointerException e) {
            type = TYPE_VOID;
            out.write(type);
            if (key != null) {
                writeIStr(out, key);
            }
        }
    }

    public static void writeTable(DataOutputStream out, Table table) throws IOException {
        int i, ii;
        out.writeInt(table.fields.length);
        out.writeInt(table.records.size());
        out.write(table.keyType);
        writeIStr(out, table.keyName);
        for (i = 0; i < table.fields.length; i++) {
            out.write(table.types[i]);
            writeIStr(out, table.fields[i]);
        }
        Map.Entry<Object, Object[]> next;
        Object[] val;
        Iterator<Map.Entry<Object, Object[]>> it = table.records.entrySet().iterator();
        while (it.hasNext()) {
            next = it.next();
            val = next.getValue();
            writeTypeAndObj(out, table.keyType, next.getKey());
            for (i = 0; i < table.fields.length; i++) {
                writeRecord(out, null, val[i]);
            }
        }
    }

    public int getFieldId(String name) {
        for (int i = 0; i < fields.length; i++) {
            if (name.equals(fields[i])) {
                return i;
            }
        }
        return -1;
    }

    public boolean hasRecord(K key) {
        return records.containsKey(key);
    }

    public Object get(K key, String field) throws NoSuchFieldException {
        int i = getFieldId(field);
        if (i < 0) {
            throw new NoSuchFieldException(field);
        }
        if (!hasRecord(key)) {
            throw new NoSuchElementException("key:" + key);
        }
        return records.get(key)[i];
    }

    public void set(K key, String field, Serializable val) throws NoSuchFieldException {
        synchronized (this) {
            int i = getFieldId(field);
            if (i < 0) {
                throw new NoSuchFieldException(field);
            }
            if (!hasRecord(key)) {
                throw new NoSuchElementException("key:" + key);
            }

            records.get(key)[i] = val;
        }
    }

    public void insert(K key) {
        synchronized (this) {
            if (hasRecord(key)) {
                return;
            }
            Object[] rec = new Object[fields.length];
            records.put(key, rec);
        }
    }

    public Object[] delete(K key) {
        synchronized (this) {
            return records.remove(key);
        }
    }

    public boolean addField(String field, byte type) {
        synchronized (this) {
            if (getFieldId(field) >= 0) {
                return true;
            }
            if (field == null) {
                return false;
            }
            String[] _f = new String[fields.length + 1];
            byte[] _t = new byte[fields.length + 1];
            System.arraycopy(fields, 0, _f, 0, fields.length);
            System.arraycopy(types, 0, _t, 0, fields.length);
            _f[fields.length] = field;
            _t[fields.length] = type;
            types = _t;
            fields = _f;
            Iterator<Map.Entry<K, Object[]>> it = records.entrySet().iterator();
            Map.Entry<K, Object[]> entry;
            Object[] old, ne;
            while (it.hasNext()) {
                entry = it.next();
                old = entry.getValue();
                ne = new Object[fields.length];
                System.arraycopy(old, 0, ne, 0, fields.length - 1);
                entry.setValue(ne);
            }
            return true;
        }
    }

    public void init() {
        synchronized (this) {
            if (records == null) {
                records = new HashMap<>();
            }
            if (fields == null) {
                fields = new String[0];
            }
            if (types == null) {
                types = new byte[0];
            }
            if (keyName == null) {
                keyName = "id";
            }
            if (keyType == 0) {
                keyType = TYPE_INT;
            }
        }
    }

    @NonNull
    public static Table fromBytes(@Nullable byte[] bytes) throws IOException {
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        return readTable(new DataInputStream(is));
    }

    @NonNull
    public synchronized byte[] toBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeTable(new DataOutputStream(baos), this);
        return baos.toByteArray();
    }
}
