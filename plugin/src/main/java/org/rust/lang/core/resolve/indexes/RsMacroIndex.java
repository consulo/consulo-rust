/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.indexes;

import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.IndexSink;
import consulo.language.psi.stub.StringStubIndexExtension;
import consulo.language.psi.stub.StubIndex;
import consulo.language.psi.stub.StubIndexKey;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import jakarta.annotation.Nonnull;
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
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.core.psi.ext.QueryAttributes;

@ExtensionImpl
public class RsMacroIndex extends StringStubIndexExtension<RsMacro> {

    private static final StubIndexKey<String, RsMacro> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.resolve.indexes.RsMacroIndex");

    private static final String SINGLE_KEY = "#";

    private static final Key<CachedValue<Map<RsMod, List<RsMacro>>>> EXPORTED_KEY = Key.create("EXPORTED_KEY");

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @Nonnull
    @Override
    public StubIndexKey<String, RsMacro> getKey() {
        return KEY;
    }

    public static void index(@Nonnull RsMacroStub stub, @Nonnull IndexSink sink) {
        // Raw (non-`cfg`-evaluated) attributes only: evaluating `cfg` here would need the containing
        // crate, which is not available while the file is being indexed
        QueryAttributes<?> attributes =
            RsDocAndAttributeOwnerUtil.getTraversedRawAttributes(stub.getPsi(), false);
        if (stub.getName() != null && (attributes.hasMacroExport() || attributes.isRustcDocOnlyMacro())) {
            sink.occurrence(KEY, SINGLE_KEY);
        }
    }

    @Nonnull
    public static Map<RsMod, List<RsMacro>> allExportedMacros(@Nonnull Project project) {
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
