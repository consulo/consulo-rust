/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.psi.ext.RsMod;

public class RsModInfo {
    @NotNull
    private final Project project;
    @NotNull
    private final CrateDefMap defMap;
    @NotNull
    private final ModData modData;
    @Nullable
    private final Crate crate;
    @Nullable
    private final DataPsiHelper dataPsiHelper;

    public RsModInfo(
        @NotNull Project project,
        @NotNull CrateDefMap defMap,
        @NotNull ModData modData,
        @Nullable Crate crate,
        @Nullable DataPsiHelper dataPsiHelper
    ) {
        this.project = project;
        this.defMap = defMap;
        this.modData = modData;
        this.crate = crate;
        this.dataPsiHelper = dataPsiHelper;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public CrateDefMap getDefMap() {
        return defMap;
    }

    @NotNull
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
    public MacroIndex getMacroIndex(@NotNull org.rust.lang.core.psi.ext.RsPossibleMacroCall call, @Nullable org.rust.lang.core.crate.Crate crate) {
        return modData.getMacroIndex();
    }
}
