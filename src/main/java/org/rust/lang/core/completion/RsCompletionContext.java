/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.infer.ExpectedType;

public class RsCompletionContext {
    @Nullable
    private final RsElement myContext;
    @Nullable
    private final ExpectedType myExpectedTy;
    private final boolean myIsSimplePath;
    @Nullable
    private final ImplLookup myLookup;

    public RsCompletionContext(@Nullable RsElement context, @Nullable ExpectedType expectedTy, boolean isSimplePath) {
        myContext = context;
        myExpectedTy = expectedTy;
        myIsSimplePath = isSimplePath;
        myLookup = context != null ? RsTypesUtil.getImplLookup(context) : null;
    }

    public RsCompletionContext() {
        this(null, null, false);
    }

    @Nullable
    public RsElement getContext() {
        return myContext;
    }

    @Nullable
    public ExpectedType getExpectedTy() {
        return myExpectedTy;
    }

    public boolean isSimplePath() {
        return myIsSimplePath;
    }

    @Nullable
    public ImplLookup getLookup() {
        return myLookup;
    }
}
