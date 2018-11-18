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

    private Map<N, List<N>[]> adjancency;

    Hypergraph() {
        this.adjancency = new LinkedHashMap<>();
    }

    public void addEdge(N source, N target, boolean directed) {
        if (directed) {
            adjancency.computeIfAbsent(source, e -> new ArrayList[3])[OUTGOING].add(target);
            adjancency.computeIfAbsent(target, e -> new ArrayList[3])[INCOMING].add(source);
        } else {
            throw new UnsupportedOperationException("not yet implemented");
        }
    }

    public List<N> getOutgoingNodes(N source) {
        return adjancency.get(source)[OUTGOING];
    }

    public List<N> getIncomingNodes(N target) {
        return adjancency.get(target)[INCOMING];
    }

    public void addHyperedge(H edge, N... targets) {
        for (N target : targets) {
            adjancency.computeIfAbsent(edge, e -> new ArrayList[3])[UNDIRECTED].add(target);
            adjancency.computeIfAbsent(target, e -> new ArrayList[3])[UNDIRECTED].add(edge);
        }
    }

    public List<N> getTargets(H edge) {
        return adjancency.get(edge)[UNDIRECTED];
    }

    @Override
    public String toString() {
        String bla = "";
        for (N key : adjancency.keySet()) {
            bla += key.toString()+" -> "+adjancency.get(key).toString();
        }
        return bla;
    }
}
