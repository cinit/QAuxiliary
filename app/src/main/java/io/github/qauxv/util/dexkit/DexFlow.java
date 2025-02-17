/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
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
package io.github.qauxv.util.dexkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.NonUiThread;
import io.github.qauxv.util.QQVersion;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class DexFlow {

    private DexFlow() {
        throw new AssertionError("No instances");
    }

    private static final byte[] OPCODE_LENGTH_TABLE = new byte[]{
            1, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 2, 3, 2, 2, 3, 5, 2, 2, 3, 2, 1, 1, 2,
            2, 1, 2, 2, 3, 3, 3, 1, 1, 2, 3, 3, 3, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1,
            1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3,
            3, 3, 3, 1, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4, 4, 3, 3, 2, 2
    };

    public static DexMethodDescriptor[] getDeclaredDexMethods(byte[] buf, String klass) {
        int methodIdsSize = readLe32(buf, 0x58);
        int methodIdsOff = readLe32(buf, 0x5c);
        int classDefsSize = readLe32(buf, 0x60);
        int classDefsOff = readLe32(buf, 0x64);
        int dexCodeOffset = -1;
        int[] p = new int[1];
        int[] ret = new int[1];
        int[] co = new int[1];
        for (int cn = 0; cn < classDefsSize; cn++) {
            int classIdx = readLe32(buf, classDefsOff + cn * 32);
            int classDataOff = readLe32(buf, classDefsOff + cn * 32 + 24);
            if (!klass.equals(readType(buf, classIdx))) {
                continue;
            }
            p[0] = classDataOff;
            if (classDataOff == 0) {
                continue;
            }
            int fieldIdx = 0;
            ArrayList<DexMethodDescriptor> methods = new ArrayList<>();
            int staticFieldsSize = readUleb128(buf, p),
                instanceFieldsSize = readUleb128(buf, p),
                directMethodsSize = readUleb128(buf, p),
                virtualMethodsSize = readUleb128(buf, p);
            for (int fn = 0; fn < staticFieldsSize + instanceFieldsSize; fn++) {
                fieldIdx += readUleb128(buf, p);
                int accessFlags = readUleb128(buf, p);
            }
            int methodIdx = 0;
            for (int mn = 0; mn < directMethodsSize; mn++) {
                methodIdx += readUleb128(buf, p);
                int accessFlags = readUleb128(buf, p);
                int codeOff = co[0] = readUleb128(buf, p);
                int pMethodId = methodIdsOff + 8 * methodIdx;
                String name = readString(buf, readLe32(buf, pMethodId + 4));
                String sig = readProto(buf, readLe16(buf, pMethodId + 2));
                methods.add(new DexMethodDescriptor(klass, name, sig));
            }
            methodIdx = 0;
            for (int mn = 0; mn < virtualMethodsSize; mn++) {
                methodIdx += readUleb128(buf, p);
                int accessFlags = readUleb128(buf, p);
                int codeOff = co[0] = readUleb128(buf, p);
                int pMethodId = methodIdsOff + 8 * methodIdx;
                String name = readString(buf, readLe32(buf, pMethodId + 4));
                String sig = readProto(buf, readLe16(buf, pMethodId + 2));
                methods.add(new DexMethodDescriptor(klass, name, sig));
            }
            return methods.toArray(new DexMethodDescriptor[0]);
        }
        return null;//class not found
    }

    @NonUiThread
    public static int getDexMethodOffset(@NonNull byte[] buf, @NonNull DexMethodDescriptor method) {
        int methodIdsSize = readLe32(buf, 0x58);
        int methodIdsOff = readLe32(buf, 0x5c);
        int classDefsSize = readLe32(buf, 0x60);
        int classDefsOff = readLe32(buf, 0x64);
        int dexCodeOffset = 0;
        int[] p = new int[1];
        int[] co = new int[1];
        main_loop:
        for (int cn = 0; cn < classDefsSize; cn++) {
            int classIdx = readLe32(buf, classDefsOff + cn * 32);
            int classDataOff = readLe32(buf, classDefsOff + cn * 32 + 24);
            if (!method.declaringClass.equals(readType(buf, classIdx))) {
                continue;
            }
            p[0] = classDataOff;
            if (classDataOff == 0) {
                continue;
            }
            int fieldIdx = 0;
            int staticFieldsSize = readUleb128(buf, p),
                instanceFieldsSize = readUleb128(buf, p),
                directMethodsSize = readUleb128(buf, p),
                virtualMethodsSize = readUleb128(buf, p);
            for (int fn = 0; fn < staticFieldsSize + instanceFieldsSize; fn++) {
                fieldIdx += readUleb128(buf, p);
                int accessFlags = readUleb128(buf, p);
            }
            int methodIdx = 0;
            for (int mn = 0; mn < directMethodsSize; mn++) {
                methodIdx += readUleb128(buf, p);
                int accessFlags = readUleb128(buf, p);
                int codeOff = co[0] = readUleb128(buf, p);
                if (codeOff == 0) {
                    continue;
                }
                int pMethodId = methodIdsOff + 8 * methodIdx;
                String name = readString(buf, readLe32(buf, pMethodId + 4));
                String sig = readProto(buf, readLe16(buf, pMethodId + 2));
                if (method.name.equals(name) && method.signature.equals(sig)) {
                    dexCodeOffset = codeOff;
                    break main_loop;
                }
            }
            methodIdx = 0;
            for (int mn = 0; mn < virtualMethodsSize; mn++) {
                methodIdx += readUleb128(buf, p);
                int accessFlags = readUleb128(buf, p);
                int codeOff = co[0] = readUleb128(buf, p);
                if (codeOff == 0) {
                    continue;
                }
                int pMethodId = methodIdsOff + 8 * methodIdx;
                String name = readString(buf, readLe32(buf, pMethodId + 4));
                String sig = readProto(buf, readLe16(buf, pMethodId + 2));
                if (method.name.equals(name) && method.signature.equals(sig)) {
                    dexCodeOffset = codeOff;
                    break main_loop;
                }
            }
        }
        return dexCodeOffset;
    }

    @NonUiThread
    @Deprecated
    public static String guessNewInstanceType(byte[] buf, DexMethodDescriptor method, DexFieldDescriptor field) throws NoSuchMethodException {
        Objects.requireNonNull(buf, "buf == null");
        Objects.requireNonNull(method, "method == null");
        Objects.requireNonNull(field, "field == null");
        int dexCodeOffset = getDexMethodOffset(buf, method);
        if (dexCodeOffset == 0) {
            throw new NoSuchMethodException(method.toString());
        }
        int registersSize = readLe16(buf, dexCodeOffset);
        int insSize = readLe16(buf, dexCodeOffset + 2);
        int outsSize = readLe16(buf, dexCodeOffset + 4);
        int triesSize = readLe16(buf, dexCodeOffset + 6);
        int insnsSize = readLe16(buf, dexCodeOffset + 12);
        int insnsOff = dexCodeOffset + 16;
        // we only handle new-instance and iput-object
        String[] regObjType = new String[insSize + outsSize];
        for (int i = 0; i < insnsSize; ) {
            int opv = buf[insnsOff + 2 * i] & 0xff;
            int len = OPCODE_LENGTH_TABLE[opv];
            if (len == 0) {
                throw new RuntimeException(String.format(Locale.ROOT, "Unrecognized opcode = 0x%02x", opv));
            }
            if (opv == 0x22) {
                //new-instance
                int reg = buf[insnsOff + 2 * i + 1] & 0xff;
                int typeId = readLe16(buf, insnsOff + 2 * i + 2);
                regObjType[reg] = readType(buf, typeId);
            } else if (opv == 0x5b) {
                //iput-object
                int regs = buf[insnsOff + 2 * i + 1] & 0xff;
                int val = regs & 0x0F;
                //int obj = regs & 0xF0;//who cares?
                int fieldId = readLe16(buf, insnsOff + 2 * i + 2);
                if (field.equals(readField(buf, fieldId))) {
                    return regObjType[val];
                }
            }
            i += len;
        }
        return null;
    }

    @NonUiThread
    public static ArrayList<Integer> getViewSetIdP1Values(byte[] buf, DexMethodDescriptor method) throws NoSuchMethodException {
        Objects.requireNonNull(buf, "buf == null");
        Objects.requireNonNull(method, "method == null");
        int dexCodeOffset = getDexMethodOffset(buf, method);
        if (dexCodeOffset == 0) {
            throw new NoSuchMethodException(method.toString());
        }
        int registersSize = readLe16(buf, dexCodeOffset);
        int insSize = readLe16(buf, dexCodeOffset + 2);
        int outsSize = readLe16(buf, dexCodeOffset + 4);
        int triesSize = readLe16(buf, dexCodeOffset + 6);
        int insnsSize = readLe16(buf, dexCodeOffset + 12);
        int insnsOff = dexCodeOffset + 16;
        // we only handle const and invoke-virtual Landroid/widget/TextView;->setId(I)V
        // on 9.1.50+, it called Landroid/view/View;->setId(I)V
        String targetMethodDesc = "Landroid/widget/TextView;->setId(I)V";
        if (HostInfo.requireMinQQVersion(QQVersion.QQ_9_1_50)){
            targetMethodDesc = "Landroid/view/View;->setId(I)V";
        }
        Integer[] regObjType = new Integer[registersSize];
        ArrayList<Integer> results = new ArrayList<>();
        int pc = 0; // program counter
        while (pc < insnsSize) {
            int opv = buf[insnsOff + 2 * pc] & 0xff;
            int len = OPCODE_LENGTH_TABLE[opv];
            if (len == 0) {
                throw new RuntimeException(String.format(Locale.ROOT, "Unrecognized opcode = 0x%02x", opv));
            }
            if (opv == 0x14) {
                // 14 31i const vAA, #+BBBBBBBB
                int reg = buf[insnsOff + 2 * pc + 1] & 0xff;
                int valLow16 = readLe16(buf, insnsOff + 2 * pc + 2);
                int valHigh16 = readLe16(buf, insnsOff + 2 * pc + 4);
                int value = valLow16 | (valHigh16 << 16) & 0xffff0000;
                regObjType[reg] = value;
            } else if (opv == 0x6e) {
                // 6e 35c invoke-kind {vC, vD, vE, vF, vG}, meth@BBBB
                // [A=2] op {vC, vD}, kind@BBBB
                int methodIdx = readLe16(buf, insnsOff + 2 * pc + 2);
                DexMethodDescriptor m = readMethod(buf, methodIdx);
                if (m.getDescriptor().equals(targetMethodDesc)) {
                    // get p1/C reg index
                    int insOffset4 = readLe16(buf, insnsOff + 2 * pc + 4);
                    int regIndex = (insOffset4 >> 4) & 0xf;
                    if (regObjType[regIndex] != null) {
                        results.add(regObjType[regIndex]);
                    }
                }
            }
            pc += len;
        }
        return results;
    }

    @NonUiThread
    public static DexFieldDescriptor guessFieldByNewInstance(byte[] buf, DexMethodDescriptor method,
                                                             Class<?> instanceClass) throws NoSuchMethodException {
        if (instanceClass == null) {
            throw new NullPointerException("instanceClass == null");
        }
        return guessFieldByNewInstance(buf, method,
            "L" + instanceClass.getName().replace('.', '/') + ";");
    }

    @NonUiThread
    @Deprecated
    public static DexFieldDescriptor guessFieldByNewInstance(byte[] buf, DexMethodDescriptor method,
                                                             String instanceClass) throws NoSuchMethodException {
        if (buf == null) {
            throw new NullPointerException("dex == null");
        }
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        if (instanceClass == null) {
            throw new NullPointerException("instanceClass == null");
        }
        int methodIdsSize = readLe32(buf, 0x58);
        int methodIdsOff = readLe32(buf, 0x5c);
        int classDefsSize = readLe32(buf, 0x60);
        int classDefsOff = readLe32(buf, 0x64);
        int dexCodeOffset = -1;
        int[] p = new int[1];
        int[] ret = new int[1];
        int[] co = new int[1];
        main_loop:
        for (int cn = 0; cn < classDefsSize; cn++) {
            int classIdx = readLe32(buf, classDefsOff + cn * 32);
            int classDataOff = readLe32(buf, classDefsOff + cn * 32 + 24);
            if (!method.declaringClass.equals(readType(buf, classIdx))) {
                continue;
            }
            p[0] = classDataOff;
            if (classDataOff == 0) {
                continue;
            }
            int fieldIdx = 0;
            int staticFieldsSize = readUleb128(buf, p),
                instanceFieldsSize = readUleb128(buf, p),
                directMethodsSize = readUleb128(buf, p),
                virtualMethodsSize = readUleb128(buf, p);
            for (int fn = 0; fn < staticFieldsSize + instanceFieldsSize; fn++) {
                fieldIdx += readUleb128(buf, p);
                int accessFlags = readUleb128(buf, p);
            }
            int methodIdx = 0;
            for (int mn = 0; mn < directMethodsSize; mn++) {
                methodIdx += readUleb128(buf, p);
                int accessFlags = readUleb128(buf, p);
                int codeOff = co[0] = readUleb128(buf, p);
                if (codeOff == 0) {
                    continue;
                }
                int pMethodId = methodIdsOff + 8 * methodIdx;
                String name = readString(buf, readLe32(buf, pMethodId + 4));
                String sig = readProto(buf, readLe16(buf, pMethodId + 2));
                if (method.name.equals(name) && method.signature.equals(sig)) {
                    dexCodeOffset = codeOff;
                    break main_loop;
                }
            }
            methodIdx = 0;
            for (int mn = 0; mn < virtualMethodsSize; mn++) {
                methodIdx += readUleb128(buf, p);
                int accessFlags = readUleb128(buf, p);
                int codeOff = co[0] = readUleb128(buf, p);
                if (codeOff == 0) {
                    continue;
                }
                int pMethodId = methodIdsOff + 8 * methodIdx;
                String name = readString(buf, readLe32(buf, pMethodId + 4));
                String sig = readProto(buf, readLe16(buf, pMethodId + 2));
                if (method.name.equals(name) && method.signature.equals(sig)) {
                    dexCodeOffset = codeOff;
                    break main_loop;
                }
            }
        }
        if (dexCodeOffset == -1) {
            throw new NoSuchMethodException(method.toString());
        }
        int registersSize = readLe16(buf, dexCodeOffset);
        int insSize = readLe16(buf, dexCodeOffset + 2);
        int outsSize = readLe16(buf, dexCodeOffset + 4);
        int triesSize = readLe16(buf, dexCodeOffset + 6);
        int insnsSize = readLe16(buf, dexCodeOffset + 12);
        int insnsOff = dexCodeOffset + 16;
        //we only handle new-instance and iput-object
        String[] regObjType = new String[insSize + outsSize];
        for (int i = 0; i < insnsSize; ) {
            int opv = buf[insnsOff + 2 * i] & 0xff;
            int len = OPCODE_LENGTH_TABLE[opv];
            if (len == 0) {
                throw new RuntimeException(String.format(Locale.ROOT, "Unrecognized opcode = 0x%02x", opv));
            }
            if (opv == 0x22) {
                //new-instance
                int reg = buf[insnsOff + 2 * i + 1] & 0xff;
                int typeId = readLe16(buf, insnsOff + 2 * i + 2);
                regObjType[reg] = readType(buf, typeId);
            } else if (opv == 0x5b) {
                //iput-object
                int regs = buf[insnsOff + 2 * i + 1] & 0xff;
                int val = regs & 0x0F;
                //int obj = regs & 0xF0;//who cares?
                int fieldId = readLe16(buf, insnsOff + 2 * i + 2);
                if (instanceClass.equals(regObjType[val])) {
                    return readField(buf, fieldId);
                }
            }
            i += len;
        }
        return null;
    }
    //struct DexCode {
    //0   u2  registersSize;
    //2   u2  insSize;
    //4   u2  outsSize;
    //6   u2  triesSize;
    //8   u4  debugInfoOff;       /* file offset to debug info stream */
    //12  u4  insnsSize;          /* size of the insns array, in u2 units */
    //16  u2  insns[1];
    //    /* followed by optional u2 padding */
    //    /* followed by try_item[triesSize] */
    //    /* followed by uleb128 handlersSize */
    //    /* followed by catch_handler_item[handlersSize] */
    //};

    public static boolean hasClassInDex(byte[] dex, String clz) {
        if (!clz.endsWith(";")) {
            clz = "L" + clz.replace('.', '/') + ";";
        }
        int classDefsSize = readLe32(dex, 0x60);
        int classDefsOff = readLe32(dex, 0x64);
        for (int cn = 0; cn < classDefsSize; cn++) {
            int classIdx = readLe32(dex, classDefsOff + cn * 32);
            String c = readType(dex, classIdx);
            if (clz.equals(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param buf       the byte array containing the whole dex file
     * @param opcodeOff offset relative to {@code buf}
     * @param verify    whether to verify if the {@code opcodeOff} is aligned to opcode, return {@code null} if the
     *                  offset failed the verification
     */
    @Nullable
    public static DexMethodDescriptor getDexMethodByOpOffset(byte[] buf, int opcodeOff,
                                                             boolean verify) {
        int methodIdsSize = readLe32(buf, 0x58);
        int methodIdsOff = readLe32(buf, 0x5c);
        int classDefsSize = readLe32(buf, 0x60);
        int classDefsOff = readLe32(buf, 0x64);
        int[] p = new int[1];
        int[] ret = new int[1];
        int[] co = new int[1];
        for (int cn = 0; cn < classDefsSize; cn++) {
            int classIdx = readLe32(buf, classDefsOff + cn * 32);
            int classDataOff = readLe32(buf, classDefsOff + cn * 32 + 24);
            p[0] = classDataOff;
            if (classDataOff == 0) {
                continue;
            }
            int fieldIdx = 0;
            int staticFieldsSize = readUleb128(buf, p),
                instanceFieldsSize = readUleb128(buf, p),
                directMethodsSize = readUleb128(buf, p),
                virtualMethodsSize = readUleb128(buf, p);
            for (int fn = 0; fn < staticFieldsSize + instanceFieldsSize; fn++) {
                fieldIdx += readUleb128(buf, p);
                int accessFlags = readUleb128(buf, p);
            }
            int methodIdx = 0;
            for (int mn = 0; mn < directMethodsSize; mn++) {
                methodIdx += readUleb128(buf, p);
                int accessFlags = readUleb128(buf, p);
                int codeOff = co[0] = readUleb128(buf, p);
                if (codeOff == 0) {
                    continue;
                }
                int insnsSize = readLe32(buf, codeOff + 12);
                if (codeOff + 16 <= opcodeOff && opcodeOff <= codeOff + 16 + insnsSize * 2) {
                    if (verify && !verifyOpcodeOffset(buf, codeOff + 16, insnsSize * 2,
                        opcodeOff)) {
                        return null;
                    }
                    String clz = readType(buf, classIdx);
                    int pMethodId = methodIdsOff + 8 * methodIdx;
                    String name = readString(buf, readLe32(buf, pMethodId + 4));
                    String sig = readProto(buf, readLe16(buf, pMethodId + 2));
                    return new DexMethodDescriptor(clz, name, sig);
                }
            }
            methodIdx = 0;
            for (int mn = 0; mn < virtualMethodsSize; mn++) {
                methodIdx += readUleb128(buf, p);
                int accessFlags = readUleb128(buf, p);
                int codeOff = co[0] = readUleb128(buf, p);
                if (codeOff == 0) {
                    continue;
                }
                int insnsSize = readLe32(buf, codeOff + 12);
                if (codeOff + 16 <= opcodeOff && opcodeOff <= codeOff + 16 + insnsSize * 2) {
                    if (verify && !verifyOpcodeOffset(buf, codeOff + 16, insnsSize * 2,
                        opcodeOff)) {
                        return null;
                    }
                    String clz = readType(buf, classIdx);
                    int pMethodId = methodIdsOff + 8 * methodIdx;
                    String name = readString(buf, readLe32(buf, pMethodId + 4));
                    String sig = readProto(buf, readLe16(buf, pMethodId + 2));
                    return new DexMethodDescriptor(clz, name, sig);
                }
            }
        }
        return null;
    }

    public static int readUleb128(byte[] src, int[] offset) {
        int result = 0;
        int count = 0;
        int cur;
        do {
            cur = src[offset[0]];
            cur &= 0xff;
            result |= (cur & 0x7f) << count * 7;
            count++;
            offset[0]++;
        } while ((cur & 0x80) == 128 && count < 5);
        return result;
    }

    public static String readString(byte[] buf, int idx) {
        int stringIdsOff = readLe32(buf, 0x3c);
        int strOff = readLe32(buf, stringIdsOff + 4 * idx);
        int[] ppos = new int[1];
        ppos[0] = strOff;
        int len = readUleb128(buf, ppos);
        return new String(buf, ppos[0], len);
    }

    public static String readType(byte[] buf, int idx) {
        int typeIdsOff = readLe32(buf, 0x44);
        int strIdx = readLe32(buf, typeIdsOff + 4 * idx);
        return readString(buf, strIdx);
    }

    public static DexFieldDescriptor readField(byte[] buf, int idx) {
        int fieldIdsOff = readLe32(buf, 0x54);
        int p = fieldIdsOff + 8 * idx;
        String clz = readType(buf, readLe16(buf, p));
        String type = readType(buf, readLe16(buf, p + 2));
        String name = readString(buf, readLe32(buf, p + 4));
        return new DexFieldDescriptor(clz, name, type);
    }

    public static DexMethodDescriptor readMethod(byte[] buf, int idx) {
        int methodIdsOff = readLe32(buf, 0x5c);
        int p = methodIdsOff + 8 * idx;
        String clz = readType(buf, readLe16(buf, p));
        String sig = readProto(buf, readLe16(buf, p + 2));
        String name = readString(buf, readLe32(buf, p + 4));
        return new DexMethodDescriptor(clz, name, sig);
    }

    public static String readProto(byte[] buf, int idx) {
        int protoIdsOff = readLe32(buf, 0x4c);
        int returnTypeIdx = readLe32(buf, protoIdsOff + 12 * idx + 4);
        int parametersOff = readLe32(buf, protoIdsOff + 12 * idx + 8);
        StringBuilder sb = new StringBuilder("(");
        if (parametersOff != 0) {
            int size = readLe32(buf, parametersOff);
            for (int i = 0; i < size; i++) {
                int typeIdx = readLe16(buf, parametersOff + 4 + 2 * i);
                sb.append(readType(buf, typeIdx));
            }
        }
        sb.append(")");
        sb.append(readType(buf, returnTypeIdx));
        return sb.toString();
    }

    public static int arrayIndexOf(byte[] arr, byte[] subArr, int startIndex, int endIndex) {
        byte a = subArr[0];
        float d = endIndex - startIndex;
        int b = endIndex - subArr.length;
        int i = startIndex;
        int ii;
        a:
        while (i <= b) {
            if (arr[i] == a) {
                for (ii = 0; ii < subArr.length; ii++) {
                    if (arr[i++] != subArr[ii]) {
                        i = i - ii;
                        continue a;
                    }
                }
                return i - ii;
            } else {
                i++;
            }
        }
        return -1;
    }

    public static byte[] int2u4le(int i) {
        return new byte[]{(byte) i, (byte) (i >> 8), (byte) (i >> 16), (byte) (i >> 24)};
    }

    public static byte[] int2u2le(int i) {
        return new byte[]{(byte) i, (byte) (i >> 8)};
    }

    public static int readLe32(byte[] buf, int index) {
        return buf[index] & 0xFF | (buf[index + 1] << 8) & 0xff00
            | (buf[index + 2] << 16) & 0xff0000 | (buf[index + 3] << 24) & 0xff000000;
    }

    public static int readLe16(byte[] buf, int off) {
        return (buf[off] & 0xFF) | ((buf[off + 1] << 8) & 0xff00);
    }

    public static boolean verifyOpcodeOffset(byte[] buf, int insStart, int bLen, int opcodeOffset) {
        for (int i = 0; i < bLen; ) {
            if (insStart + i == opcodeOffset) {
                return true;
            }
            int opv = buf[insStart + i] & 0xff;
            int len = OPCODE_LENGTH_TABLE[opv];
            if (len == 0) {
                Log.e(String.format(Locale.ROOT, "Unrecognized opcode = 0x%02x", opv));
                return false;
            }
            i += 2 * len;
        }
        return false;
    }

    @NonNull
    public static byte[] packUtf8(@NonNull String string) {
        if (string.length() > 127) {
            throw new UnsupportedOperationException("String too long");
        }
        byte[] tmp = string.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[tmp.length + 1];
        result[0] = (byte) tmp.length;
        System.arraycopy(tmp, 0, result, 1, tmp.length);
        return result;
    }

    @NonNull
    public static byte[] packUtf8(@NonNull String str, int len) {
        // dex uses utf-8 encoded utf-16, s.t. even if the str is complete, str.length != str.bytes.length
        if (len < 0 || len > 127 || str.length() > 127) {
            throw new IllegalArgumentException("String too long");
        }
        byte[] u8 = str.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[u8.length + 1];
        result[0] = (byte) len;
        System.arraycopy(u8, 0, result, 1, u8.length);
        return result;
    }
}
