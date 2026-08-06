/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractStructFields;

import jakarta.annotation.Nonnull;
import org.rust.ide.refactoring.generate.StructMember;
import org.rust.lang.core.psi.RsStructItem;

import java.util.List;

public class RsExtractStructFieldsContext {
    @Nonnull
    private final RsStructItem myStruct;
    @Nonnull
    private final List<StructMember> myFields;
    @Nonnull
    private final String myName;

    public RsExtractStructFieldsContext(
        @Nonnull RsStructItem struct,
        @Nonnull List<StructMember> fields,
        @Nonnull String name
    ) {
        myStruct = struct;
        myFields = fields;
        myName = name;
    }

    @Nonnull
    public RsStructItem getStruct() {
        return myStruct;
    }

    @Nonnull
    public List<StructMember> getFields() {
        return myFields;
    }

    @Nonnull
    public String getName() {
        return myName;
    }
}
