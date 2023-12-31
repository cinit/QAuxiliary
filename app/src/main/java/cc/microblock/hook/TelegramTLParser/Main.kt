/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier

fun toStringRecursive(obj: Any, indent: Int = 0, skipFirstIndent: Boolean = false): String {
    val className = obj.javaClass.simpleName
    val fields = getAllFields(obj.javaClass)

    val result = StringBuilder()
    if(skipFirstIndent) result.append("$className {\n")
    else result.append("${"  ".repeat(indent)}$className {\n")

    for (field in fields) {
        try {
            field.isAccessible = true
        } catch(e: Exception) {
            continue;
        }
        val fieldName = field.name
        val value = field.get(obj)

        result.append("${"  ".repeat(indent + 1)}$fieldName: ")

        when {
            value is Collection<*> -> {
                // Handle collections
                result.append("[\n")
                val collectionIndent = indent + 2
                for ((index, element) in (value as Collection<*>).withIndex()) {
                    result.append("${"  ".repeat(collectionIndent)}[$index] = ${toStringRecursive(element!!, collectionIndent + 1, true)}\n")
                }
                result.append("${"  ".repeat(indent + 1)}]\n")
            }
            value is Array<*> -> {
                // Handle arrays
                result.append("[\n")
                val arrayIndent = indent + 2
                for ((index, element) in (value as Array<*>).withIndex()) {
                    result.append("${"  ".repeat(arrayIndent)}[$index] = ${toStringRecursive(element!!, arrayIndent + 1, true)}\n")
                }
                result.append("${"  ".repeat(indent + 1)}]\n")
            }
            value != null && !field.type.isPrimitive && !field.type.isAssignableFrom(String::class.java) -> {
                // Handle non-primitive and non-string fields
                result.append("${toStringRecursive(value, indent + 1, true)}\n")
            }
            else -> {
                // Handle primitive and string fields
                result.append("$value\n")
            }
        }
    }

    result.append("${"  ".repeat(indent)}}")
    return result.toString()
}


fun getAllFields(cls: Class<*>): List<Field> {
    val fields = cls.declaredFields.toMutableList()
    val superClass = cls.superclass

    if (superClass != null) {
        fields.addAll(getAllFields(superClass))
    }

    return fields
}

//
//fun main(args: Array<String>) {
//    val stream = SerializedData(
////        File("J:/file_to_path.db")
//        File("C:\\Users\\MicroBlock\\Downloads\\cache4.db-stickersets-34-data.bin")
//    );
//
//    val set = TLRPC.TL_messages_stickerSet.TLdeserialize(stream, stream.readInt32(true), true);
//
//    println(toStringRecursive(set));
//}
