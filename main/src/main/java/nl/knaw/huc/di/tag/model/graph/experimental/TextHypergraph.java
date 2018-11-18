package nl.knaw.huc.di.tag.model.graph.experimental;

import nl.knaw.huc.di.tag.model.graph.experimental.nodes.MarkupNode;
import nl.knaw.huc.di.tag.model.graph.experimental.nodes.TextGraphNode;
import nl.knaw.huc.di.tag.model.graph.experimental.nodes.TextNode;

/*
 * Text Hypergraph
 *
 * @author: Ronald Haentjens Dekker
 * @date:
 *
 * To make the text hypergraph work fast we need an index of the markup nodes
 * and we need to keep a topological sort of the nodes at all times...
 * start and end node are not strictly necessary since there divergence nodes
 *
 */
public class TextHypergraph {
    private Hypergraph<MarkupNode, TextGraphNode> underlyingGraph;
    private TextNode firstTextNode;

    public TextHypergraph() {
        this.underlyingGraph = new Hypergraph<>();
        this.firstTextNode = null;
    }

    public void addText(String text) {
        TextNode textNode = new TextNode(text);
//        underlyingGraph.addNode(new TextNode());
        if (firstTextNode==null) {
            firstTextNode = textNode;
        } else {
            throw new UnsupportedOperationException("Not implemented yet!");
        }
    }

    public void addTextAfter(String text, TextNode previous) {
        TextNode textNode = new TextNode(text);
        underlyingGraph.addEdge(previous, textNode, true);
    }

    public void addMarkupNode(String tag, TextNode start, TextNode end) {

    }
}
