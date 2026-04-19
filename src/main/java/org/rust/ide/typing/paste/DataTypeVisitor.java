/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.paste;

import java.util.Map;

public class DataTypeVisitor {

    public void visit(DataType dataType) {
        if (dataType instanceof DataType.Array) {
            visitArray((DataType.Array) dataType);
        } else if (dataType instanceof DataType.Nullable) {
            visitNullable((DataType.Nullable) dataType);
        } else if (dataType instanceof DataType.StructRef) {
            visitStruct((DataType.StructRef) dataType);
        } else if (dataType instanceof DataType.BooleanType) {
            visitBoolean((DataType.BooleanType) dataType);
        } else if (dataType instanceof DataType.FloatType) {
            visitFloat((DataType.FloatType) dataType);
        } else if (dataType instanceof DataType.IntegerType) {
            visitInteger((DataType.IntegerType) dataType);
        } else if (dataType instanceof DataType.StringType) {
            visitString((DataType.StringType) dataType);
        } else if (dataType instanceof DataType.Unknown) {
            visitUnknown((DataType.Unknown) dataType);
        }
    }

    public void visitArray(DataType.Array dataType) {
        visit(dataType.getType());
    }

    public void visitNullable(DataType.Nullable dataType) {
        visit(dataType.getType());
    }

    public void visitStruct(DataType.StructRef dataType) {
        for (DataType fieldType : dataType.getStruct().getFields().values()) {
            visit(fieldType);
        }
    }

    public void visitInteger(DataType.IntegerType dataType) {
    }

    public void visitFloat(DataType.FloatType dataType) {
    }

    public void visitBoolean(DataType.BooleanType dataType) {
    }

    public void visitString(DataType.StringType dataType) {
    }

    public void visitUnknown(DataType.Unknown dataType) {
    }
}
