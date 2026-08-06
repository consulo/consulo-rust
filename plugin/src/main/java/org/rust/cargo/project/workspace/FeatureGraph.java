/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import org.rust.lang.utils.Edge;
import org.rust.lang.utils.Node;
import org.rust.lang.utils.PresentableGraph;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class FeatureGraph {

    private final PresentableGraph<PackageFeature, Void> myGraph;
    private final Map<PackageFeature, Node<PackageFeature, Void>> myFeatureToNode;

    private FeatureGraph(
        PresentableGraph<PackageFeature, Void> graph,
        Map<PackageFeature, Node<PackageFeature, Void>> featureToNode
    ) {
        myGraph = graph;
        myFeatureToNode = featureToNode;
    }

    /**
     * Applies the specified function {@code f} to a freshly created {@link FeaturesView} and returns its state
     */
    public Map<PackageFeature, FeatureState> apply(FeatureState defaultState, Consumer<FeaturesView> f) {
        FeaturesView view = new FeaturesView(defaultState);
        f.accept(view);
        return view.getState();
    }

    /** Mutable view of a {@link FeatureGraph} */
    public final class FeaturesView {
        private final Map<PackageFeature, FeatureState> myState;

        public FeaturesView(FeatureState defaultState) {
            myState = new HashMap<>();
            for (PackageFeature feature : myFeatureToNode.keySet()) {
                myState.put(feature, defaultState);
            }
        }

        public Map<PackageFeature, FeatureState> getState() {
            return myState;
        }

        public void enableAll(Iterable<PackageFeature> features) {
            for (PackageFeature feature : features) {
                enable(feature);
            }
        }

        public void disableAll(Iterable<PackageFeature> features) {
            for (PackageFeature feature : features) {
                disable(feature);
            }
        }

        public void enable(PackageFeature feature) {
            Node<PackageFeature, Void> node = myFeatureToNode.get(feature);
            if (node == null) return;
            enableFeatureTransitively(node);
        }

        public void disable(PackageFeature feature) {
            Node<PackageFeature, Void> node = myFeatureToNode.get(feature);
            if (node == null) return;
            disableFeatureTransitively(node);
        }

        private void enableFeatureTransitively(Node<PackageFeature, Void> node) {
            if (myState.get(node.getData()) == FeatureState.Enabled) return;

            myState.put(node.getData(), FeatureState.Enabled);

            for (Edge<PackageFeature, Void> edge : myGraph.incomingEdges(node)) {
                Node<PackageFeature, Void> dependency = edge.getSource();
                enableFeatureTransitively(dependency);
            }
        }

        private void disableFeatureTransitively(Node<PackageFeature, Void> node) {
            if (myState.get(node.getData()) == FeatureState.Disabled) return;

            myState.put(node.getData(), FeatureState.Disabled);

            for (Edge<PackageFeature, Void> edge : myGraph.outgoingEdges(node)) {
                Node<PackageFeature, Void> dependant = edge.getTarget();
                disableFeatureTransitively(dependant);
            }
        }
    }

    public static FeatureGraph buildFor(Map<PackageFeature, List<PackageFeature>> features) {
        PresentableGraph<PackageFeature, Void> graph = new PresentableGraph<>();
        HashMap<PackageFeature, Node<PackageFeature, Void>> featureToNode = new HashMap<>();

        // Add nodes
        for (PackageFeature feature : features.keySet()) {
            if (!featureToNode.containsKey(feature)) {
                Node<PackageFeature, Void> newNode = graph.addNode(feature);
                featureToNode.put(feature, newNode);
            }
        }

        // Add edges
        for (Map.Entry<PackageFeature, List<PackageFeature>> entry : features.entrySet()) {
            PackageFeature feature = entry.getKey();
            List<PackageFeature> dependencies = entry.getValue();
            Node<PackageFeature, Void> targetNode = featureToNode.get(feature);
            if (targetNode == null) {
                if (OpenApiUtil.isUnitTestMode()) {
                    throw new IllegalStateException("Unknown feature " + feature);
                }
                continue;
            }
            for (PackageFeature dependency : dependencies) {
                Node<PackageFeature, Void> sourceNode = featureToNode.get(dependency);
                if (sourceNode == null) {
                    if (OpenApiUtil.isUnitTestMode()) {
                        throw new IllegalStateException("Unknown feature " + dependency + " (dependency of " + feature + ")");
                    }
                    continue;
                }
                graph.addEdge(sourceNode, targetNode, null);
            }
        }

        return new FeatureGraph(graph, featureToNode);
    }

    public static FeatureGraph getEmpty() {
        return new FeatureGraph(new PresentableGraph<>(), java.util.Collections.emptyMap());
    }

    /**
     * Converts a map of PackageFeature->FeatureState to a map of PackageRoot->(FeatureName->FeatureState).
     */
    public static Map<Path, Map<String, FeatureState>> associateByPackageRoot(Map<PackageFeature, FeatureState> featureStateMap) {
        Map<Path, Map<String, FeatureState>> map = new HashMap<>();
        for (Map.Entry<PackageFeature, FeatureState> entry : featureStateMap.entrySet()) {
            PackageFeature feature = entry.getKey();
            FeatureState state = entry.getValue();
            map.computeIfAbsent(feature.getPkg().getRootDirectory(), k -> new HashMap<>())
                .put(feature.getName(), state);
        }
        return map;
    }
}
