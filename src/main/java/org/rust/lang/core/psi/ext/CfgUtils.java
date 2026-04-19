/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import com.intellij.psi.PsiElement;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;
import org.rust.lang.utils.evaluation.CfgEvaluator;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;

/**
 * See tests in RsCodeStatusTest.
 */
public final class CfgUtils {

    private CfgUtils() {
    }

    public static boolean isEnabledByCfg(PsiElement element) {
        return isEnabledByCfgInner(element, null);
    }

    public static boolean isEnabledByCfg(PsiElement element, Crate crate) {
        return isEnabledByCfgInner(element, crate);
    }

    public static boolean existsAfterExpansion(PsiElement element) {
        return existsAfterExpansion(element, null);
    }

    public static boolean existsAfterExpansion(PsiElement element, Crate crate) {
        RsCodeStatus status = getCodeStatus(element, crate);
        return status == RsCodeStatus.CODE || status == RsCodeStatus.CFG_UNKNOWN;
    }

    private static boolean isEnabledByCfgInner(PsiElement element, Crate crate) {
        RsCodeStatus status = getCodeStatus(element, crate);
        return status == RsCodeStatus.CODE || status == RsCodeStatus.ATTR_PROC_MACRO_CALL
            || status == RsCodeStatus.CFG_UNKNOWN;
    }

    public static boolean isCfgUnknown(PsiElement element) {
        PsiElement current = element;
        while (current != null) {
            if (current instanceof RsDocAndAttributeOwner) {
                if (RsDocAndAttributeOwnerUtil.isCfgUnknownSelf((RsDocAndAttributeOwner) current)) {
                    return true;
                }
            }
            current = current.getParent();
        }
        return false;
    }

    public static RsCodeStatus getCodeStatus(PsiElement element, Crate crate) {
        RsCodeStatus result = RsCodeStatus.CODE;
        PsiElement current = element;
        while (current != null) {
            if (current instanceof RsDocAndAttributeOwner) {
                RsDocAndAttributeOwner owner = (RsDocAndAttributeOwner) current;
                ThreeValuedLogic cfgResult = RsDocAndAttributeOwnerUtil.evaluateCfg(owner, crate);
                if (cfgResult == ThreeValuedLogic.False) return RsCodeStatus.CFG_DISABLED;
                if (cfgResult == ThreeValuedLogic.Unknown) result = RsCodeStatus.CFG_UNKNOWN;

                if (owner instanceof RsAttrProcMacroOwner) {
                    ProcMacroAttribute<?> attr = ProcMacroAttribute.getProcMacroAttribute(
                        (RsAttrProcMacroOwner) owner, null, crate, false, false);
                    if (attr instanceof ProcMacroAttribute.Attr) {
                        return RsCodeStatus.ATTR_PROC_MACRO_CALL;
                    }
                }
            }
            current = PsiElementUtil.getStubParent(current);
        }
        return result;
    }

    public static boolean isDisabledCfgAttrAttribute(RsAttr attr, Crate crate) {
        RsMetaItem metaItem = attr.getMetaItem();
        if (!"cfg_attr".equals(RsMetaItemUtil.getName(metaItem))) return false;
        RsMetaItemArgs args = metaItem.getMetaItemArgs();
        if (args == null) return false;
        java.util.List<RsMetaItem> metaItemList = args.getMetaItemList();
        if (metaItemList.isEmpty()) return false;
        RsMetaItem condition = metaItemList.get(0);
        return CfgEvaluator.forCrate(crate).evaluateCondition(condition) == ThreeValuedLogic.False;
    }
}
