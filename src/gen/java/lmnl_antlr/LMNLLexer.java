// Generated from C:/Users/bramb/workspaces/alexandria/Institute-Materials-2017/alexandria/lmnl-grammar/src/main/java/lmnl_antlr\LMNLLexer.g4 by ANTLR 4.6
package lmnl_antlr;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class LMNLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.6", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		BEGIN_OPEN_RANGE=1, BEGIN_CLOSE_RANGE=2, TEXT=3, END_ANONYMOUS_RANGE=4, 
		END_OPEN_RANGE=5, BEGIN_OPEN_ANNO=6, Name_Open_Range=7, RANGE_S=8, END_CLOSE_RANGE=9, 
		BEGIN_OPEN_ANNO_IN_RANGE_CLOSER=10, Name_Close_Range=11, RANGE_S2=12, 
		END_ANONYMOUS_ANNO=13, END_OPEN_ANNO=14, OPEN_ANNO_IN_ANNO=15, Name_Open_Annotation=16, 
		ANNO_S=17, END_CLOSE_ANNO=18, Name_Close_Annotation=19, BEGIN_CLOSE_ANNO=20, 
		ANNO_TEXT=21;
	public static final int INSIDE_RANGE_OPENER = 1;
	public static final int INSIDE_RANGE_CLOSER = 2;
	public static final int INSIDE_ANNOTATION_OPENER = 3;
	public static final int INSIDE_ANNOTATION_CLOSER = 4;
	public static final int INSIDE_ANNOTATION_TEXT = 5;
	public static String[] modeNames = {
		"DEFAULT_MODE", "INSIDE_RANGE_OPENER", "INSIDE_RANGE_CLOSER", "INSIDE_ANNOTATION_OPENER", 
		"INSIDE_ANNOTATION_CLOSER", "INSIDE_ANNOTATION_TEXT"
	};

	public static final String[] ruleNames = {
		"BEGIN_OPEN_RANGE", "BEGIN_CLOSE_RANGE", "TEXT", "END_ANONYMOUS_RANGE", 
		"END_OPEN_RANGE", "BEGIN_OPEN_ANNO", "Name_Open_Range", "RANGE_S", "END_CLOSE_RANGE", 
		"BEGIN_OPEN_ANNO_IN_RANGE_CLOSER", "Name_Close_Range", "RANGE_S2", "END_ANONYMOUS_ANNO", 
		"END_OPEN_ANNO", "OPEN_ANNO_IN_ANNO", "Name_Open_Annotation", "ANNO_S", 
		"END_CLOSE_ANNO", "Name_Close_Annotation", "BEGIN_CLOSE_ANNO", "ANNO_TEXT", 
		"DIGIT", "NameChar", "NameStartChar"
	};

	private static final String[] _LITERAL_NAMES = {
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "BEGIN_OPEN_RANGE", "BEGIN_CLOSE_RANGE", "TEXT", "END_ANONYMOUS_RANGE", 
		"END_OPEN_RANGE", "BEGIN_OPEN_ANNO", "Name_Open_Range", "RANGE_S", "END_CLOSE_RANGE", 
		"BEGIN_OPEN_ANNO_IN_RANGE_CLOSER", "Name_Close_Range", "RANGE_S2", "END_ANONYMOUS_ANNO", 
		"END_OPEN_ANNO", "OPEN_ANNO_IN_ANNO", "Name_Open_Annotation", "ANNO_S", 
		"END_CLOSE_ANNO", "Name_Close_Annotation", "BEGIN_CLOSE_ANNO", "ANNO_TEXT"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public LMNLLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "LMNLLexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\27\u00b7\b\1\b\1"+
		"\b\1\b\1\b\1\b\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t"+
		"\b\4\t\t\t\4\n\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20"+
		"\t\20\4\21\t\21\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27"+
		"\t\27\4\30\t\30\4\31\t\31\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\4\6\4B\n\4"+
		"\r\4\16\4C\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\b\3\b\7\b"+
		"T\n\b\f\b\16\bW\13\b\3\b\3\b\6\b[\n\b\r\b\16\b\\\5\b_\n\b\3\t\3\t\3\t"+
		"\3\t\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\f\3\f\7\fo\n\f\f\f\16\fr\13"+
		"\f\3\f\3\f\6\fv\n\f\r\f\16\fw\5\fz\n\f\3\r\3\r\3\r\3\r\3\16\3\16\3\16"+
		"\3\16\3\17\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\20\3\21\3\21\7\21\u008f"+
		"\n\21\f\21\16\21\u0092\13\21\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3"+
		"\24\3\24\7\24\u009e\n\24\f\24\16\24\u00a1\13\24\3\25\3\25\3\25\3\25\3"+
		"\25\3\26\6\26\u00a9\n\26\r\26\16\26\u00aa\3\27\3\27\3\30\3\30\3\30\3\30"+
		"\5\30\u00b3\n\30\3\31\5\31\u00b6\n\31\2\2\32\b\3\n\4\f\5\16\6\20\7\22"+
		"\b\24\t\26\n\30\13\32\f\34\r\36\16 \17\"\20$\21&\22(\23*\24,\25.\26\60"+
		"\27\62\2\64\2\66\2\b\2\3\4\5\6\7\b\4\2]^}}\5\2\13\f\17\17\"\"\3\2\62;"+
		"\4\2/\60aa\5\2\u00b9\u00b9\u0302\u0371\u2041\u2042\n\2<<C\\c|\u2072\u2191"+
		"\u2c02\u2ff1\u3003\ud801\uf902\ufdd1\ufdf2\uffff\u00bb\2\b\3\2\2\2\2\n"+
		"\3\2\2\2\2\f\3\2\2\2\3\16\3\2\2\2\3\20\3\2\2\2\3\22\3\2\2\2\3\24\3\2\2"+
		"\2\3\26\3\2\2\2\4\30\3\2\2\2\4\32\3\2\2\2\4\34\3\2\2\2\4\36\3\2\2\2\5"+
		" \3\2\2\2\5\"\3\2\2\2\5$\3\2\2\2\5&\3\2\2\2\5(\3\2\2\2\6*\3\2\2\2\6,\3"+
		"\2\2\2\7.\3\2\2\2\7\60\3\2\2\2\b8\3\2\2\2\n<\3\2\2\2\fA\3\2\2\2\16E\3"+
		"\2\2\2\20I\3\2\2\2\22M\3\2\2\2\24Q\3\2\2\2\26`\3\2\2\2\30d\3\2\2\2\32"+
		"h\3\2\2\2\34l\3\2\2\2\36{\3\2\2\2 \177\3\2\2\2\"\u0083\3\2\2\2$\u0088"+
		"\3\2\2\2&\u008c\3\2\2\2(\u0093\3\2\2\2*\u0097\3\2\2\2,\u009b\3\2\2\2."+
		"\u00a2\3\2\2\2\60\u00a8\3\2\2\2\62\u00ac\3\2\2\2\64\u00b2\3\2\2\2\66\u00b5"+
		"\3\2\2\289\7]\2\29:\3\2\2\2:;\b\2\2\2;\t\3\2\2\2<=\7}\2\2=>\3\2\2\2>?"+
		"\b\3\3\2?\13\3\2\2\2@B\n\2\2\2A@\3\2\2\2BC\3\2\2\2CA\3\2\2\2CD\3\2\2\2"+
		"D\r\3\2\2\2EF\7_\2\2FG\3\2\2\2GH\b\5\4\2H\17\3\2\2\2IJ\7\177\2\2JK\3\2"+
		"\2\2KL\b\6\4\2L\21\3\2\2\2MN\7]\2\2NO\3\2\2\2OP\b\7\5\2P\23\3\2\2\2QU"+
		"\5\66\31\2RT\5\64\30\2SR\3\2\2\2TW\3\2\2\2US\3\2\2\2UV\3\2\2\2V^\3\2\2"+
		"\2WU\3\2\2\2XZ\7?\2\2Y[\5\64\30\2ZY\3\2\2\2[\\\3\2\2\2\\Z\3\2\2\2\\]\3"+
		"\2\2\2]_\3\2\2\2^X\3\2\2\2^_\3\2\2\2_\25\3\2\2\2`a\t\3\2\2ab\3\2\2\2b"+
		"c\b\t\6\2c\27\3\2\2\2de\7_\2\2ef\3\2\2\2fg\b\n\4\2g\31\3\2\2\2hi\7]\2"+
		"\2ij\3\2\2\2jk\b\13\5\2k\33\3\2\2\2lp\5\66\31\2mo\5\64\30\2nm\3\2\2\2"+
		"or\3\2\2\2pn\3\2\2\2pq\3\2\2\2qy\3\2\2\2rp\3\2\2\2su\7?\2\2tv\5\64\30"+
		"\2ut\3\2\2\2vw\3\2\2\2wu\3\2\2\2wx\3\2\2\2xz\3\2\2\2ys\3\2\2\2yz\3\2\2"+
		"\2z\35\3\2\2\2{|\t\3\2\2|}\3\2\2\2}~\b\r\6\2~\37\3\2\2\2\177\u0080\7_"+
		"\2\2\u0080\u0081\3\2\2\2\u0081\u0082\b\16\4\2\u0082!\3\2\2\2\u0083\u0084"+
		"\7\177\2\2\u0084\u0085\3\2\2\2\u0085\u0086\b\17\4\2\u0086\u0087\b\17\7"+
		"\2\u0087#\3\2\2\2\u0088\u0089\7]\2\2\u0089\u008a\3\2\2\2\u008a\u008b\b"+
		"\20\5\2\u008b%\3\2\2\2\u008c\u0090\5\66\31\2\u008d\u008f\5\64\30\2\u008e"+
		"\u008d\3\2\2\2\u008f\u0092\3\2\2\2\u0090\u008e\3\2\2\2\u0090\u0091\3\2"+
		"\2\2\u0091\'\3\2\2\2\u0092\u0090\3\2\2\2\u0093\u0094\t\3\2\2\u0094\u0095"+
		"\3\2\2\2\u0095\u0096\b\22\6\2\u0096)\3\2\2\2\u0097\u0098\7_\2\2\u0098"+
		"\u0099\3\2\2\2\u0099\u009a\b\23\4\2\u009a+\3\2\2\2\u009b\u009f\5\66\31"+
		"\2\u009c\u009e\5\64\30\2\u009d\u009c\3\2\2\2\u009e\u00a1\3\2\2\2\u009f"+
		"\u009d\3\2\2\2\u009f\u00a0\3\2\2\2\u00a0-\3\2\2\2\u00a1\u009f\3\2\2\2"+
		"\u00a2\u00a3\7}\2\2\u00a3\u00a4\3\2\2\2\u00a4\u00a5\b\25\4\2\u00a5\u00a6"+
		"\b\25\b\2\u00a6/\3\2\2\2\u00a7\u00a9\n\2\2\2\u00a8\u00a7\3\2\2\2\u00a9"+
		"\u00aa\3\2\2\2\u00aa\u00a8\3\2\2\2\u00aa\u00ab\3\2\2\2\u00ab\61\3\2\2"+
		"\2\u00ac\u00ad\t\4\2\2\u00ad\63\3\2\2\2\u00ae\u00b3\5\66\31\2\u00af\u00b3"+
		"\t\5\2\2\u00b0\u00b3\5\62\27\2\u00b1\u00b3\t\6\2\2\u00b2\u00ae\3\2\2\2"+
		"\u00b2\u00af\3\2\2\2\u00b2\u00b0\3\2\2\2\u00b2\u00b1\3\2\2\2\u00b3\65"+
		"\3\2\2\2\u00b4\u00b6\t\7\2\2\u00b5\u00b4\3\2\2\2\u00b6\67\3\2\2\2\24\2"+
		"\3\4\5\6\7CU\\^pwy\u0090\u009f\u00aa\u00b2\u00b5\t\7\3\2\7\4\2\6\2\2\7"+
		"\5\2\b\2\2\7\7\2\7\6\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}