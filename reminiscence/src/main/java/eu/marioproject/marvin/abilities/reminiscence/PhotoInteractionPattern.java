package eu.marioproject.marvin.abilities.reminiscence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.marioproject.marvin.abilities.reminiscence.nlp.AnswerType;

@JsonInclude(value=JsonInclude.Include.NON_EMPTY)
public class PhotoInteractionPattern {
	
	private final static String PHOTO_PATTERNS_FILE = "mario" + File.separator + "reminiscence" + File.separator + "photoPatterns.json";
	private final static String PEOPLE_PATTERNS_FILE = "mario" + File.separator + "reminiscence" + File.separator + "peoplePatterns.json";
	private final static String MARRIAGE_PATTERNS_FILE = "mario" + File.separator + "reminiscence" + File.separator + "marriagePatterns.json";
	private final static String TRAVEL_PATTERNS_FILE = "mario" + File.separator + "reminiscence" + File.separator + "travelPatterns.json";
	private final static String LIVED_PLACES_PATTERNS_FILE = "mario" + File.separator + "reminiscence" + File.separator + "livedPlacePatterns.json";
	private final static String PET_PATTERNS_FILE = "mario" + File.separator + "reminiscence" + File.separator + "petPatterns.json";
	private final static String JOB_PATTERNS_FILE = "mario" + File.separator + "reminiscence" + File.separator + "jobPatterns.json";
	private final static String SCHOOL_PATTERNS_FILE = "mario" + File.separator + "reminiscence" + File.separator + "schoolPatterns.json";
	
	private final static ObjectMapper mapper = new ObjectMapper();
	
	private final static String TRUE = Boolean.TRUE.toString();
	
	@JsonProperty
	private String precondition;
	@JsonProperty
	private Map<String, List<String>> question;
	@JsonProperty
	private AnswerType answerType;
	@JsonProperty
	private List<ParserModel> parsers = new ArrayList<>();
	@JsonProperty
	private Object expectedAnswers;
	@JsonProperty
	private AnswerType matcher;
	@JsonProperty
	private Map<String, List<String>> ifMatchSay;
	@JsonProperty
	private Map<String, List<String>> ifPartialMatchSay;
	@JsonProperty
	private Map<String, List<String>> answer;
	
	public PhotoInteractionPattern() {
	}
	
	public PhotoInteractionPattern(String lang, List<String> prompts) {
		question = new HashMap<>();
		question.put(lang, prompts);
	}

	public String getPrecondition() {
		if (StringUtils.isNotBlank(precondition)) {
			return precondition;
		}
		return TRUE;
	}
	
	public String getQuestionForLang(String lang) {
		List<String> questions = question.get(lang);
		if (questions.size() == 1) {
			return questions.iterator().next();
		}
		return questions.get(RandomUtils.nextInt(0, questions.size()));
	}
	
	public AnswerType getAnswerType() {
		return answerType;
	}
	
	public List<ParserModel> getParsers() {
		return parsers;
	}

	public Object getExpectedAnswers() {
		return expectedAnswers;
	}
	
	public String getUtteranceForMatch(String lang) {
		List<String> utterances = ifMatchSay.get(lang);
		if (utterances.size() == 1) {
			return utterances.iterator().next();
		}
		return utterances.get(RandomUtils.nextInt(0, utterances.size()));
	}
	
	public String getUtteranceForPartialMatch(String lang) {
		List<String> utterances = ifPartialMatchSay.get(lang);
		if (utterances.size() == 1) {
			return utterances.iterator().next();
		}
		return utterances.get(RandomUtils.nextInt(0, utterances.size()));
	}
	
	public String getAnswer(String lang) {
		List<String> utterances = answer.get(lang);
		if (utterances.size() == 1) {
			return utterances.iterator().next();
		}
		return utterances.get(RandomUtils.nextInt(0, utterances.size()));
	}
	
	
	
	// STATIC METHODS
	
	public static List<PhotoInteractionPattern> loadPatterns() {
		return loadInteractionPatterns(PHOTO_PATTERNS_FILE);
	}
	
	public static List<PhotoInteractionPattern> loadPeoplePatterns() {	
		return loadInteractionPatterns(PEOPLE_PATTERNS_FILE);
	}
	
	public static List<PhotoInteractionPattern> loadMarriagePatterns() {	
		return loadInteractionPatterns(MARRIAGE_PATTERNS_FILE);
	}
	
	public static List<PhotoInteractionPattern> loadTravelPatterns() {	
		return loadInteractionPatterns(TRAVEL_PATTERNS_FILE);
	}
	
	public static List<PhotoInteractionPattern> loadLivedPlacesPatterns() {	
		return loadInteractionPatterns(LIVED_PLACES_PATTERNS_FILE);
	}
	
	public static List<PhotoInteractionPattern> loadPetPatterns() {	
		return loadInteractionPatterns(PET_PATTERNS_FILE);
	}
	
	public static List<PhotoInteractionPattern> loadJobPatterns() {	
		return loadInteractionPatterns(JOB_PATTERNS_FILE);
	}
	
	public static List<PhotoInteractionPattern> loadSchoolPatterns() {	
		return loadInteractionPatterns(SCHOOL_PATTERNS_FILE);
	}
	
	private static List<PhotoInteractionPattern> loadInteractionPatterns(String file) {
		try {
			return Collections.unmodifiableList(mapper.readValue(new File(file), new TypeReference<List<PhotoInteractionPattern>>(){}));
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

}
