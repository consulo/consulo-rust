/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace.library;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.library.DummyLibraryProperties;
import consulo.content.library.LibraryProperties;
import consulo.content.library.LibraryType;
import consulo.content.library.NewLibraryConfiguration;
import consulo.content.library.PersistentLibraryKind;
import consulo.content.library.ui.LibraryEditorComponent;
import consulo.content.library.ui.LibraryPropertiesEditor;
import consulo.project.Project;
import consulo.rust.icon.RustIconGroup;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JComponent;

/**
 * Marks the project libraries that carry the sources of Cargo dependencies, so that they are
 * recognisable in Project Structure and keep their kind across a reload.
 * <p>
 * The libraries follow {@code Cargo.toml} rather than the classpath editor, so the type offers no way
 * of creating or configuring one by hand.
 */
@ExtensionImpl
public class CargoLibraryType extends LibraryType<LibraryProperties> {

    public static final PersistentLibraryKind<LibraryProperties> KIND = new PersistentLibraryKind<>("cargo") {
        @Nonnull
        @Override
        public LibraryProperties createDefaultProperties() {
            return DummyLibraryProperties.INSTANCE;
        }
    };

    public CargoLibraryType() {
        super(KIND);
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Nullable
    @Override
    public String getCreateActionName() {
        return null;
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(
        @Nonnull JComponent parentComponent,
        @Nullable VirtualFile contextDirectory,
        @Nonnull Project project
    ) {
        return null;
    }

    @Nullable
    @Override
    public LibraryPropertiesEditor createPropertiesEditor(@Nonnull LibraryEditorComponent<LibraryProperties> editorComponent) {
        return null;
    }

    @Nullable
    @Override
    public Image getIcon() {
        return RustIconGroup.cargo();
    }
}
