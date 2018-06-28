package eu.marioproject.marvin.abilities.reminiscence.nlp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Parser {

	protected final AnswerType type;

	private final static Map<AnswerType, Parser> parsers = new HashMap<>();

	static {
		parsers.put(AnswerType.YES_NO, new YesNoParser());
		parsers.put(AnswerType.PERSON, new PersonParser());
		parsers.put(AnswerType.CITY, new CityParser());
	}

	public Parser(AnswerType type){
		this.type = type;
	}

	public AnswerType getType(){
		return this.type;
	}

	protected void matchRegex(String utterance, List<ParseResult> results, String regex, AnswerType answerType, Object answer){

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(utterance);
		ParseResult parseResult;
		
		while (matcher.find()) {
			parseResult = new ParseResult(answerType, matcher.start(), matcher.end(), matcher.group(0), answer);
			System.err.println(parseResult);
			results.add(parseResult);
		}
	}

	public abstract List<ParseResult> parse(String utterance, String lang);
	
	public abstract List<ParseResult> find(String utterance, List<Object> targets, String lang);

	
	public static Parser getParserForType(AnswerType type) {
		return parsers.get(type);
	}

}
