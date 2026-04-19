/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.rust.RsBundle;
import org.rust.ide.utils.imports.ImportBridge;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.Namespace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public class AddImportIntention extends RsElementBaseIntentionAction<AddImportIntention.Context> {

    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.add.import");
    }

    public static class Context {
        public final RsPath path;
        public final boolean needsImport;

        public Context(RsPath path, boolean needsImport) {
            this.path = path;
            this.needsImport = needsImport;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        // ignore paths in use items
        if (PsiTreeUtil.getParentOfType(element, RsUseItem.class) != null) return null;

        RsPath path = PsiTreeUtil.getParentOfType(element, RsPath.class);
        if (path == null) return null;
        RsPath importablePath = findImportablePath(path);
        if (importablePath == null) return null;
        if (importablePath.getKind() != PathKind.IDENTIFIER
            || importablePath.getReference() == null
            || RsPathUtil.getResolveStatus(importablePath) != PathResolveStatus.RESOLVED) return null;

        // ignore paths that cannot be shortened
        if (importablePath.getPath() == null) return null;

        PsiElement resolved = importablePath.getReference().resolve();
        if (!(resolved instanceof RsQualifiedNamedElement)) return null;
        RsQualifiedNamedElement target = (RsQualifiedNamedElement) resolved;
        Set<?> namespace = Namespace.getNamespaces(target);

        // check if the name is already in scope in the same namespace
        String refName = importablePath.getReferenceName();
        if (refName == null) return null;
        PsiElement existingItem = org.rust.lang.core.resolve.NameResolution.findInScope(importablePath, refName, (java.util.Set<org.rust.lang.core.resolve.Namespace>) namespace);
        boolean sameReference = target.equals(existingItem);

        // if name exists, but is a different type, exit
        if (existingItem != null && !sameReference) {
            return null;
        }

        setText(RsBundle.message("intention.name.add.import.for", importablePath.getText()));
        return new Context(importablePath, existingItem == null);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        RsPath path = ctx.path;

        PsiElement resolved = path.getReference().resolve();
        if (!(resolved instanceof RsQualifiedNamedElement)) return;
        RsQualifiedNamedElement target = (RsQualifiedNamedElement) resolved;

        if (ctx.needsImport) {
            String usePath = generateUsePath(path);
            var importInfo = ImportBridge.createLocalImportInfo(usePath);
            ImportBridge.doImport(importInfo, path);
        }

        ReferencesSearch.search(target, new LocalSearchScope(RsPsiJavaUtil.getContainingMod(path))).forEach(ref -> {
            if (PsiTreeUtil.getParentOfType(ref.getElement(), RsUseItem.class) != null) return;
            if (!(ref.getElement() instanceof RsPath)) return;
            RsPath pathReference = (RsPath) ref.getElement();
            shorten(pathReference, target);
        });
    }

    @Override
    public IntentionPreviewInfo generatePreview(Project project, Editor editor, PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    /**
     * Shortens the path so that it resolves to target without any additional qualification.
     */
    private static void shorten(RsPath path, RsQualifiedNamedElement target) {
        if (path.getReference() != null) {
            PsiElement resolved = path.getReference().resolve();
            if (resolved instanceof RsQualifiedNamedElement) {
                if (resolved.equals(target)) {
                    RsPath subPath = path.getPath();
                    if (subPath != null) subPath.delete();
                    PsiElement coloncolon = path.getColoncolon();
                    if (coloncolon != null) coloncolon.delete();
                    return;
                }
            }
        }

        RsPath subPath = path.getPath();
        if (subPath != null) {
            shorten(subPath, target);
        }
    }

    /**
     * Finds the first part of the path that is possibly importable.
     */
    private static RsPath findImportablePath(RsPath path) {
        PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
        if (!(resolved instanceof RsQualifiedNamedElement)) {
            RsPath subPath = path.getPath();
            return subPath != null ? findImportablePath(subPath) : null;
        }

        RsQualifiedNamedElement target = (RsQualifiedNamedElement) resolved;
        boolean canBeImported;
        if (target instanceof RsStructOrEnumItemElement
            || target instanceof RsMod
            || target instanceof RsTraitAlias
            || target instanceof RsTraitItem
            || target instanceof RsTypeAlias
            || target instanceof RsEnumVariant
            || target instanceof RsMacro) {
            canBeImported = true;
        } else if (target instanceof RsAbstractable) {
            canBeImported = !((RsAbstractable) target).getOwner().isImplOrTrait();
        } else {
            canBeImported = false;
        }

        if (canBeImported) {
            return path;
        }
        RsPath subPath = path.getPath();
        return subPath != null ? findImportablePath(subPath) : null;
    }

    private static String generateUsePath(RsPath path) {
        List<String> parts = new ArrayList<>();
        RsPath current = path;
        while (current != null) {
            String name = current.getReferenceName();
            parts.add(name != null ? name : "");
            current = current.getPath();
        }
        Collections.reverse(parts);
        return String.join("::", parts);
    }
}
