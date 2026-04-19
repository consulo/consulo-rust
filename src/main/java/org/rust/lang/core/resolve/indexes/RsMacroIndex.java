/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.indexes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.psi.ext.RsAttrProcMacroOwnerUtil;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsMacroUtil;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.RsPsiManagerUtil;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsMacroStub;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;

public class RsMacroIndex extends StringStubIndexExtension<RsMacro> {

    private static final StubIndexKey<String, RsMacro> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.resolve.indexes.RsMacroIndex");

    private static final String SINGLE_KEY = "#";

    private static final Key<CachedValue<Map<RsMod, List<RsMacro>>>> EXPORTED_KEY = Key.create("EXPORTED_KEY");

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @NotNull
    @Override
    public StubIndexKey<String, RsMacro> getKey() {
        return KEY;
    }

    public static void index(@NotNull RsMacroStub stub, @NotNull IndexSink sink) {
        org.rust.lang.core.psi.ext.QueryAttributes<?> attributes = RsDocAndAttributeOwnerUtil.getQueryAttributes(stub.getPsi());
        if (stub.getName() != null && (RsMacroUtil.getHasMacroExport(stub.getPsi()) || RsMacroUtil.isRustcDocOnlyMacro(attributes))) {
            sink.occurrence(KEY, SINGLE_KEY);
        }
    }

    @NotNull
    public static Map<RsMod, List<RsMacro>> allExportedMacros(@NotNull Project project) {
        OpenApiUtil.checkCommitIsNotInProgress(project);
        return CachedValuesManager.getManager(project).getCachedValue(project, EXPORTED_KEY, () -> {
            HashMap<RsMod, List<RsMacro>> result = new HashMap<>();
            Collection<String> keys = StubIndex.getInstance().getAllKeys(KEY, project);
            for (String key : keys) {
                Collection<RsMacro> elements = OpenApiUtil.getElements(KEY, key, project, GlobalSearchScope.allScope(project));
                for (RsMacro element : elements) {
                    if (RsElementUtil.getContainingCrate(element) != null && (RsMacroUtil.getHasMacroExport(element) || RsMacroUtil.isRustcDocOnlyMacro(element))) {
                        RsMod crateRoot = element.getCrateRoot();
                        if (crateRoot == null) continue;
                        result.computeIfAbsent(crateRoot, k -> new ArrayList<>()).add(element);
                    }
                }
            }

            // remove macros with same names (may exist under #[cfg] attrs)
            for (List<RsMacro> macros : result.values()) {
                HashSet<String> names = new HashSet<>();
                HashSet<String> duplicatedNames = new HashSet<>();
                for (RsMacro macro : macros) {
                    String name = macro.getName();
                    if (name != null && !names.add(name)) {
                        duplicatedNames.add(name);
                    }
                }
                macros.removeIf(it -> duplicatedNames.contains(it.getName()));
            }
            return CachedValueProvider.Result.create(result, RsPsiManagerUtil.getRustStructureModificationTracker(project));
        }, false);
    }
}
