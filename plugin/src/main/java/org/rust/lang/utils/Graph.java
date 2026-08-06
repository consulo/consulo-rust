/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.stdext.CollectionsUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Graph<N, E> {
    private final List<Node<N, E>> myNodes;
    private final List<Edge<N, E>> myEdges;

    public Graph() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    public Graph(@Nonnull List<Node<N, E>> nodes, @Nonnull List<Edge<N, E>> edges) {
        myNodes = nodes;
        myEdges = edges;
    }

    private int getNextNodeIndex() {
        return myNodes.size();
    }

    private int getNextEdgeIndex() {
        return myEdges.size();
    }

    public int getNodesCount() {
        return myNodes.size();
    }

    @Nonnull
    public Node<N, E> getNode(int index) {
        return myNodes.get(index);
    }

    @Nonnull
    public Node<N, E> addNode(@Nonnull N data) {
        Node<N, E> newNode = new Node<>(data, getNextNodeIndex());
        myNodes.add(newNode);
        return newNode;
    }

    @Nonnull
    public Edge<N, E> addEdge(@Nonnull Node<N, E> source, @Nonnull Node<N, E> target, @Nonnull E data) {
        Edge<N, E> sourceFirst = source.getFirstOutEdge();
        Edge<N, E> targetFirst = target.getFirstInEdge();

        Edge<N, E> newEdge = new Edge<>(source, target, data, getNextEdgeIndex(), sourceFirst, targetFirst);
        myEdges.add(newEdge);

        source.setFirstOutEdge(newEdge);
        target.setFirstInEdge(newEdge);

        return newEdge;
    }

    @Nonnull
    public Edge<N, E> addEdge(int sourceIndex, int targetIndex, @Nonnull E data) {
        Node<N, E> source = myNodes.get(sourceIndex);
        Node<N, E> target = myNodes.get(targetIndex);
        return addEdge(source, target, data);
    }

    @Nonnull
    public Iterable<Edge<N, E>> outgoingEdges(@Nonnull Node<N, E> source) {
        return () -> new EdgeIterator<>(source.getFirstOutEdge(), true);
    }

    @Nonnull
    public Iterable<Edge<N, E>> incomingEdges(@Nonnull Node<N, E> target) {
        return () -> new EdgeIterator<>(target.getFirstInEdge(), false);
    }

    @Nonnull
    private Iterable<Edge<N, E>> incidentEdges(@Nonnull Node<N, E> node, @Nonnull Direction direction) {
        switch (direction) {
            case OUTGOING:
                return outgoingEdges(node);
            case INCOMING:
                return incomingEdges(node);
            default:
                throw new IllegalArgumentException("Unknown direction: " + direction);
        }
    }

    public void forEachNode(@Nonnull Consumer<Node<N, E>> f) {
        myNodes.forEach(f);
    }

    public void forEachEdge(@Nonnull Consumer<Edge<N, E>> f) {
        myEdges.forEach(f);
    }

    @Nonnull
    public Iterable<Node<N, E>> depthFirstTraversal(@Nonnull Node<N, E> startNode) {
        return depthFirstTraversal(startNode, Direction.OUTGOING, e -> true);
    }

    @Nonnull
    public Iterable<Node<N, E>> depthFirstTraversal(
        @Nonnull Node<N, E> startNode,
        @Nonnull Direction direction
    ) {
        return depthFirstTraversal(startNode, direction, e -> true);
    }

    @Nonnull
    public Iterable<Node<N, E>> depthFirstTraversal(
        @Nonnull Node<N, E> startNode,
        @Nonnull Direction direction,
        @Nonnull Predicate<Edge<N, E>> edgeFilter
    ) {
        return () -> {
            Set<Node<N, E>> visited = new HashSet<>();
            visited.add(startNode);
            ArrayDeque<Node<N, E>> stack = new ArrayDeque<>();
            stack.push(startNode);

            return new Iterator<Node<N, E>>() {
                @Nullable
                private Node<N, E> myNext = advance();

                @Nullable
                private Node<N, E> advance() {
                    Node<N, E> next = stack.poll();
                    if (next != null) {
                        for (Edge<N, E> edge : incidentEdges(next, direction)) {
                            if (edgeFilter.test(edge)) {
                                Node<N, E> incident = edge.incidentNode(direction);
                                if (visited.add(incident)) {
                                    stack.push(incident);
                                }
                            }
                        }
                    }
                    return next;
                }

                @Override
                public boolean hasNext() {
                    return myNext != null;
                }

                @Override
                public Node<N, E> next() {
                    Node<N, E> result = myNext;
                    if (result == null) throw new NoSuchElementException();
                    myNext = advance();
                    return result;
                }
            };
        };
    }

    @Nonnull
    public List<Node<N, E>> nodesInPostOrder(@Nonnull Node<N, E> entryNode) {
        return nodesInPostOrder(entryNode, Direction.OUTGOING);
    }

    @Nonnull
    public List<Node<N, E>> nodesInPostOrder(@Nonnull Node<N, E> entryNode, @Nonnull Direction direction) {
        Set<Node<N, E>> visited = new HashSet<>();
        ArrayDeque<Map.Entry<Node<N, E>, Iterator<Edge<N, E>>>> stack = new ArrayDeque<>();
        List<Node<N, E>> result = new ArrayList<>();

        List<Node<N, E>> nodesWithEntry = new ArrayList<>();
        nodesWithEntry.add(entryNode);
        nodesWithEntry.addAll(myNodes);

        for (Node<N, E> nextNode : nodesWithEntry) {
            if (visited.add(nextNode)) {
                stack.push(new AbstractMap.SimpleEntry<>(nextNode, toIterator(incidentEdges(nextNode, direction))));
            }
            while (!stack.isEmpty()) {
                Map.Entry<Node<N, E>, Iterator<Edge<N, E>>> top = stack.pop();
                Node<N, E> node = top.getKey();
                Iterator<Edge<N, E>> iter = top.getValue();
                Edge<N, E> child = CollectionsUtil.nextOrNull(iter);
                if (child != null) {
                    Node<N, E> incident = child.incidentNode(direction);
                    stack.push(new AbstractMap.SimpleEntry<>(node, iter));
                    if (visited.add(incident)) {
                        stack.push(new AbstractMap.SimpleEntry<>(incident, toIterator(incidentEdges(incident, direction))));
                    }
                } else {
                    result.add(node);
                }
            }
        }

        return result;
    }

    @Nonnull
    private static <T> Iterator<T> toIterator(@Nonnull Iterable<T> iterable) {
        return iterable.iterator();
    }

    private static class EdgeIterator<N, E> implements Iterator<Edge<N, E>> {
        @Nullable
        private Edge<N, E> myCurrent;
        private final boolean myIsOutgoing;

        EdgeIterator(@Nullable Edge<N, E> first, boolean isOutgoing) {
            myCurrent = first;
            myIsOutgoing = isOutgoing;
        }

        @Override
        public boolean hasNext() {
            return myCurrent != null;
        }

        @Override
        public Edge<N, E> next() {
            Edge<N, E> result = myCurrent;
            if (result == null) throw new NoSuchElementException();
            myCurrent = myIsOutgoing ? result.getNextSourceEdge() : result.getNextTargetEdge();
            return result;
        }
    }
}
