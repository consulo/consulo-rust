/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.errors.ResolveMacroWithoutPsiError;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsMacroDefinitionBase;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import org.rust.lang.core.resolve2.DeclMacro2DefInfo;
import org.rust.lang.core.resolve2.DeclMacroDefInfo;
import org.rust.lang.core.resolve2.MacroDefInfo;
import org.rust.lang.core.resolve2.ProcMacroDefInfo;
import org.rust.stdext.HashCode;
import org.rust.stdext.RsResult;
import org.rust.cargo.api.workspace.CargoWorkspaceData;
import org.rust.lang.core.psi.RsMacroBody;

public class RsMacroDataWithHash<T extends RsMacroData> {
    private final T myData;
    private final HashCode myBodyHash;

    public RsMacroDataWithHash(@Nonnull T data, @Nullable HashCode bodyHash) {
        myData = data;
        myBodyHash = bodyHash;
    }

    @Nonnull
    public T getData() {
        return myData;
    }

    @Nullable
    public HashCode getBodyHash() {
        return myBodyHash;
    }

    @Nullable
    public HashCode mixHash(@Nonnull RsMacroCallDataWithHash call) {
        HashCode callHash;
        if (myData instanceof RsDeclMacroData) {
            callHash = call.getBodyHash();
        } else if (myData instanceof RsProcMacroData) {
            callHash = call.hashWithEnv();
        } else if (myData instanceof RsBuiltinMacroData) {
            callHash = call.getBodyHash();
        } else {
            throw new IllegalStateException("Unknown RsMacroData type: " + myData.getClass());
        }
        if (myBodyHash == null || callHash == null) return null;
        return HashCode.mix(myBodyHash, callHash);
    }

    @Nullable
    public static RsMacroDataWithHash<?> fromPsi(@Nonnull RsNamedElement def) {
        if (def instanceof RsMacroDefinitionBase) {
            RsMacroDefinitionBase macroDef = (RsMacroDefinitionBase) def;
            if (macroDef.getHasRustcBuiltinMacro()) {
                String name = macroDef.getName();
                if (name == null) return null;
                return new RsBuiltinMacroData(name).withHash();
            } else {
                return new RsMacroDataWithHash<>(new RsDeclMacroData(macroDef), macroDef.getBodyHash());
            }
        }
        if (def instanceof RsFunction && RsFunctionUtil.isProcMacroDef((RsFunction) def)) {
            RsFunction func = (RsFunction) def;
            String name = RsFunctionUtil.getProcMacroName(func);
            if (name == null) return null;
            Object procMacro = func.getContainingCrate().getProcMacroArtifact();
            if (procMacro == null) return null;
            org.rust.cargo.api.workspace.CargoWorkspaceData.ProcMacroArtifact artifact =
                (org.rust.cargo.api.workspace.CargoWorkspaceData.ProcMacroArtifact) procMacro;
            HashCode hash = HashCode.mix(artifact.getHash(), HashCode.compute(name));
            return new RsMacroDataWithHash<>(new RsProcMacroData(name, artifact), hash);
        }
        return null;
    }

    @Nonnull
    public static RsResult<RsMacroDataWithHash<?>, ResolveMacroWithoutPsiError> fromDefInfo(
        @Nonnull MacroDefInfo def,
        boolean errorIfIdentity
    ) {
        if (def instanceof DeclMacroDefInfo) {
            DeclMacroDefInfo declDef = (DeclMacroDefInfo) def;
            if (declDef.isHasRustcBuiltinMacro()) {
                return new RsResult.Ok<>(new RsBuiltinMacroData(declDef.getPath().getName()).withHash());
            } else {
                return new RsResult.Ok<>(new RsMacroDataWithHash<>(
                    new RsDeclMacroData((RsDeclMacroData.Lazy<org.rust.lang.core.psi.RsMacroBody>) () -> declDef.getBody()),
                    declDef.getBodyHash()
                ));
            }
        }
        if (def instanceof DeclMacro2DefInfo) {
            DeclMacro2DefInfo declDef = (DeclMacro2DefInfo) def;
            if (declDef.isHasRustcBuiltinMacro()) {
                return new RsResult.Ok<>(new RsBuiltinMacroData(declDef.getPath().getName()).withHash());
            } else {
                return new RsResult.Ok<>(new RsMacroDataWithHash<>(
                    new RsDeclMacroData((RsDeclMacroData.Lazy<org.rust.lang.core.psi.RsMacroBody>) () -> declDef.getBody()),
                    declDef.getBodyHash()
                ));
            }
        }
        if (def instanceof ProcMacroDefInfo) {
            ProcMacroDefInfo procDef = (ProcMacroDefInfo) def;
            String name = procDef.getPath().getName();
            org.rust.cargo.api.workspace.CargoWorkspaceData.ProcMacroArtifact procMacroArtifact =
                procDef.getProcMacroArtifact();
            if (procMacroArtifact == null) {
                return new RsResult.Err<>(ResolveMacroWithoutPsiError.NoProcMacroArtifact);
            }
            if (errorIfIdentity && procDef.getKind().getTreatAsBuiltinAttr()) {
                return new RsResult.Err<>(ResolveMacroWithoutPsiError.HardcodedProcMacroAttribute);
            }
            HashCode hash = HashCode.mix(procMacroArtifact.getHash(), HashCode.compute(name));
            return new RsResult.Ok<>(new RsMacroDataWithHash<>(new RsProcMacroData(name, procMacroArtifact), hash));
        }
        throw new IllegalStateException("Unknown MacroDefInfo type: " + def.getClass());
    }

    @Nonnull
    public static RsResult<RsMacroDataWithHash<?>, ResolveMacroWithoutPsiError> fromDefInfo(@Nonnull MacroDefInfo def) {
        return fromDefInfo(def, false);
    }
}
