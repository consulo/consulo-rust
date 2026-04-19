/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.stubs.RsValueParameterStub;

public final class RsValueParameterUtil {
    private RsValueParameterUtil() {
    }

    @Nullable
    public static String getPatText(@NotNull RsValueParameter param) {
        RsValueParameterStub stub = RsPsiJavaUtil.getGreenStub(param);
        if (stub != null) {
            return stub.getPatText();
        }
        RsPat pat = param.getPat();
        return pat != null ? presentableText(pat) : null;
    }

    @NotNull
    private static String presentableText(@NotNull RsPat pat) {
        if (pat instanceof RsPatTup) {
            RsPatTup patTup = (RsPatTup) pat;
            StringBuilder sb = new StringBuilder("(");
            boolean first = true;
            for (RsPat p : patTup.getPatList()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(presentableText(p));
            }
            sb.append(")");
            return sb.toString();
        }
        if (pat instanceof RsPatSlice) {
            RsPatSlice patSlice = (RsPatSlice) pat;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (RsPat p : patSlice.getPatList()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(presentableText(p));
            }
            sb.append("]");
            return sb.toString();
        }
        if (pat instanceof RsPatStruct) {
            RsPatStruct patStruct = (RsPatStruct) pat;
            StringBuilder sb = new StringBuilder(patStruct.getPath().getText());
            sb.append(" {");
            boolean first = true;
            for (RsPatField pf : patStruct.getPatFieldList()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(patFieldPresentableText(pf));
            }
            sb.append("}");
            return sb.toString();
        }
        if (pat instanceof RsPatTupleStruct) {
            RsPatTupleStruct patTupleStruct = (RsPatTupleStruct) pat;
            StringBuilder sb = new StringBuilder(patTupleStruct.getPath().getText());
            sb.append("(");
            boolean first = true;
            for (RsPat p : patTupleStruct.getPatList()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(presentableText(p));
            }
            sb.append(")");
            return sb.toString();
        }
        if (pat instanceof RsPatIdent) {
            RsPatIdent patIdent = (RsPatIdent) pat;
            return patIdent.getPatBinding().getIdentifier().getText();
        }
        return pat.getText();
    }

    @NotNull
    private static String patFieldPresentableText(@NotNull RsPatField field) {
        RsPatBinding binding = field.getPatBinding();
        if (binding != null) {
            return binding.getIdentifier().getText();
        }
        RsPatFieldFull full = field.getPatFieldFull();
        if (full != null && full.getIdentifier() != null) {
            return full.getIdentifier().getText();
        }
        return "";
    }
}
