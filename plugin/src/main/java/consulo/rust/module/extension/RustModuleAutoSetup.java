/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.module.extension;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.jdom.Element;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Remembers the modules of a project whose Rust support is settled - either found configured, or set
 * up automatically from a Rust bundle.
 * <p>
 * A module extension that is switched off writes nothing into the module file, so a module that has
 * never been configured and a module whose Rust support was switched off on purpose look exactly the
 * same. This record is what tells them apart: a module is configured automatically at most once, and
 * from then on it keeps whatever the user left it with.
 */
@State(name = "RustModuleAutoSetup", storages = {
    @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)
})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class RustModuleAutoSetup implements PersistentStateComponent<Element> {

    private static final String MODULE_ELEMENT = "module";
    private static final String NAME_ATTRIBUTE = "name";

    private final Set<String> myConfiguredModules = new LinkedHashSet<>();

    @Inject
    public RustModuleAutoSetup() {
    }

    @Nonnull
    public static RustModuleAutoSetup getInstance(@Nonnull Project project) {
        return project.getInstance(RustModuleAutoSetup.class);
    }

    /**
     * Records that the module named {@code moduleName} has its Rust support configured, and answers
     * whether that is the first time it was recorded. A {@code false} answer means the module has been
     * through this once already and has to be left exactly as the user left it.
     */
    public synchronized boolean markConfigured(@Nonnull String moduleName) {
        return myConfiguredModules.add(moduleName);
    }

    @Nonnull
    @Override
    public synchronized Element getState() {
        Element state = new Element("state");
        for (String moduleName : myConfiguredModules) {
            state.addContent(new Element(MODULE_ELEMENT).setAttribute(NAME_ATTRIBUTE, moduleName));
        }
        return state;
    }

    @Override
    public synchronized void loadState(@Nonnull Element state) {
        myConfiguredModules.clear();
        List<Element> modules = state.getChildren(MODULE_ELEMENT);
        for (Element module : modules) {
            String moduleName = module.getAttributeValue(NAME_ATTRIBUTE);
            if (moduleName != null) {
                myConfiguredModules.add(moduleName);
            }
        }
    }
}
