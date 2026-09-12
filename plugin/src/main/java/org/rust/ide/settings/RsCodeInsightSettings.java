/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.settings;

import consulo.disposer.Disposable;
import consulo.application.ApplicationManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.annotation.component.ComponentScope;

@State(name = "RsCodeInsightSettings", storages = @Storage("rust"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class RsCodeInsightSettings implements PersistentStateComponent<RsCodeInsightSettings>, Disposable {

    public boolean showImportPopup = false;
    public boolean importOutOfScopeItems = true;
    public boolean suggestOutOfScopeItems = true;
    public boolean addUnambiguousImportsOnTheFly = false;
    public boolean importOnPaste = false;
    private ExcludedPath[] excludedPaths = null;

    private static final ExcludedPath[] DEFAULT_EXCLUDED_PATHS = {
        // These imports interfere with `RefCell::borrow` & `RefCell::borrow_mut` and methods from
        // them are very rarely needed (mostly inside a `HashMap` implementations).
        // See https://github.com/intellij-rust/intellij-rust/issues/5805
        new ExcludedPath("std::borrow::Borrow", ExclusionType.Methods),
        new ExcludedPath("std::borrow::BorrowMut", ExclusionType.Methods),
        new ExcludedPath("core::borrow::Borrow", ExclusionType.Methods),
        new ExcludedPath("core::borrow::BorrowMut", ExclusionType.Methods),
        new ExcludedPath("alloc::borrow::Borrow", ExclusionType.Methods),
        new ExcludedPath("alloc::borrow::BorrowMut", ExclusionType.Methods),
        // Functions from this module are often suggested instead of `panic!()` macro, also
        // it is always unstable (with a stable alternative - `panic!()` macro).
        // See https://github.com/intellij-rust/intellij-rust/issues/9157
        new ExcludedPath("core::panicking::*"),
        // This method is often suggested in completion instead of `unreachable!()` macro, also
        // it is always unstable (with a stable alternative - `core::hint::unreachable_unchecked`)
        new ExcludedPath("std::intrinsics::unreachable"),
    };

    @Nonnull
    public ExcludedPath[] getExcludedPaths() {
        return excludedPaths != null ? excludedPaths : DEFAULT_EXCLUDED_PATHS;
    }

    public void setExcludedPaths(@Nonnull ExcludedPath[] value) {
        excludedPaths = Arrays.equals(DEFAULT_EXCLUDED_PATHS, value) ? null : value;
    }

    @Nonnull
    @Override
    public RsCodeInsightSettings getState() {
        return this;
    }

    @Override
    public void loadState(@Nonnull RsCodeInsightSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public void dispose() {
    }

    @Nonnull
    public static RsCodeInsightSettings getInstance() {
        return ApplicationManager.getApplication().getService(RsCodeInsightSettings.class);
    }
}
