/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@State(name = "RsCodeInsightSettings", storages = @Storage("rust.xml"))
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

    @NotNull
    public ExcludedPath[] getExcludedPaths() {
        return excludedPaths != null ? excludedPaths : DEFAULT_EXCLUDED_PATHS;
    }

    public void setExcludedPaths(@NotNull ExcludedPath[] value) {
        excludedPaths = Arrays.equals(DEFAULT_EXCLUDED_PATHS, value) ? null : value;
    }

    @NotNull
    @Override
    public RsCodeInsightSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull RsCodeInsightSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public void dispose() {
    }

    @NotNull
    public static RsCodeInsightSettings getInstance() {
        return ApplicationManager.getApplication().getService(RsCodeInsightSettings.class);
    }
}
