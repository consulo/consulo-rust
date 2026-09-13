/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.types.regions.Scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Scopes {
    @Nullable
    private IfThenScope ifThenScope;
    @Nonnull
    private final DropTree unwindDrops = new DropTree();
    @Nonnull
    private final List<BreakableScope> breakableScopes = new ArrayList<>();
    @Nonnull
    private final List<MirScope> stack = new ArrayList<>();

    @Nullable
    public IfThenScope getIfThenScope() {
        return ifThenScope;
    }

    public void setIfThenScope(@Nullable IfThenScope ifThenScope) {
        this.ifThenScope = ifThenScope;
    }

    @Nonnull
    public DropTree getUnwindDrops() {
        return unwindDrops;
    }

    public void push(@Nonnull MirScope scope) {
        stack.add(scope);
    }

    public void pop() {
        stack.remove(stack.size() - 1);
    }

    @Nonnull
    public Scope topmost() {
        return stack.get(stack.size() - 1).getRegionScope();
    }

    @Nonnull
    public MirScope last() {
        return stack.get(stack.size() - 1);
    }

    public int scopeIndex(@Nonnull Scope scope) {
        for (int i = stack.size() - 1; i >= 0; i--) {
            if (stack.get(i).getRegionScope().equals(scope)) {
                return i;
            }
        }
        return -1;
    }

    @Nonnull
    public List<MirScope> scopes(boolean reversed) {
        if (reversed) {
            List<MirScope> result = new ArrayList<>(stack);
            Collections.reverse(result);
            return result;
        }
        return Collections.unmodifiableList(stack);
    }

    @Nonnull
    public List<MirScope> scopes() {
        return scopes(false);
    }

    public void pushBreakable(@Nonnull BreakableScope scope) {
        breakableScopes.add(scope);
    }

    public void popBreakable() {
        breakableScopes.remove(breakableScopes.size() - 1);
    }

    @Nonnull
    public List<BreakableScope> reversedBreakableScopes() {
        List<BreakableScope> result = new ArrayList<>(breakableScopes);
        Collections.reverse(result);
        return result;
    }
}
