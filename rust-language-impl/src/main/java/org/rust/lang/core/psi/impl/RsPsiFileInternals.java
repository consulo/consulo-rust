/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.impl;

import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.psi.stub.PsiFileStub;
import jakarta.annotation.Nullable;
import org.rust.openapiext.OpenApiUtil;

import java.lang.reflect.Method;
import org.rust.lang.core.psi.*;

public final class RsPsiFileInternals {
    public static final RsPsiFileInternals INSTANCE = new RsPsiFileInternals();

    
    @Nullable
    public static final Method setStubTreeMethod;

    static {
        Method method;
        try {
            method = PsiFileImpl.class.getDeclaredMethod("setStubTree", PsiFileStub.class);
            method.setAccessible(true);
        } catch (Throwable e) {
            if (OpenApiUtil.isUnitTestMode()) {
                throw new RuntimeException(e);
            }
            method = null;
        }
        setStubTreeMethod = method;
    }

    public static boolean setStubTree(RsFile file, PsiFileStub<?> stub) {
        if (setStubTreeMethod != null) {
            try {
                setStubTreeMethod.invoke(file, stub);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private RsPsiFileInternals() {}
}
