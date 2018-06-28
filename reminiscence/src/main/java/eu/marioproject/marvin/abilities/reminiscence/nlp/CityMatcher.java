package eu.marioproject.marvin.abilities.reminiscence.nlp;

import java.util.List;

import eu.marioproject.marvin.abilities.reminiscence.nlp.MatchResult.MatchStatus;

public class CityMatcher extends EntityMatcher {

	public CityMatcher() {
		super(AnswerType.CITY);
	}

	@Override
	public MatchResult match(String utterance, List<Object> targets, String lang) {
		List<ParseResult> parseResults = Parser.getParserForType(type).find(utterance, targets, lang);
		
		MatchResult matchResult = new MatchResult(MatchStatus.NO_MATCH);
		
		// no match, all target entities are missing
		if (parseResults.isEmpty()) {
			matchResult.setMissingEntities(targets);
			return matchResult;
		}
		
		
		boolean oneFound = false;
		Object matchedEntity;
		for (ParseResult result : parseResults) {
			matchedEntity = result.getResultObject();
			if (targets.contains(matchedEntity)) {
				oneFound = true;
				matchResult.addMatchingElement(result);
				matchResult.addMatching(matchedEntity);
			}
		}
		List<Object> missingEntities = targets;
		missingEntities.removeAll(matchResult.getMatchingEntities());
		matchResult.setMissingEntities(missingEntities);
		
		if (missingEntities.isEmpty()) {
			matchResult.setStatus(MatchStatus.MATCH);
		}
		else {
			if (oneFound) {
				matchResult.setStatus(MatchStatus.PARTIAL_MATCH);
			}
		}
		
		return matchResult;
	}

}
