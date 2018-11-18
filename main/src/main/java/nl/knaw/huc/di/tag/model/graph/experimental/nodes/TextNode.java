package nl.knaw.huc.di.tag.model.graph.experimental.nodes;

/*
 * Text node
 *
 * @author: Ronald Haentjens Dekker
 * @date: 18-11-2018
 *
 */
public class TextNode implements TextGraphNode {
    private String text;

    public TextNode(String text) {
        this.text = text;
    }
}
