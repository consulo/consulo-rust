/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.move;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.WithIndex;
import org.rust.lang.core.mir.schemas.MirPlace;
import org.rust.stdext.StdextUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * MovePath is a canonicalized representation of a path that is moved or assigned to.
 *
 * It follows a tree structure.
 *
 * Given {@code struct X { m: M, n: N }} and {@code x: X}, moves like {@code move x.m;} move *out* of the place {@code x.m}.
 *
 * The MovePaths representing {@code x.m} and {@code x.n} are siblings (that is, one of them will link to
 * the other via the next_sibling field, and the other will have no entry in its next_sibling
 * field), and they both have the MovePath representing {@code x} as their parent.
 */
public class MovePath implements WithIndex {
    private final int index;
    @NotNull
    private final MirPlace place;
    @Nullable
    private final MovePath parent;
    @Nullable
    private MovePath nextSibling;
    @Nullable
    private MovePath firstChild;

    public MovePath(int index, @NotNull MirPlace place, @Nullable MovePath parent) {
        this.index = index;
        this.place = place;
        this.parent = parent;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @NotNull
    public MirPlace getPlace() {
        return place;
    }

    @Nullable
    public MovePath getParent() {
        return parent;
    }

    @Nullable
    public MovePath getNextSibling() {
        return nextSibling;
    }

    public void setNextSibling(@Nullable MovePath nextSibling) {
        this.nextSibling = nextSibling;
    }

    @Nullable
    public MovePath getFirstChild() {
        return firstChild;
    }

    public void setFirstChild(@Nullable MovePath firstChild) {
        this.firstChild = firstChild;
    }

    @NotNull
    public List<MovePath> getAncestors() {
        List<MovePath> result = new ArrayList<>();
        MovePath current = this;
        while (current != null) {
            result.add(current);
            current = current.parent;
        }
        return result;
    }

    @Nullable
    public MovePath findInMovePathOrItsDescendants(@NotNull Predicate<MovePath> predicate) {
        if (predicate.test(this)) return this;
        return findDescendant(predicate);
    }

    @Nullable
    private MovePath findDescendant(@NotNull Predicate<MovePath> predicate) {
        if (firstChild == null) return null;
        ArrayDeque<MovePath> queue = new ArrayDeque<>();
        queue.push(firstChild);
        while (!queue.isEmpty()) {
            MovePath element = queue.pop();
            if (predicate.test(element)) return element;
            if (element.firstChild != null) queue.push(element.firstChild);
            if (element.nextSibling != null) queue.push(element.nextSibling);
        }
        return null;
    }
}
