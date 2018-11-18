package nl.knaw.huc.di.tag.model.graph.experimental;

import nl.knaw.huc.di.tag.model.graph.experimental.nodes.MarkupNode;
import nl.knaw.huc.di.tag.model.graph.experimental.nodes.TextNode;

/*
 * Simple Text Hypergraph example
 */
public class TextGraphExample {

    public static void main(String args[]) {
        TextHypergraph textHypergraph = new TextHypergraph();
        MarkupNode root = textHypergraph.addMarkupNode("TAGML");
        MarkupNode chapter1 = textHypergraph.addMarkupNode("chapter");
        TextNode text1 = textHypergraph.addText("text of chapter 1./n");
        MarkupNode chapter2 = textHypergraph.addMarkupNode("chapter");
        TextNode text2 = textHypergraph.addText("text of chapter 2./n");

        textHypergraph.addHyperedge(root, text1, text2);
        textHypergraph.addHyperedge(chapter1, text1);
        textHypergraph.addHyperedge(chapter2, text2);

        textHypergraph.showMe();

    }
}
