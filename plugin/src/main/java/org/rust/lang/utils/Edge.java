/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class Edge<N, E> {
    @Nonnull
    private final Node<N, E> mySource;
    @Nonnull
    private final Node<N, E> myTarget;
    @Nonnull
    private final E myData;
    private final int myIndex;
    @Nullable
    private final Edge<N, E> myNextSourceEdge;
    @Nullable
    private final Edge<N, E> myNextTargetEdge;

    public Edge(
        @Nonnull Node<N, E> source,
        @Nonnull Node<N, E> target,
        @Nonnull E data,
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

    @Nonnull
    public Node<N, E> getSource() {
        return mySource;
    }

    @Nonnull
    public Node<N, E> getTarget() {
        return myTarget;
    }

    @Nonnull
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

    @Nonnull
    public Node<N, E> incidentNode(@Nonnull Direction direction) {
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
