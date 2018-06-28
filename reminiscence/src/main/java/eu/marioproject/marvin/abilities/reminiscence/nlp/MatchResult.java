package eu.marioproject.marvin.abilities.reminiscence.nlp;

import java.util.ArrayList;
import java.util.List;

public class MatchResult {
	
	public enum MatchStatus {
		MATCH,
		PARTIAL_MATCH,
		NO_MATCH;
	}
	
	private MatchStatus status = MatchStatus.NO_MATCH;
	private List<ParseResult> matchingElements = new ArrayList<>();
	private List<Object> matchingEntities = new ArrayList<>();
	private List<Object> missingEntities = new ArrayList<>();
	
	public MatchResult(MatchStatus status) {
		super();
		this.status = status;
	}

	public MatchStatus getStatus() {
		return status;
	}

	public void setStatus(MatchStatus status) {
		this.status = status;
	}

	public List<ParseResult> getMatchingElements() {
		return matchingElements;
	}

	public void setMatchingElements(List<ParseResult> matches) {
		this.matchingElements = matches;
	}
	
	public void addMatchingElement(ParseResult result) {
		matchingElements.add(result);
	}

	public List<Object> getMissingEntities() {
		return missingEntities;
	}

	public void setMissingEntities(List<Object> missingElems) {
		this.missingEntities = missingElems;
	}
	
	public void addMissing(Object missingElem) {
		if (!missingEntities.contains(missingElem)) {
			missingEntities.add(missingElem);
		}
	}
	
	public List<Object> getMatchingEntities() {
		return matchingEntities;
	}

	public void setMatchingEntities(List<Object> matchingEntities) {
		this.matchingEntities = matchingEntities;
	}
	
	public void addMatching(Object matchingElem) {
		if (!matchingEntities.contains(matchingElem)) {
			matchingEntities.add(matchingElem);
		}
	}
	
}
