package eu.marioproject.marvin.abilities.reminiscence.nlp;

public class ParseResult {
	
	private final Object resultObject;
	private final int beginMatch;
	private final int endMatch;
	private final String matchedSequence;
	private final AnswerType parserType;
	
	public ParseResult(AnswerType parserType, int beginMatch, int endMatch, String matchedSequence, Object resultObject) {
		super();
		this.resultObject = resultObject;
		this.beginMatch = beginMatch;
		this.endMatch = endMatch;
		this.matchedSequence = matchedSequence;
		this.parserType = parserType;
	}

	public Object getResultObject() {
		return resultObject;
	}

	public int getBeginMatch() {
		return beginMatch;
	}

	public int getEndMatch() {
		return endMatch;
	}

	public String getMatchedSequence() {
		return matchedSequence;
	}

	public AnswerType getParserType() {
		return parserType;
	}
	
	@Override
	public String toString(){
		return "Parser " + parserType + " matched sequence [" + beginMatch + "-" + endMatch + "] --> ("+ matchedSequence +") as: " + resultObject.toString();
	}
	
}
