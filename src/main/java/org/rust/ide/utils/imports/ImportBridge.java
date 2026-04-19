/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.imports;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.settings.RsCodeInsightSettings;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * {@code org.rust.ide.utils.imports} package because {@code import} is a reserved Java keyword.
 */
public final class ImportBridge {
    private ImportBridge() {
    }

    public static void importElement(@NotNull RsElement context, @NotNull RsQualifiedNamedElement element) {
        importElements(context, Collections.singleton(element));
    }

    public static void importElements(@NotNull RsElement context, @NotNull Set<RsQualifiedNamedElement> elements) {
        if (!RsCodeInsightSettings.getInstance().importOutOfScopeItems) return;
        ImportContext importContext = ImportContext.from(context, ImportContext.Type.OTHER);
        if (importContext == null) return;
        for (RsQualifiedNamedElement element : elements) {
            ImportCandidate candidate = ImportCandidatesCollector.findImportCandidate(importContext, element);
            if (candidate != null) {
                importCandidate(candidate, context);
            }
        }
    }

    public static void importTypeReferencesFromElement(@NotNull RsElement context, @NotNull RsElement element) {
        // Type-reference collection is not yet ported; nothing to do until getTypeReferencesInfoFromElements
        // lands on the Java side.
    }

    public static void importTypeReferencesFromTy(@NotNull RsElement context, @NotNull Ty ty) {
        importTypeReferencesFromTys(context, Collections.singletonList(ty));
    }

    public static void importTypeReferencesFromTys(@NotNull RsElement context, @NotNull Collection<Ty> tys) {
        TypeReferencesInfo info = RsImportHelper.getTypeReferencesInfoFromTys(context, tys.toArray(new Ty[0]));
        importElements(context, info.getToImport());
    }

    // --- ImportInfoBridge methods ---

    @NotNull
    public static Object createLocalImportInfo(@NotNull String usePath) {
        return new ImportInfo.LocalImportInfo(usePath);
    }

    public static void doImport(@NotNull Object importInfo, @NotNull RsElement context) {
        if (!(importInfo instanceof ImportInfo)) return;
        ImportInfo info = (ImportInfo) importInfo;
        if (RsElementUtil.isIntentionPreviewElement(context)) return;

        info.insertExternCrateIfNeeded(context);

        RsPsiFactory psiFactory = new RsPsiFactory(context.getProject());
        RsMod insertionScope = RsElementUtil.getContainingMod(context);
        if (insertionScope == null) return;

        RsUseItem useItem = psiFactory.createUseItem(info.getUsePath());
        ImportUtils.insertUseItem(insertionScope, psiFactory, useItem);
    }

    /** {@code fun ImportCandidate.import(context) = info.import(context)}. */
    public static void importCandidate(@NotNull Object candidate, @NotNull RsElement context) {
        if (!(candidate instanceof ImportCandidate)) return;
        doImport(((ImportCandidate) candidate).getInfo(), context);
    }

    // --- stdlibAttributes bridge ---

    @NotNull
    public static RsFile.Attributes getStdlibAttributes(@NotNull RsElement element) {
        return ImportUtils.getStdlibAttributes(element);
    }
}
