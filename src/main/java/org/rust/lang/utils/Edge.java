/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Edge<N, E> {
    @NotNull
    private final Node<N, E> mySource;
    @NotNull
    private final Node<N, E> myTarget;
    @NotNull
    private final E myData;
    private final int myIndex;
    @Nullable
    private final Edge<N, E> myNextSourceEdge;
    @Nullable
    private final Edge<N, E> myNextTargetEdge;

    public Edge(
        @NotNull Node<N, E> source,
        @NotNull Node<N, E> target,
        @NotNull E data,
        int index,
        @Nullable Edge<N, E> nextSourceEdge,
        @Nullable Edge<N, E> nextTargetEdge
    ) {
        mySource = source;
        myTarget = target;
        myData = data;
        myIndex = index;
        myNextSourceEdge = nextSourceEdge;
        myNextTargetEdge = nextTargetEdge;
    }

    @NotNull
    public Node<N, E> getSource() {
        return mySource;
    }

    @NotNull
    public Node<N, E> getTarget() {
        return myTarget;
    }

    @NotNull
    public E getData() {
        return myData;
    }

    public int getIndex() {
        return myIndex;
    }

    @Nullable
    public Edge<N, E> getNextSourceEdge() {
        return myNextSourceEdge;
    }

    @Nullable
    public Edge<N, E> getNextTargetEdge() {
        return myNextTargetEdge;
    }

    @NotNull
    public Node<N, E> incidentNode(@NotNull Direction direction) {
        switch (direction) {
            case OUTGOING:
                return myTarget;
            case INCOMING:
                return mySource;
            default:
                throw new IllegalArgumentException("Unknown direction: " + direction);
        }
    }
}
