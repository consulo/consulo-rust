/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsFile;
import org.toml.lang.psi.*;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralKt;

import java.util.Arrays;
import java.util.List;

public class CargoTomlFileReferenceProvider extends PsiReferenceProvider {
    private static final List<String> TARGET_TABLE_NAMES = Arrays.asList("lib", "bin", "test", "bench", "example");
    private static final List<String> KEYS_SUPPORTING_GLOB = Arrays.asList("members", "default-members");

    private final PathPatternType myPatternType;

    public CargoTomlFileReferenceProvider(@NotNull PathPatternType patternType) {
        myPatternType = patternType;
    }

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!(element instanceof TomlLiteral)) return PsiReference.EMPTY_ARRAY;
        Object kind = TomlLiteralKt.getKind((TomlLiteral) element);
        if (!(kind instanceof TomlLiteralKind.String)) return PsiReference.EMPTY_ARRAY;

        boolean completeDirs;
        boolean completeRustFiles;
        switch (myPatternType) {
            case WORKSPACE:
                completeDirs = true;
                completeRustFiles = false;
                break;
            case GENERAL: {
                completeDirs = true;
                TomlHeaderOwner table = PsiTreeUtil.getParentOfType(element, TomlHeaderOwner.class);
                if (table != null) {
                    TomlKey headerKey = table.getHeader().getKey();
                    String name = headerKey != null && !headerKey.getSegments().isEmpty()
                        ? headerKey.getSegments().get(headerKey.getSegments().size() - 1).getName()
                        : null;
                    completeRustFiles = name != null && TARGET_TABLE_NAMES.contains(name);
                } else {
                    completeRustFiles = false;
                }
                break;
            }
            case BUILD:
                completeDirs = false;
                completeRustFiles = true;
                break;
            default:
                completeDirs = true;
                completeRustFiles = false;
                break;
        }

        boolean ignoreGlobs = false;
        if (myPatternType == PathPatternType.WORKSPACE) {
            TomlKeyValue keyValue = PsiTreeUtil.getParentOfType(element, TomlKeyValue.class);
            if (keyValue != null) {
                List<TomlKeySegment> segments = keyValue.getKey().getSegments();
                if (!segments.isEmpty()) {
                    String firstName = segments.get(0).getName();
                    if (firstName != null && KEYS_SUPPORTING_GLOB.contains(firstName)) {
                        ignoreGlobs = true;
                    }
                }
            }
        }

        FileReferenceSet referenceSet;
        if (ignoreGlobs) {
            referenceSet = new GlobIgnoringFileReferenceSet((TomlLiteral) element, completeDirs, completeRustFiles);
        } else {
            referenceSet = new CargoTomlFileReferenceSet((TomlLiteral) element, completeDirs, completeRustFiles);
        }

        return referenceSet.getAllReferences();
    }

    private static class CargoTomlFileReferenceSet extends FileReferenceSet {
        private final boolean myCompleteDirs;
        private final boolean myCompleteRustFiles;

        public CargoTomlFileReferenceSet(@NotNull TomlLiteral element, boolean completeDirs, boolean completeRustFiles) {
            super(element);
            myCompleteDirs = completeDirs;
            myCompleteRustFiles = completeRustFiles;
        }

        @Override
        protected Condition<PsiFileSystemItem> getReferenceCompletionFilter() {
            return item -> {
                if (item instanceof PsiDirectory) return myCompleteDirs;
                if (item instanceof RsFile) return myCompleteRustFiles;
                return false;
            };
        }
    }

    private static class GlobIgnoringFileReferenceSet extends CargoTomlFileReferenceSet {
        private boolean myGlobPatternFound;

        public GlobIgnoringFileReferenceSet(@NotNull TomlLiteral element, boolean completeDirs, boolean completeRustFiles) {
            super(element, completeDirs, completeRustFiles);
        }

        @Override
        protected void reparse() {
            myGlobPatternFound = false;
            super.reparse();
        }

        @Nullable
        @Override
        public FileReference createFileReference(TextRange range, int index, String text) {
            if (!myGlobPatternFound && isGlobPathFragment(text)) {
                myGlobPatternFound = true;
            }
            if (myGlobPatternFound) return null;
            return super.createFileReference(range, index, text);
        }
    }

    private static boolean isGlobPathFragment(@Nullable String text) {
        if (text == null) return false;
        return text.contains("?") || text.contains("*") || text.contains("[") || text.contains("]");
    }
}
