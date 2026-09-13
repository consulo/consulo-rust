/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
        @Nonnull
        private final K myKey;

        Root(@Nonnull K key) {
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

    @Nonnull
    private Root<K, V> get(@Nonnull Node key) {
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

    private void setValue(@Nonnull Root<K, V> root, @Nonnull V value) {
        logNodeState(root.myKey);
        root.myKey.setParent(new VarValue<>(value, root.getRank()));
    }

    @Nonnull
    private K unify(@Nonnull Root<K, V> rootA, @Nonnull Root<K, V> rootB, @Nullable V newValue) {
        if (rootA.getRank() > rootB.getRank()) {
            return redirectRoot(rootA.getRank(), rootB, rootA, newValue);
        } else if (rootA.getRank() < rootB.getRank()) {
            return redirectRoot(rootB.getRank(), rootA, rootB, newValue);
        } else {
            return redirectRoot(rootA.getRank() + 1, rootA, rootB, newValue);
        }
    }

    @Nonnull
    private K redirectRoot(int newRank, @Nonnull Root<K, V> oldRoot, @Nonnull Root<K, V> newRoot, @Nullable V newValue) {
        K oldRootKey = oldRoot.myKey;
        K newRootKey = newRoot.myKey;
        logNodeState(newRootKey);
        logNodeState(oldRootKey);
        oldRootKey.setParent(newRootKey);
        newRootKey.setParent(new VarValue<>(newValue, newRank));
        return newRootKey;
    }

    @Nonnull
    public K findRoot(@Nonnull K key) {
        return get(key).myKey;
    }

    @Nullable
    public V findValue(@Nonnull K key) {
        return get(key).getValue();
    }

    @Nonnull
    public K unifyVarVar(@Nonnull K key1, @Nonnull K key2) {
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

    public void unifyVarValue(@Nonnull K key, @Nonnull V value) {
        Root<K, V> node = get(key);
        if (node.getValue() != null && !node.getValue().equals(value)) {
            throw new IllegalStateException("unification error");
        }
        setValue(node, value);
    }

    private void logNodeState(@Nonnull Node node) {
        myUndoLog.logChange(new SetParent(node, node.getParent()));
    }

    @Nonnull
    public Snapshot startSnapshot() {
        return myUndoLog.startSnapshot();
    }

    private static class SetParent implements Undoable {
        @Nonnull
        private final Node myNode;
        @Nonnull
        private final NodeOrValue myOldParent;

        SetParent(@Nonnull Node node, @Nonnull NodeOrValue oldParent) {
            myNode = node;
            myOldParent = oldParent;
        }

        @Override
        public void undo() {
            myNode.setParent(myOldParent);
        }
    }
}
