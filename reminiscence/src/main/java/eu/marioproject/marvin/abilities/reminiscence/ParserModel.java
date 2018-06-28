package eu.marioproject.marvin.abilities.reminiscence;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.marioproject.marvin.abilities.reminiscence.nlp.AnswerType;

public class ParserModel {
	
	@JsonProperty
	private AnswerType type;
	@JsonProperty
	private List<String> targets = new ArrayList<>();
	
	public ParserModel() {
	}

	public AnswerType getType() {
		return type;
	}

	public void setType(AnswerType type) {
		this.type = type;
	}

	public List<String> getTargets() {
		return targets;
	}

	public void setTargets(List<String> targets) {
		this.targets = targets;
	}
	
	

}
