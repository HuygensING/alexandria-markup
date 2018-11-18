package nl.knaw.huc.di.tag.model.graph.experimental.importer;

import nl.knaw.huc.di.tag.model.graph.experimental.TextHypergraph;

public class HypergraphTAGMLListener extends nl.knaw.huc.di.tag.tagml.grammar.TAGMLParserBaseListener {

    private final TextHypergraph textgraph;

    public HypergraphTAGMLListener() {
        this.textgraph = new TextHypergraph();
    }


}
