/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.paste;

public final class Field {
    private final String myName;
    private final DataType myType;

    public Field(String name, DataType type) {
        myName = name;
        myType = type;
    }

    public String getName() {
        return myName;
    }

    public DataType getType() {
        return myType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Field field = (Field) o;
        return myName.equals(field.myName) && myType.equals(field.myType);
    }

    @Override
    public int hashCode() {
        int result = myName.hashCode();
        result = 31 * result + myType.hashCode();
        return result;
    }
}
