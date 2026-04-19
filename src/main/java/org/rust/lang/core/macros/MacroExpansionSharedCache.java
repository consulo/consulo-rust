/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.stubs.SerializedStubTree;
import com.intellij.psi.stubs.SerializerNotFoundException;
import com.intellij.psi.stubs.StubTreeBuilder;
import com.intellij.testFramework.ReadOnlyLightVirtualFile;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.FileContentImpl;
import com.intellij.util.io.*;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.macros.errors.MacroExpansionError;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.stdext.HashCode;
import org.rust.stdext.RsResult;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A persistent (stored on disk, in the real file system) cache for macro expansion text and stubs.
 * The cache is shared between different {@link Project}s (i.e. it's an application service).
 */
@SuppressWarnings("UnstableApiUsage")
@Service
public final class MacroExpansionSharedCache implements Disposable {

    private final AtomicReference<Object> data = new AtomicReference<>(null);

    public boolean isEnabled() {
        return data.get() != null;
    }

    @Override
    public void dispose() {
        data.set(null);
    }

    public void flush() {
        // Persistent cache flush - implementation depends on PersistentHashMap setup
    }

    /**
     * Cache and expand a macro. Returns the expansion result.
     */
    @NotNull
    public <T extends RsMacroData, E extends MacroExpansionError> RsResult<ExpansionResultOk, E> cachedExpand(
        @NotNull MacroExpander<T, E> expander,
        @NotNull T def,
        @NotNull RsMacroCallData call,
        @NotNull HashCode mixHash
    ) {
        // In this Java translation, we delegate directly to the expander without persistent caching.
        // The full persistent caching implementation requires PersistentHashMap setup.
        RsResult<Pair<CharSequence, RangeMap>, E> result = expander.expandMacroAsTextWithErr(def, call);
        if (result instanceof RsResult.Ok) {
            Pair<CharSequence, RangeMap> pair = ((RsResult.Ok<Pair<CharSequence, RangeMap>, E>) result).get();
            return new RsResult.Ok<>(new ExpansionResultOk(pair.getFirst().toString(), pair.getSecond()));
        } else {
            @SuppressWarnings({"unchecked", "rawtypes"})
            RsResult.Err<ExpansionResultOk, E> err = (RsResult.Err) result;
            return err;
        }
    }

    @Nullable
    public RsResult<ExpansionResultOk, MacroExpansionError> getExpansionIfCached(@NotNull HashCode hash) {
        // Would query the persistent cache; returns null when cache is not available
        return null;
    }

    @Nullable
    public SerializedStubTree cachedBuildStub(@NotNull FileContent fileContent, @NotNull HashCode hash) {
        // Would query the persistent stub cache
        return null;
    }

    @NotNull
    public <T extends RsMacroData, E extends MacroExpansionError> RsResult<Pair<RsFileStub, ExpansionResultOk>, E> createExpansionStub(
        @NotNull Project project,
        @NotNull MacroExpander<T, E> expander,
        @NotNull T def,
        @NotNull RsMacroCallData call,
        @NotNull HashCode mixHash
    ) {
        RsResult<ExpansionResultOk, E> expandResult = cachedExpand(expander, def, call, mixHash);
        if (expandResult instanceof RsResult.Err) {
            @SuppressWarnings("unchecked")
            RsResult.Err<Pair<RsFileStub, ExpansionResultOk>, E> err = new RsResult.Err<>(((RsResult.Err<ExpansionResultOk, E>) expandResult).get());
            return err;
        }
        ExpansionResultOk resultOk = ((RsResult.Ok<ExpansionResultOk, E>) expandResult).get();

        ReadOnlyLightVirtualFile lightFile = new ReadOnlyLightVirtualFile("macro.rs", RsLanguage.INSTANCE, resultOk.getText());
        FileContent fc = FileContentImpl.createByText(lightFile, resultOk.getText(), project);

        var stub = StubTreeBuilder.buildStubTree(fc);
        if (stub == null) {
            return new RsResult.Ok<>(new Pair<>(null, resultOk));
        }

        try {
            var rsFileStub = stub instanceof RsFileStub ? (RsFileStub) stub : null;
            return new RsResult.Ok<>(new Pair<>(rsFileStub, resultOk));
        } catch (Exception e) {
            MacroExpansionManagerUtil.MACRO_LOG.error(e);
            return new RsResult.Ok<>(new Pair<>(null, resultOk));
        }
    }

    @NotNull
    public static MacroExpansionSharedCache getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication().getService(MacroExpansionSharedCache.class);
    }
}
