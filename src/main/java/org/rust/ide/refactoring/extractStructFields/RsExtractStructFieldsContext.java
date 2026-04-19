/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractStructFields;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.refactoring.generate.StructMember;
import org.rust.lang.core.psi.RsStructItem;

import java.util.List;

public class RsExtractStructFieldsContext {
    @NotNull
    private final RsStructItem myStruct;
    @NotNull
    private final List<StructMember> myFields;
    @NotNull
    private final String myName;

    public RsExtractStructFieldsContext(
        @NotNull RsStructItem struct,
        @NotNull List<StructMember> fields,
        @NotNull String name
    ) {
        myStruct = struct;
        myFields = fields;
        myName = name;
    }

    @NotNull
    public RsStructItem getStruct() {
        return myStruct;
    }

    @NotNull
    public List<StructMember> getFields() {
        return myFields;
    }

    @NotNull
    public String getName() {
        return myName;
    }
}
