package nl.knaw.huc.di.tag.model.graph.experimental;


import java.util.*;

/*
 * Generic hypergraph class that allows for undirected hyperedges and directed and undirected edges.
 * Hyperedges and Nodes are generic.
 * Hyperedges are treated as a special kind of node
 * Edges are not stored
 *
 * Implementation is like an adjacency list
 * The nodes are stored in insertion order (which while parsing is the same as topological order)
 *
 * @author: Ronald Haentjens Dekker
 * @Date: 17-11-2018
 *
 */
public class Hypergraph<H extends N, N> {
    private static int UNDIRECTED = 0;
    private static int OUTGOING = 1;
    private static int INCOMING = 2;

    private Map<N, List<N>[]> adjacency;

    Hypergraph() {
        this.adjacency = new LinkedHashMap<>();
    }

    public void addEdge(N source, N target, boolean directed) {
        if (directed) {
            adjacency.computeIfAbsent(source, e -> new ArrayList[3])[OUTGOING].add(target);
            adjacency.computeIfAbsent(target, e -> new ArrayList[3])[INCOMING].add(source);
        } else {
            throw new UnsupportedOperationException("not yet implemented");
        }
    }

    public List<N> getOutgoingNodes(N source) {
        return adjacency.get(source)[OUTGOING];
    }

    public List<N> getIncomingNodes(N target) {
        return adjacency.get(target)[INCOMING];
    }

    public void addNode(N node) {
        adjacency.computeIfAbsent(node, n -> new ArrayList[3]);
    }

    public void addHyperedge(H edge, N... targets) {
        for (N target : targets) {
            adjacency.computeIfAbsent(edge, e -> new ArrayList[3])[UNDIRECTED].add(target);
            adjacency.computeIfAbsent(target, e -> new ArrayList[3])[UNDIRECTED].add(edge);
        }
    }

    public void addHyperedge(H edge, N[] targets, boolean dummy) {
        for (N target : targets) {
        adjacency.computeIfAbsent(edge, e -> new ArrayList[3])[UNDIRECTED].add(target);
        adjacency.computeIfAbsent(target, e -> new ArrayList[3])[UNDIRECTED].add(edge);
    }
}

    public List<N> getTargets(H edge) {
        return adjacency.get(edge)[UNDIRECTED];
    }

    @Override
    public String toString() {
        String bla = "";
        for (N key : adjacency.keySet()) {
            bla += key.toString()+" -> "+ adjacency.get(key).toString();
        }
        return bla;
    }

}
