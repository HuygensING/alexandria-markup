package nl.knaw.huc.di.tag.model.graph.experimental.importer;

import nl.knaw.huc.di.tag.tagml.TAGMLBreakingError;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser;
import nl.knaw.huc.di.tag.tagml.importer.TAGMLListener;
import nl.knaw.huygens.alexandria.ErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class TAGMLImporter {

    public void importML(String input) {
        CharStream c = CharStreams.fromString(input);
        nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer lexer = new TAGMLLexer(c);

        ErrorListener errorListener = new ErrorListener();
        lexer.addErrorListener(errorListener);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        TAGMLParser parser = new TAGMLParser(tokens);
        parser.addErrorListener(errorListener);

        parser.setBuildParseTree(true);
        ParseTree parseTree = parser.document();

        //    LOG.debug("parsetree: {}", parseTree.toStringTree(parser));

        HypergraphTAGMLListener listener = new HypergraphTAGMLListener();

        try {
            ParseTreeWalker.DEFAULT.walk(listener, parseTree);
        } catch (TAGMLBreakingError ignored) {

        }
    }
}
