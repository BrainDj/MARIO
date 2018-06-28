package eu.marioproject.marvin.abilities.reminiscence.nlp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class EntityMatcher {

	protected final AnswerType type;

	private final static Map<AnswerType, EntityMatcher> matchers = new HashMap<>();

	static {
		matchers.put(AnswerType.YES_NO, new YesNoMatcher());
		matchers.put(AnswerType.PERSON, new PersonMatcher());
		matchers.put(AnswerType.CITY, new CityMatcher());
	}

	public EntityMatcher(AnswerType type){
		this.type = type;
	}

	public AnswerType getType(){
		return this.type;
	}

	public abstract MatchResult match(String utterance, List<Object> targets, String lang);

	
	
	
	public static EntityMatcher getMatcherForType(AnswerType type) {
		return matchers.get(type);
	}

}
