/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.types.regions.Scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Scopes {
    @Nullable
    private IfThenScope ifThenScope;
    @NotNull
    private final DropTree unwindDrops = new DropTree();
    @NotNull
    private final List<BreakableScope> breakableScopes = new ArrayList<>();
    @NotNull
    private final List<MirScope> stack = new ArrayList<>();

    @Nullable
    public IfThenScope getIfThenScope() {
        return ifThenScope;
    }

    public void setIfThenScope(@Nullable IfThenScope ifThenScope) {
        this.ifThenScope = ifThenScope;
    }

    @NotNull
    public DropTree getUnwindDrops() {
        return unwindDrops;
    }

    public void push(@NotNull MirScope scope) {
        stack.add(scope);
    }

    public void pop() {
        stack.remove(stack.size() - 1);
    }

    @NotNull
    public Scope topmost() {
        return stack.get(stack.size() - 1).getRegionScope();
    }

    @NotNull
    public MirScope last() {
        return stack.get(stack.size() - 1);
    }

    public int scopeIndex(@NotNull Scope scope) {
        for (int i = stack.size() - 1; i >= 0; i--) {
            if (stack.get(i).getRegionScope().equals(scope)) {
                return i;
            }
        }
        return -1;
    }

    @NotNull
    public List<MirScope> scopes(boolean reversed) {
        if (reversed) {
            List<MirScope> result = new ArrayList<>(stack);
            Collections.reverse(result);
            return result;
        }
        return Collections.unmodifiableList(stack);
    }

    @NotNull
    public List<MirScope> scopes() {
        return scopes(false);
    }

    public void pushBreakable(@NotNull BreakableScope scope) {
        breakableScopes.add(scope);
    }

    public void popBreakable() {
        breakableScopes.remove(breakableScopes.size() - 1);
    }

    @NotNull
    public List<BreakableScope> reversedBreakableScopes() {
        List<BreakableScope> result = new ArrayList<>(breakableScopes);
        Collections.reverse(result);
        return result;
    }
}
