/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.consts;

import com.intellij.codeInsight.completion.CompletionUtil;
import org.rust.lang.core.psi.RsConstParameter;
import org.rust.lang.core.types.KindUtil;

public class CtConstParameter extends Const {
    private final RsConstParameter myParameter;

    public CtConstParameter(RsConstParameter parameter) {
        super(KindUtil.HAS_CT_PARAMETER_MASK);
        myParameter = CompletionUtil.getOriginalOrSelf(parameter);
    }

    public RsConstParameter getParameter() {
        return myParameter;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CtConstParameter)) return false;
        return ((CtConstParameter) other).myParameter.equals(myParameter);
    }

    @Override
    public int hashCode() {
        return myParameter.hashCode();
    }

    @Override
    public String toString() {
        String name = myParameter.getName();
        return name != null ? name : "<unknown>";
    }
}
