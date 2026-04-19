/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

public class PresentableGraph<N extends PresentableNodeData, E> extends Graph<N, E> {

    /**
     * Creates graph description in the DOT language format.
     * The graph can be rendered right inside the IDE using the DOT Language plugin.
     * Also, the graph can be rendered from the file using the CLI: {@code dot -Tpng cfg.dot -o cfg.png}
     */
    @NotNull
    public String createDotDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph {\n");

        forEachEdge(edge -> {
            Node<N, E> source = edge.getSource();
            Node<N, E> target = edge.getTarget();
            N sourceNode = source.getData();
            N targetNode = target.getData();
            String escapedSourceText = sourceNode.getText().replace("\"", "\\\"");
            String escapedTargetText = targetNode.getText().replace("\"", "\\\"");
            sb.append("    \"").append(source.getIndex()).append(": ").append(escapedSourceText)
                .append("\" -> \"").append(target.getIndex()).append(": ").append(escapedTargetText)
                .append("\";\n");
        });

        sb.append("}\n");
        return sb.toString();
    }

    @NotNull
    public String depthFirstTraversalTrace() {
        return depthFirstTraversalTrace(getNode(0));
    }

    @NotNull
    public String depthFirstTraversalTrace(@NotNull Node<N, E> startNode) {
        StringJoiner joiner = new StringJoiner("\n");
        for (Node<N, E> node : depthFirstTraversal(startNode)) {
            joiner.add(node.getData().getText());
        }
        return joiner.toString();
    }
}
