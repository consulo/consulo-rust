/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Used for optimization purposes, to reduce access to a cache and PSI tree in one very hot
 * place - processItemDeclarations.
 * <p>
 */
public class RsCachedItems {
    @NotNull
    private final List<RsUseItem> myImports;
    @NotNull
    private final List<RsMacro> myLegacyMacros;
    @NotNull
    private final Map<String, List<RsItemElement>> myNamed;

    private volatile List<RsItemElement> myCfgEnabledNamedItems;

    public RsCachedItems(@NotNull List<RsUseItem> imports,
                         @NotNull List<RsMacro> legacyMacros,
                         @NotNull Map<String, List<RsItemElement>> named) {
        myImports = imports;
        myLegacyMacros = legacyMacros;
        myNamed = named;
    }

    @NotNull
    public List<RsUseItem> getImports() {
        return myImports;
    }

    /** May contain cfg-disabled items. RsMacro2 are stored in named. */
    @NotNull
    public List<RsMacro> getLegacyMacros() {
        return myLegacyMacros;
    }

    /** May contain cfg-disabled items. */
    @NotNull
    public Map<String, List<RsItemElement>> getNamed() {
        return myNamed;
    }

    @NotNull
    public List<RsItemElement> getCfgEnabledNamedItems() {
        List<RsItemElement> result = myCfgEnabledNamedItems;
        if (result == null) {
            synchronized (this) {
                result = myCfgEnabledNamedItems;
                if (result == null) {
                    result = new ArrayList<>();
                    for (List<RsItemElement> items : myNamed.values()) {
                        for (RsItemElement item : items) {
                            if (isEnabledByCfgSelf(item)) {
                                result.add(item);
                            }
                        }
                    }
                    result = Collections.unmodifiableList(result);
                    myCfgEnabledNamedItems = result;
                }
            }
        }
        return result;
    }

    @Nullable
    public List<RsItemElement> getNamedElementsIfCfgEnabled(@NotNull String name) {
        List<RsItemElement> items = myNamed.get(name);
        if (items == null) return null;
        List<RsItemElement> enabled = new ArrayList<>();
        for (RsItemElement item : items) {
            if (isEnabledByCfgSelf(item)) {
                enabled.add(item);
            }
        }
        return enabled.isEmpty() ? null : enabled;
    }

    private static boolean isEnabledByCfgSelf(@NotNull RsItemElement item) {
        if (!(item instanceof RsDocAndAttributeOwner)) return true;
        return RsDocAndAttributeOwnerUtil.evaluateCfg((RsDocAndAttributeOwner) item, null) != ThreeValuedLogic.False;
    }
}
