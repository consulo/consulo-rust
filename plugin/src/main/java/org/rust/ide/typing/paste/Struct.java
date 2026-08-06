/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.paste;

import java.util.LinkedHashMap;

// Structs with the same field names and types are considered the same, regardless of field order.
// LinkedHashMap::equals ignores key (field) order.
public final class Struct {
    private final LinkedHashMap<String, DataType> myFields;

    public Struct(LinkedHashMap<String, DataType> fields) {
        myFields = fields;
    }

    public LinkedHashMap<String, DataType> getFields() {
        return myFields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Struct struct = (Struct) o;
        return myFields.equals(struct.myFields);
    }

    @Override
    public int hashCode() {
        return myFields.hashCode();
    }

    @Override
    public String toString() {
        return "Struct(fields=" + myFields + ")";
    }
}
