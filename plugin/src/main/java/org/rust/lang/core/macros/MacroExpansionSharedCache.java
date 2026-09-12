/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.disposer.Disposable;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.disposer.Disposer;
import com.intellij.psi.stubs.SerializedStubTree;
import consulo.language.psi.stub.SerializerNotFoundException;
import consulo.language.psi.stub.StubTreeBuilder;
import consulo.language.file.light.ReadOnlyLightVirtualFile;
import consulo.language.psi.stub.FileContent;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.macros.errors.MacroExpansionError;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.stdext.HashCode;
import org.rust.stdext.RsResult;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.annotation.component.ComponentScope;
import consulo.application.ApplicationManager;

/**
 * A persistent (stored on disk, in the real file system) cache for macro expansion text and stubs.
 * The cache is shared between different {@link Project}s (i.e. it's an application service).
 */
@SuppressWarnings("UnstableApiUsage")
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
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
    @Nonnull
    public <T extends RsMacroData, E extends MacroExpansionError> RsResult<ExpansionResultOk, E> cachedExpand(
        @Nonnull MacroExpander<T, E> expander,
        @Nonnull T def,
        @Nonnull RsMacroCallData call,
        @Nonnull HashCode mixHash
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
    public RsResult<ExpansionResultOk, MacroExpansionError> getExpansionIfCached(@Nonnull HashCode hash) {
        // Would query the persistent cache; returns null when cache is not available
        return null;
    }

    @Nullable
    public SerializedStubTree cachedBuildStub(@Nonnull FileContent fileContent, @Nonnull HashCode hash) {
        // Would query the persistent stub cache
        return null;
    }

    @Nonnull
    public <T extends RsMacroData, E extends MacroExpansionError> RsResult<Pair<RsFileStub, ExpansionResultOk>, E> createExpansionStub(
        @Nonnull Project project,
        @Nonnull MacroExpander<T, E> expander,
        @Nonnull T def,
        @Nonnull RsMacroCallData call,
        @Nonnull HashCode mixHash
    ) {
        RsResult<ExpansionResultOk, E> expandResult = cachedExpand(expander, def, call, mixHash);
        if (expandResult instanceof RsResult.Err) {
            @SuppressWarnings("unchecked")
            RsResult.Err<Pair<RsFileStub, ExpansionResultOk>, E> err = new RsResult.Err<>(((RsResult.Err<ExpansionResultOk, E>) expandResult).get());
            return err;
        }
        ExpansionResultOk resultOk = ((RsResult.Ok<ExpansionResultOk, E>) expandResult).get();

        ReadOnlyLightVirtualFile lightFile = new ReadOnlyLightVirtualFile("macro.rs", RsLanguage.INSTANCE, resultOk.getText());
        FileContent fc = new RsMacroExpansionFileContent(project, lightFile, resultOk.getText());

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

    @Nonnull
    public static MacroExpansionSharedCache getInstance() {
        return consulo.application.ApplicationManager.getApplication().getService(MacroExpansionSharedCache.class);
    }
}
