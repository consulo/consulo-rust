/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir;

import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.MirBody;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsInferenceContextOwner;
import org.rust.lang.core.psi.ext.RsInferenceContextOwnerUtil;

public final class MirExtensions {
    private MirExtensions() {
    }

    private static final Key<CachedValue<MirBody>> MIR_KEY = Key.create("org.rust.lang.core.mir.MIR_KEY");

    @Nullable
    public static MirBody getMirBody(@Nullable RsInferenceContextOwner owner) {
        if (owner == null) return null;
        return CachedValuesManager.getCachedValue(owner, MIR_KEY, () -> {
            MirBody mirBody;
            try {
                if (owner instanceof RsFunction) {
                    mirBody = MirBuilder.build((RsFunction) owner);
                } else if (owner instanceof RsConstant) {
                    mirBody = MirBuilder.build((RsConstant) owner);
                } else {
                    mirBody = null; // TODO support building MIR for more cases
                }
            } catch (UnsupportedOperationException ignored) {
                mirBody = null;
            } catch (IllegalStateException ignored) {
                // TODO use a special exception class if we can't build MIR
                mirBody = null;
            }
            return RsInferenceContextOwnerUtil.createCachedResult(owner, mirBody);
        });
    }
}
