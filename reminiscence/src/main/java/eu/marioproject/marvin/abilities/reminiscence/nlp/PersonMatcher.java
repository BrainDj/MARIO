package eu.marioproject.marvin.abilities.reminiscence.nlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ontologydesignpatterns.ont.mario.person_owl.Person;
import org.ontologydesignpatterns.ont.mario.person_owl.bean.PersonBean;

import eu.marioproject.marvin.abilities.reminiscence.nlp.MatchResult.MatchStatus;

public class PersonMatcher extends EntityMatcher {

	public PersonMatcher() {
		super(AnswerType.PERSON);
	}

	@Override
	public MatchResult match(String utterance, List<Object> targets, String lang) {
		List<ParseResult> parseResults = Parser.getParserForType(type).find(utterance, targets, lang);
		
		MatchResult matchResult = new MatchResult(MatchStatus.NO_MATCH);
		
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
	
	public static void main(String[] args) {
		String utterance = "si certo, è luigi";
		AnswerType type = AnswerType.PERSON;
		EntityMatcher m = EntityMatcher.getMatcherForType(type);
		PersonBean person = new PersonBean();
		person.setPerson_firstName(Collections.singleton("Luigi"));
		MatchResult res = m.match(utterance.trim().toLowerCase(), new ArrayList<>(Person.getAll()), "it");
		System.err.println(res.getStatus());
		System.err.println("MATCHING");
		for (Object match : res.getMatchingEntities()) {
			System.err.println(((Person)match).getPerson_firstName());
		}
		System.err.println("MISSING");
		for (Object miss : res.getMissingEntities()) {
			System.err.println(((Person)miss).getPerson_firstName());
		}
	}

}
