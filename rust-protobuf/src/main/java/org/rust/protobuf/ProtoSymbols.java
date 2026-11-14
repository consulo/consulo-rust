/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.protobuf;

import com.intellij.protobuf.lang.psi.PbEnumDefinition;
import com.intellij.protobuf.lang.psi.PbFile;
import com.intellij.protobuf.lang.psi.PbMessageBody;
import com.intellij.protobuf.lang.psi.PbMessageDefinition;
import com.intellij.protobuf.lang.psi.PbTypeName;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The messages and enums of a set of {@code .proto} files by fully qualified name ({@code .pkg.Outer.Inner}), and
 * the protobuf scoping rules to resolve a type name. Built from the PSI alone, so that it works while indexing.
 */
final class ProtoSymbols {
    /** A message or an enum, by its fully qualified name with a leading dot. */
    record Symbol(@Nonnull String fqName, @Nullable PbMessageDefinition message, @Nullable PbEnumDefinition enumDefinition) {
        boolean isMessage() {
            return message != null;
        }
    }

    private final Map<String, Symbol> symbols = new HashMap<>();

    ProtoSymbols(@Nonnull List<PbFile> files) {
        for (PbFile file : files) {
            String scope = packageScope(file);
            for (PbMessageDefinition message : PsiTreeUtil.getChildrenOfTypeAsList(file, PbMessageDefinition.class)) {
                addMessage(scope, message);
            }
            for (PbEnumDefinition enumDefinition : PsiTreeUtil.getChildrenOfTypeAsList(file, PbEnumDefinition.class)) {
                addEnum(scope, enumDefinition);
            }
        }
    }

    /** {@code .pkg} for a file of package {@code pkg}, the empty string for a file without a package. */
    @Nonnull
    static String packageScope(@Nonnull PbFile file) {
        String packageName = file.getPackageQualifiedName().toString();
        return packageName.isEmpty() ? "" : "." + packageName;
    }

    private void addMessage(@Nonnull String scope, @Nonnull PbMessageDefinition message) {
        String fqName = scope + "." + message.getName();
        symbols.put(fqName, new Symbol(fqName, message, null));
        PbMessageBody body = message.getBody();
        if (body == null) return;
        for (PbMessageDefinition nested : body.getMessageDefinitionList()) {
            addMessage(fqName, nested);
        }
        for (PbEnumDefinition nested : body.getEnumDefinitionList()) {
            addEnum(fqName, nested);
        }
    }

    private void addEnum(@Nonnull String scope, @Nonnull PbEnumDefinition enumDefinition) {
        String fqName = scope + "." + enumDefinition.getName();
        symbols.put(fqName, new Symbol(fqName, null, enumDefinition));
    }

    @Nullable
    Symbol get(@Nonnull String fqName) {
        return symbols.get(fqName);
    }

    /**
     * The message or enum {@code typeName} names, looked up like protoc does: a fully qualified name as it is, any
     * other name in the scope it is written in ({@code .pkg.Outer}), then in each enclosing scope.
     */
    @Nullable
    Symbol resolve(@Nonnull PbTypeName typeName, @Nonnull String scope) {
        String reference = typeName.getReferenceString();
        if (reference.startsWith(".")) return symbols.get(reference);
        String current = scope;
        while (true) {
            Symbol symbol = symbols.get(current + "." + reference);
            if (symbol != null) return symbol;
            if (current.isEmpty()) return null;
            int dot = current.lastIndexOf('.');
            current = dot <= 0 ? "" : current.substring(0, dot);
        }
    }
}
