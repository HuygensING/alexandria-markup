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
    private TextNode lastTextNode;

    public TextHypergraph() {
        this.underlyingGraph = new Hypergraph<>();
        this.firstTextNode = null;
        this.lastTextNode = null;
    }

    public TextNode addText(String text) {
        TextNode textNode = new TextNode(text);
        underlyingGraph.addNode(textNode);
        if (firstTextNode==null) {
            firstTextNode = textNode;
        } else {
            underlyingGraph.addEdge(lastTextNode, textNode, true);
        }
        lastTextNode = textNode;
        return textNode;
    }

    public MarkupNode addMarkupNode(String tag) {
        MarkupNode node = new MarkupNode(tag);
        underlyingGraph.addNode(node);
        return node;
    }

    public void addHyperedge(MarkupNode edge, TextGraphNode... text) {
        underlyingGraph.addHyperedge(edge, text, true);
    }

    public void showMe() {
        System.out.println(underlyingGraph);
    }
}
