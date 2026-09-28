/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace.orderEntry;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.orderEntry.CustomOrderEntryTypeProvider;
import consulo.util.xml.serializer.InvalidDataException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.rust.cargo.project.workspace.CargoLibrary;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes the module entries that carry Cargo dependency sources.
 * <p>
 * The roots are written out, not only the coordinates, so that a project opened before its first sync
 * of the session still knows which directories belong to it.
 */
@ExtensionImpl
public class CargoLibraryOrderEntryType implements CustomOrderEntryTypeProvider<CargoLibraryOrderEntryModel> {

    public static final String ID = "cargo-library";

    private static final String KIND_ATTRIBUTE = "kind";
    private static final String ID_ATTRIBUTE = "id";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String VERSION_ATTRIBUTE = "version";
    private static final String MANIFEST_ATTRIBUTE = "manifest";

    private static final String SOURCE_ROOTS_ELEMENT = "sourceRoots";
    private static final String EXCLUDED_ROOTS_ELEMENT = "excludedRoots";
    private static final String ROOT_ELEMENT = "root";
    private static final String URL_ATTRIBUTE = "url";

    @Nonnull
    public static CargoLibraryOrderEntryType getInstance() {
        return EP.findExtensionOrFail(Application.get(), CargoLibraryOrderEntryType.class);
    }

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    /**
     * Exported, so that a workspace member module depending on the module that owns the manifest sees
     * the dependency roots without carrying a copy of every entry itself.
     */
    @Override
    public boolean isExportable() {
        return true;
    }

    @Nonnull
    @Override
    public CargoLibraryOrderEntryModel loadOrderEntry(
        @Nonnull Element element,
        @Nonnull ModuleRootLayer moduleRootLayer
    ) throws InvalidDataException {
        String name = element.getAttributeValue(NAME_ATTRIBUTE);
        if (name == null) {
            throw new InvalidDataException("Cargo library order entry without a name");
        }
        String id = element.getAttributeValue(ID_ATTRIBUTE);
        if (id == null) {
            id = name;
        }
        return new CargoLibraryOrderEntryModel(
            parseKind(element.getAttributeValue(KIND_ATTRIBUTE)),
            id,
            name,
            element.getAttributeValue(VERSION_ATTRIBUTE),
            element.getAttributeValue(MANIFEST_ATTRIBUTE),
            readRoots(element, SOURCE_ROOTS_ELEMENT),
            readRoots(element, EXCLUDED_ROOTS_ELEMENT)
        );
    }

    @Override
    public void storeOrderEntry(@Nonnull Element element, @Nonnull CargoLibraryOrderEntryModel model) {
        element.setAttribute(KIND_ATTRIBUTE, model.getKind().name());
        element.setAttribute(ID_ATTRIBUTE, model.getId());
        element.setAttribute(NAME_ATTRIBUTE, model.getName());
        String version = model.getVersion();
        if (version != null) {
            element.setAttribute(VERSION_ATTRIBUTE, version);
        }
        String manifestPath = model.getManifestPath();
        if (manifestPath != null) {
            element.setAttribute(MANIFEST_ATTRIBUTE, manifestPath);
        }
        if (model.getKind() == CargoLibrary.Kind.STDLIB) {
            // a standard library crate is found under the bundle by its name, so writing its roots would
            // only put the path of one machine's toolchain into a file that is shared
            return;
        }
        writeRoots(element, SOURCE_ROOTS_ELEMENT, model.getSourceRootUrls());
        writeRoots(element, EXCLUDED_ROOTS_ELEMENT, model.getExcludedRootUrls());
    }

    /**
     * @return {@code null} when the container is absent, which marks an entry written before the roots
     * were stored; an empty list when it is present and empty.
     */
    @Nullable
    private static List<String> readRoots(@Nonnull Element element, @Nonnull String containerName) {
        Element container = element.getChild(containerName);
        if (container == null) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (Element root : container.getChildren(ROOT_ELEMENT)) {
            String url = root.getAttributeValue(URL_ATTRIBUTE);
            if (url != null) {
                result.add(url);
            }
        }
        return result;
    }

    private static void writeRoots(
        @Nonnull Element element,
        @Nonnull String containerName,
        @Nullable List<String> urls
    ) {
        if (urls == null) {
            return;
        }
        Element container = new Element(containerName);
        for (String url : urls) {
            container.addContent(new Element(ROOT_ELEMENT).setAttribute(URL_ATTRIBUTE, url));
        }
        element.addContent(container);
    }

    @Nonnull
    private static CargoLibrary.Kind parseKind(@Nullable String value) {
        if (value != null) {
            for (CargoLibrary.Kind kind : CargoLibrary.Kind.values()) {
                if (kind.name().equals(value)) {
                    return kind;
                }
            }
        }
        return CargoLibrary.Kind.DEPENDENCY;
    }
}
