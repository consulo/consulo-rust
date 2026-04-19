/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.utils.snapshot.Snapshot;
import org.rust.lang.utils.snapshot.UndoLog;
import org.rust.lang.utils.snapshot.Undoable;

/**
 * UnificationTable is a map from K to V with additional ability
 * to redirect certain K's to a single V en-masse with the help of
 * disjoint set union.
 *
 * We implement Tarjan's union-find algorithm.
 */
@SuppressWarnings("unchecked")
public class UnificationTable<K extends Node, V> {
    private final UndoLog myUndoLog = new UndoLog();

    private static class Root<K extends Node, V> {
        @NotNull
        private final K myKey;

        Root(@NotNull K key) {
            myKey = key;
        }

        int getRank() {
            return ((VarValue<V>) myKey.getParent()).getRank();
        }

        @Nullable
        V getValue() {
            return ((VarValue<V>) myKey.getParent()).getValue();
        }
    }

    @NotNull
    private Root<K, V> get(@NotNull Node key) {
        NodeOrValue parent = key.getParent();
        if (parent instanceof Node) {
            Root<K, V> root = get((Node) parent);
            if (key.getParent() != root.myKey) {
                logNodeState(key);
                key.setParent(root.myKey); // Path compression
            }
            return root;
        } else {
            return new Root<>((K) key);
        }
    }

    private void setValue(@NotNull Root<K, V> root, @NotNull V value) {
        logNodeState(root.myKey);
        root.myKey.setParent(new VarValue<>(value, root.getRank()));
    }

    @NotNull
    private K unify(@NotNull Root<K, V> rootA, @NotNull Root<K, V> rootB, @Nullable V newValue) {
        if (rootA.getRank() > rootB.getRank()) {
            return redirectRoot(rootA.getRank(), rootB, rootA, newValue);
        } else if (rootA.getRank() < rootB.getRank()) {
            return redirectRoot(rootB.getRank(), rootA, rootB, newValue);
        } else {
            return redirectRoot(rootA.getRank() + 1, rootA, rootB, newValue);
        }
    }

    @NotNull
    private K redirectRoot(int newRank, @NotNull Root<K, V> oldRoot, @NotNull Root<K, V> newRoot, @Nullable V newValue) {
        K oldRootKey = oldRoot.myKey;
        K newRootKey = newRoot.myKey;
        logNodeState(newRootKey);
        logNodeState(oldRootKey);
        oldRootKey.setParent(newRootKey);
        newRootKey.setParent(new VarValue<>(newValue, newRank));
        return newRootKey;
    }

    @NotNull
    public K findRoot(@NotNull K key) {
        return get(key).myKey;
    }

    @Nullable
    public V findValue(@NotNull K key) {
        return get(key).getValue();
    }

    @NotNull
    public K unifyVarVar(@NotNull K key1, @NotNull K key2) {
        Root<K, V> node1 = get(key1);
        Root<K, V> node2 = get(key2);

        if (node1.myKey == node2.myKey) return node1.myKey; // already unified

        V val1 = node1.getValue();
        V val2 = node2.getValue();

        V newVal;
        if (val1 != null && val2 != null) {
            if (!val1.equals(val2)) throw new IllegalStateException("unification error");
            newVal = val1;
        } else {
            newVal = val1 != null ? val1 : val2;
        }

        return unify(node1, node2, newVal);
    }

    public void unifyVarValue(@NotNull K key, @NotNull V value) {
        Root<K, V> node = get(key);
        if (node.getValue() != null && !node.getValue().equals(value)) {
            throw new IllegalStateException("unification error");
        }
        setValue(node, value);
    }

    private void logNodeState(@NotNull Node node) {
        myUndoLog.logChange(new SetParent(node, node.getParent()));
    }

    @NotNull
    public Snapshot startSnapshot() {
        return myUndoLog.startSnapshot();
    }

    private static class SetParent implements Undoable {
        @NotNull
        private final Node myNode;
        @NotNull
        private final NodeOrValue myOldParent;

        SetParent(@NotNull Node node, @NotNull NodeOrValue oldParent) {
            myNode = node;
            myOldParent = oldParent;
        }

        @Override
        public void undo() {
            myNode.setParent(myOldParent);
        }
    }
}
