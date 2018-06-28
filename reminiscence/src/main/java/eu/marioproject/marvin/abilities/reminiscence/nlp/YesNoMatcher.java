package eu.marioproject.marvin.abilities.reminiscence.nlp;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import eu.marioproject.marvin.abilities.reminiscence.nlp.MatchResult.MatchStatus;

public class YesNoMatcher extends EntityMatcher {

	public YesNoMatcher() {
		super(AnswerType.YES_NO);
	}

	@Override
	public MatchResult match(String utterance, List<Object> targets, String lang) {
		
		Parser parser = Parser.getParserForType(type);
		List<ParseResult> parseResults = parser.parse(utterance, lang);
		
		if (parseResults.isEmpty()) {
			return new MatchResult(MatchStatus.NO_MATCH);
		}
		
		Object target = CollectionUtils.extractSingleton(targets);
		
		for (ParseResult result : parseResults) {
			if (!result.getResultObject().equals(target)) {
				return new MatchResult(MatchStatus.NO_MATCH);
			}
		}
		MatchResult result = new MatchResult(MatchStatus.MATCH);
		result.setMatchingElements(parseResults);
		return result;
	}
	
	public static void main(String[] args) {
		String utterance = "si, anzi no";
		AnswerType type = AnswerType.YES_NO;
		EntityMatcher m = EntityMatcher.getMatcherForType(type);
		System.err.println(m.match(utterance.trim().toLowerCase(), Collections.singletonList("YES"), "it").getStatus());
	}

}

