/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.imports;

import jakarta.annotation.Nonnull;
import org.rust.cargo.util.AutoInjectedCrates;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.Testmark;

import java.util.List;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;

/**
 * Sealed class hierarchy for import information.
 */
public abstract class ImportInfo {
    @Nonnull
    public abstract String getUsePath();

    /**
     * Inserts an {@code extern crate} item if needed.
     * Only performs work if this is an {@link ExternCrateImportInfo}.
     *
     * @param context the PSI element providing the crate context
     */
    public void insertExternCrateIfNeeded(@Nonnull RsElement context) {
        // Default implementation does nothing for LocalImportInfo
    }

    private ImportInfo() {
    }

    public static final class LocalImportInfo extends ImportInfo {
        @Nonnull
        private final String usePath;

        public LocalImportInfo(@Nonnull String usePath) {
            this.usePath = usePath;
        }

        @Nonnull
        @Override
        public String getUsePath() {
            return usePath;
        }
    }

    public static final class ExternCrateImportInfo extends ImportInfo {
        @Nonnull
        private final Crate crate;
        @Nonnull
        private final String externCrateName;
        private final boolean needInsertExternCrateItem;
        @Nonnull
        private final String usePath;

        public ExternCrateImportInfo(
            @Nonnull Crate crate,
            @Nonnull String externCrateName,
            boolean needInsertExternCrateItem,
            @Nonnull String crateRelativePath,
            boolean hasModWithSameNameAsExternCrate
        ) {
            this.crate = crate;
            this.externCrateName = externCrateName;
            this.needInsertExternCrateItem = needInsertExternCrateItem;
            String absolutePrefix = hasModWithSameNameAsExternCrate ? "::" : "";
            String escapedName = RsPsiUtilUtil.escapeIdentifierIfNeeded(externCrateName);
            this.usePath = absolutePrefix + escapedName + "::" + crateRelativePath;
        }

        public ExternCrateImportInfo(
            @Nonnull Crate crate,
            @Nonnull String externCrateName,
            boolean needInsertExternCrateItem,
            @Nonnull String crateRelativePath
        ) {
            this(crate, externCrateName, needInsertExternCrateItem, crateRelativePath, false);
        }

        @Nonnull
        public Crate getCrate() {
            return crate;
        }

        @Nonnull
        public String getExternCrateName() {
            return externCrateName;
        }

        public boolean isNeedInsertExternCrateItem() {
            return needInsertExternCrateItem;
        }

        @Nonnull
        @Override
        public String getUsePath() {
            return usePath;
        }

        @Override
        public void insertExternCrateIfNeeded(@Nonnull RsElement context) {
            RsMod crateRoot = RsElementUtil.getCrateRoot(context);
            RsFile.Attributes attributes = crateRoot != null
                ? ImportUtils.getStdlibAttributes(crateRoot)
                : RsFile.Attributes.NONE;
            if (attributes == RsFile.Attributes.NONE && ImportUtils.isStd(crate)) {
                // std is auto-injected, no need for extern crate
                return;
            }
            if (attributes == RsFile.Attributes.NO_STD && ImportUtils.isCore(crate)) {
                // core is auto-injected when #![no_std], no need for extern crate
                return;
            }
            if (needInsertExternCrateItem && crateRoot instanceof RsMod) {
                insertExternCrateItem((RsMod) crateRoot, new RsPsiFactory(context.getProject()), externCrateName);
            }
        }

        private static void insertExternCrateItem(@Nonnull RsMod mod, @Nonnull RsPsiFactory psiFactory, @Nonnull String crateName) {
            String escapedName = RsPsiUtilUtil.escapeIdentifierIfNeeded(crateName);
            RsExternCrateItem externCrateItem = psiFactory.createExternCrateItem(escapedName);
            List<RsExternCrateItem> existingCrates = RsElementUtil.childrenOfType(mod, RsExternCrateItem.class);
            RsExternCrateItem lastExternCrate = ImportUtils.lastElement(existingCrates);
            if (lastExternCrate != null) {
                mod.addAfter(externCrateItem, lastExternCrate);
            } else {
                consulo.language.psi.PsiElement firstItem = RsItemsOwnerUtil.getFirstItem(mod);
                mod.addBefore(externCrateItem, firstItem);
                mod.addAfter(psiFactory.createNewline(), firstItem);
            }
        }
    }
}
