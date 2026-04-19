/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.refactoring.ExtraxtExpressionUtils;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;
import org.rust.lang.core.psi.ext.RsAbstractableUtil;

import java.util.*;

public class RsDuplicatedTraitMethodBindingInspection extends RsLocalInspectionTool {

    @Override
    public String getDisplayName() {
        return RsBundle.message("duplicated.trait.method.parameter.binding");
    }

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitFunction2(@NotNull RsFunction function) {
                if (!(RsAbstractableUtil.getOwner(function) instanceof RsAbstractableOwner.Trait)) return;
                if (!function.isAbstract()) return;

                RsValueParameterList parameters = function.getValueParameterList();
                if (parameters == null) return;
                Map<String, Set<RsPatBinding>> bindings = new LinkedHashMap<>();
                for (RsValueParameter param : parameters.getValueParameterList()) {
                    RsPat pat = param.getPat();
                    if (pat == null) continue;
                    RsPatBinding binding = ExtraxtExpressionUtils.findBinding(pat);
                    if (binding == null) continue;
                    String name = binding.getName();
                    if (name == null) continue;
                    Set<RsPatBinding> set = bindings.computeIfAbsent(name, k -> new LinkedHashSet<>());
                    set.add(binding);
                }

                for (Map.Entry<String, Set<RsPatBinding>> entry : bindings.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        for (RsPatBinding binding : entry.getValue()) {
                            String bindingName = binding.getName();
                            holder.registerProblem(
                                binding,
                                RsBundle.message("inspection.message.duplicated.parameter.name.consider.renaming.it", bindingName != null ? bindingName : "")
                            );
                        }
                    }
                }
            }
        };
    }
}
