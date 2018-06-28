package eu.marioproject.marvin.abilities.reminiscence.nlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YesNoParser extends Parser {
	
	private final static Map<String, String> yesRegexprs;
	private final static Map<String, String> noRegexprs;
	
	private final static String yesRegexIt = "\\b(si|sì|affermativo|certo|(?<!non è\\s)(vero)|giusto|corretto|altroché|certamente|esatto)\\b";
	private final static String noRegexIt = "\\b(no|falso|non è vero|sbagliato|niente affatto|per niente)\\b";
	
	private final static String yesRegexEn = "\\b(ye(ah?|p|s)|sure(ly)?|of course|right|true|correct|absolutely(?!\\snot)|exactly)\\b";
	private final static String noRegexEn = "\\b(no|false|absolutely not|not at all|wrong|nope)\\b";
	
	static {
		// yes
		yesRegexprs = new HashMap<>(2);
		yesRegexprs.put("en", yesRegexEn);
		yesRegexprs.put("it", yesRegexIt);
		// no
		noRegexprs = new HashMap<>(2);
		noRegexprs.put("en", noRegexEn);
		noRegexprs.put("it", noRegexIt);
	}
	
	public YesNoParser() {
		super(AnswerType.YES_NO);
	}

	@Override
	public List<ParseResult> parse(String utterance, String lang) {
		List<ParseResult> parseResults = new ArrayList<>();
		super.matchRegex(utterance, parseResults, yesRegexprs.get(lang), this.type, "YES");
		super.matchRegex(utterance, parseResults, noRegexprs.get(lang), this.type, "NO");
		return parseResults;
	}

	@Override
	public List<ParseResult> find(String utterance, List<Object> targets, String lang) {
		return Collections.emptyList();
	}
	
	public static void main(String[] args) {
		
		YesNoParser parser = new YesNoParser();
		List<ParseResult> res = parser.parse("oh sure, it surely paul!", "en");
		
		for (ParseResult result : res) {
			//System.err.println(result);
		}
	}

}
