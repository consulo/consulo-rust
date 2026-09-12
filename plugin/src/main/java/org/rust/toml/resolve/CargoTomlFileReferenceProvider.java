/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;
import consulo.language.psi.PsiReferenceProvider;

import consulo.util.lang.function.Condition;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.ElementManipulator;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.ContributedReferenceHost;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.psi.path.FileReference;
import consulo.language.psi.path.FileReferenceSet;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsFile;
import org.toml.lang.psi.*;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralExt;

import java.util.Arrays;
import java.util.List;
import consulo.language.psi.PsiFileSystemItem;

public class CargoTomlFileReferenceProvider extends PsiReferenceProvider {
    private static final List<String> TARGET_TABLE_NAMES = Arrays.asList("lib", "bin", "test", "bench", "example");
    private static final List<String> KEYS_SUPPORTING_GLOB = Arrays.asList("members", "default-members");

    private final PathPatternType myPatternType;

    public CargoTomlFileReferenceProvider(@Nonnull PathPatternType patternType) {
        myPatternType = patternType;
    }

    @Nonnull
    @Override
    public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
        if (!(element instanceof TomlLiteral)) return PsiReference.EMPTY_ARRAY;
        Object kind = TomlLiteralExt.getKind((TomlLiteral) element);
        if (!(kind instanceof TomlLiteralKind.StringKind)) return PsiReference.EMPTY_ARRAY;

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

        public CargoTomlFileReferenceSet(@Nonnull TomlLiteral element, boolean completeDirs, boolean completeRustFiles) {
            super(element);
            myCompleteDirs = completeDirs;
            myCompleteRustFiles = completeRustFiles;
        }

        @Override
        public java.util.function.Predicate<consulo.language.psi.PsiFileSystemItem> getReferenceCompletionFilter() {
            return item -> {
                if (item instanceof PsiDirectory) return myCompleteDirs;
                if (item instanceof RsFile) return myCompleteRustFiles;
                return false;
            };
        }
    }

    private static class GlobIgnoringFileReferenceSet extends CargoTomlFileReferenceSet {
        private boolean myGlobPatternFound;

        public GlobIgnoringFileReferenceSet(@Nonnull TomlLiteral element, boolean completeDirs, boolean completeRustFiles) {
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
