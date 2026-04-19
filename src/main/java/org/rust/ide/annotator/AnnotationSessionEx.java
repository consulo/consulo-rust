/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.toml.CrateExt;
import org.rust.lang.core.psi.AttrCache;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.OpenApiUtil;

import java.util.Optional;

public final class AnnotationSessionEx {
    private static final Key<Optional<Crate>> CRATE_KEY = Key.create("org.rust.ide.annotator.currentCrate");
    private static final Key<AttrCache> ATTR_CACHE_KEY = Key.create("org.rust.ide.annotator.attrCache");

    private AnnotationSessionEx() {
    }

    @Nullable
    public static Crate currentCrate(AnnotationSession session) {
        Optional<Crate> optional = OpenApiUtil.getOrPut(session, CRATE_KEY, () -> {
            if (session.getFile() instanceof RsFile) {
                RsFile rsFile = (RsFile) session.getFile();
                Crate crate = rsFile.getCrate();
                if (crate != null) {
                    Crate notFake = Crate.asNotFake(crate);
                    return Optional.ofNullable(notFake);
                }
            }
            return Optional.empty();
        });
        return optional.orElse(null);
    }

    public static void setCurrentCrate(AnnotationSession session, @Nullable Crate crate) {
        session.putUserData(CRATE_KEY, Optional.ofNullable(crate));
    }

    @Nullable
    public static Crate currentCrate(AnnotationHolder holder) {
        return currentCrate(holder.getCurrentAnnotationSession());
    }

    public static AttrCache attrCache(AnnotationHolder holder) {
        AnnotationSession session = holder.getCurrentAnnotationSession();
        return OpenApiUtil.getOrPut(session, ATTR_CACHE_KEY, () ->
            new AttrCache.HashMapCache(currentCrate(session))
        );
    }
}
