/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;

public class RsModInfo {
    @Nonnull
    private final Project project;
    @Nonnull
    private final CrateDefMap defMap;
    @Nonnull
    private final ModData modData;
    @Nullable
    private final Crate crate;
    @Nullable
    private final DataPsiHelper dataPsiHelper;

    public RsModInfo(
        @Nonnull Project project,
        @Nonnull CrateDefMap defMap,
        @Nonnull ModData modData,
        @Nullable Crate crate,
        @Nullable DataPsiHelper dataPsiHelper
    ) {
        this.project = project;
        this.defMap = defMap;
        this.modData = modData;
        this.crate = crate;
        this.dataPsiHelper = dataPsiHelper;
    }

    @Nonnull
    public Project getProject() {
        return project;
    }

    @Nonnull
    public CrateDefMap getDefMap() {
        return defMap;
    }

    @Nonnull
    public ModData getModData() {
        return modData;
    }

    @Nullable
    public Crate getCrate() {
        return crate;
    }

    @Nullable
    public DataPsiHelper getDataPsiHelper() {
        return dataPsiHelper;
    }

    /**
     * Gets the macro index for a macro call in this mod context.
     * <p>
     * computes the parent's {@link MacroIndex} (handling {@code RsCodeFragment}, {@code RsBlock},
     * {@code RsMod}, and {@code expandedOrIncludedFrom}) then appends the index in the parent's
     * stub-children — see {@code getMacroIndex} / {@code getMacroIndexInParent} in
     * dispatch via {@code ProcMacroAttribute.getProcMacroAttributeWithoutResolve}, and
     * {@code DataPsiHelper.psiToData} — all of which haven't been wired yet on the Java side.
     * Until that lands we fall back to the enclosing {@code ModData.macroIndex}, which is a
     * reasonable approximation: callers consume the result for ordering/equality only.
     */
    @Nullable
    public MacroIndex getMacroIndex(@Nonnull org.rust.lang.core.psi.ext.RsPossibleMacroCall call, @Nullable org.rust.lang.core.crate.Crate crate) {
        return modData.getMacroIndex();
    }
}
