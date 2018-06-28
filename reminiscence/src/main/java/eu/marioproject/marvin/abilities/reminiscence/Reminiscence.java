package eu.marioproject.marvin.abilities.reminiscence;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.el.ELProcessor;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;

// aggiunto per utilizzare una funzione random differente
import org.apache.commons.lang.math.*;

import org.ontologydesignpatterns.ont.mario.healthrole_owl.Patient;
import org.ontologydesignpatterns.ont.mario.multimediacontent_owl.Image;
import org.ontologydesignpatterns.ont.mario.person_owl.Person;
import org.ontologydesignpatterns.ont.mario.personalevents_owl.Employment;
import org.ontologydesignpatterns.ont.mario.personalevents_owl.LivingInAPlace;
import org.ontologydesignpatterns.ont.mario.personalevents_owl.Marriage;
import org.ontologydesignpatterns.ont.mario.personalevents_owl.PetOwnership;
import org.ontologydesignpatterns.ont.mario.personalevents_owl.SchoolAttendance;
import org.ontologydesignpatterns.ont.mario.personalevents_owl.Travel;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.marioproject.appsettings.ImagePrompts;
import eu.marioproject.appsettings.MyMemoriesAppSettings;
import eu.marioproject.marvin.abilities.commons.AbstractRobotAbility;
import eu.marioproject.marvin.abilities.commons.ResourceComponent;
import eu.marioproject.marvin.abilities.commons.RobotAbility;
import eu.marioproject.marvin.abilities.commons.UnderstandTextMessage;
import eu.marioproject.marvin.abilities.reminiscence.el.DefaultFunctions;
import eu.marioproject.marvin.abilities.reminiscence.el.DefaultFunctionsAndVars;
import eu.marioproject.marvin.abilities.reminiscence.el.ELFunction;
import eu.marioproject.marvin.abilities.reminiscence.el.UTF8Control;
import eu.marioproject.marvin.abilities.reminiscence.nlp.AnswerType;
import eu.marioproject.marvin.abilities.reminiscence.nlp.EntityMatcher;
import eu.marioproject.marvin.abilities.reminiscence.nlp.MatchResult;
import eu.marioproject.marvin.abilities.reminiscence.nlp.MatchResult.MatchStatus;
import eu.marioproject.marvin.config.Configuration;
import eu.marioproject.marvin.eventbus.EventBus;
import eu.marioproject.marvin.eventbus.Message;
import eu.marioproject.marvin.eventbus.MessageListener;
import eu.marioproject.marvin.eventbus.Subscriber;
import eu.marioproject.marvin.eventbus.SubscriberAlreadyExistsException;
import eu.marioproject.marvin.eventbus.SubscriberNotExistsException;
import eu.marioproject.marvin.eventbus.TopicNotExistsException;
import eu.marioproject.marvin.eventbus.impl.JsonBodyConverter;
import eu.marioproject.marvin.kb.service.EmploymentService;
import eu.marioproject.marvin.kb.service.ImageService;
import eu.marioproject.marvin.kb.service.LivedPlaceService;
import eu.marioproject.marvin.kb.service.MarriageService;
import eu.marioproject.marvin.kb.service.PatientService;
import eu.marioproject.marvin.kb.service.PersonService;
import eu.marioproject.marvin.kb.service.PetOwnershipService;
import eu.marioproject.marvin.kb.service.SchoolAttendanceService;
import eu.marioproject.marvin.kb.service.TravelService;
import eu.marioproject.marvin.kb.service.UserManager;
import eu.marioproject.marvin.sentiment.SentimentAnalysis;
import eu.marioproject.marvin.sentiment.SentimentAnalysis.SentimentScore;
import eu.marioproject.marvin.ui.gui.GraphicalUserInterface;
import eu.marioproject.marvin.ui.gui.ModalButton;
import eu.marioproject.marvin.ui.gui.ShowModalMessage;
import eu.marioproject.marvin.ui.gui.ShowPhotoMessage;
import eu.marioproject.marvin.ui.text2speech.TextToSpeech;

@Component(immediate = true, service = RobotAbility.class)
public class Reminiscence extends AbstractRobotAbility {

	private final static String ABILITY_NAME = "memories";
	private final static String ABILITY_TOPIC = ABILITY_NAME;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final String HOST_PORT = "http://localhost:8080";
	private final String BASE_URL = "/mario";

	@Reference
	private GraphicalUserInterface gui;
	@Reference
	private TextToSpeech tts;
	@Reference
	private EventBus eventBus;
	@Reference
	private Configuration config;
	
	// aggiunta
	@Reference
	private SentimentAnalysis sentimentAnalysis;

	private Subscriber ucEventsSubscriber;
	private final JsonBodyConverter<UnderstandTextMessage> understandMessageConverter = new JsonBodyConverter<>(UnderstandTextMessage.class);
	private Subscriber uiEventsSubscriber;
	private final ObjectMapper mapper = new ObjectMapper();
	private final String NEXT_ACTION = "*NEXT*";
	private final String YES_ACTION = "*YES*";
	private final String NO_ACTION = "*NO*";
	
	private String nextLabel;
	
	// aggiunta
	private String utterance;
	
	private BlockingQueue<String> userInputs = new LinkedBlockingQueue<>();
	private final String POISON_MESSAGE = "*POISON*";
	
	private ResourceBundle speechBundle;

	private DefaultFunctionsAndVars defaultFunctionsVars;

	// unmodifiable list of photo interaction patterns
	private List<PhotoInteractionPattern> photoInteractionPatterns;

	// unmodifiable list of people interaction patterns
	private List<PhotoInteractionPattern> peopleInteractionPatterns;
	
	// unmodifiable list of marriage interaction patterns
	private List<PhotoInteractionPattern> marriageInteractionPatterns;
	
	// unmodifiable list of travel interaction patterns
	private List<PhotoInteractionPattern> travelInteractionPatterns;
	
	// unmodifiable list of lived places interaction patterns
	private List<PhotoInteractionPattern> livedPlacesInteractionPatterns;
	
	// unmodifiable list of pet interaction patterns
	private List<PhotoInteractionPattern> petInteractionPatterns;
	
	// unmodifiable list of job interaction patterns
	private List<PhotoInteractionPattern> jobInteractionPatterns;
	
	// unmodifiable list of school interaction patterns
	private List<PhotoInteractionPattern> schoolInteractionPatterns;

	@Reference
	private PersonService personService;
	private Map<Person, Image> peopleWithProfilePics;

	@Reference
	private PatientService patientService;
	private Patient patient;

	@Reference
	private ImageService imageService;
	private List<Image> photos;

	private Locale currentLocale;

	private String[] introSentences;
	private String[] introHeadings;
	private String[] introPerson;
	private String[] doesNotKnowSentences;
	private String[] noWorry;
	
	@Reference
	private MarriageService marriageService;
	private Map<Marriage, List<Image>> marriagesWithImages;
	@Reference
	private TravelService travelService;
	private Map<Travel, List<Image>> travelsWithImages;
	@Reference
	private LivedPlaceService livedPlaceService;
	private Map<LivingInAPlace, List<Image>> livedPlacesWithImages;
	@Reference
	private PetOwnershipService petOwnershipService;
	private Map<PetOwnership, List<Image>> petsWithImages;
	@Reference
	private EmploymentService employmentService;
	private Map<Employment, List<Image>> jobsWithImages;
	@Reference
	private SchoolAttendanceService schoolAttendanceService;
	private Map<SchoolAttendance, List<Image>> schoolsWithImages;
	
	private DialogueContext dialogueContext;
	
	private String[] nextKeywords;
	private String nextKeywordsRegEx;
	
	private String nextRegexPattern;
	private Pattern nextRequestPattern;
	
	private List<String> confirmations;
	
	@Reference
	private SentimentAnalysis sentimentAnalyzer;
	
	private String[] positiveSentences;
	private String[] neutralSentences;
	private String[] negativeSentences;
	
	private class DialogueContext {
		
		private ELProcessor processor;
		private int totalNumQuestions;
		private Map<String, Person> mentionedPeople;
		private boolean prompedAboutPerson;
		
		private DialogueContext() {
			processor = buildELProcessor();
			totalNumQuestions = 0;
			mentionedPeople = new HashMap<>();
		}
	}
	
	@Reference
	private UserManager userManager;
	
	private MyMemoriesAppSettings appSettings;
	
	// maps image URI to custom prompts
	private Map<String, List<String>> imagesToCustomPromptsMap;
	// photos having custom prompts
	private List<Image> photosWithCustomPrompts;
	
	private String lastPrompt;

	@Activate
	protected void activate(final ComponentContext context) {
		
		// get language from config
		currentLocale = Locale.forLanguageTag(config.getLocale());
		log.info("Ability " + ABILITY_NAME + " is being activated for language " + currentLocale.getLanguage());

		// load i18n resource bundle
		loadResourceBundle(config.getResourceBundlesFolder(), currentLocale);
		log.info("Resource bundle for lang " + speechBundle.getLocale().getLanguage() + " loaded for ability " + ABILITY_NAME);

		introSentences = loadDataFromBundle("intro-pics", false);
		introHeadings = loadDataFromBundle("intro-headings", false);
		introPerson = loadDataFromBundle("person-intro", false);
		doesNotKnowSentences = loadDataFromBundle("does-not-know", true);
		noWorry = loadDataFromBundle("no-worry", false);
		
		// label and keywords for 'next' button/command
		nextLabel = speechBundle.getString("next-label");
		nextKeywords = loadDataFromBundle("next-keywords", true);
		// prepare regexp for 'next' recognition
		nextKeywordsRegEx = "\\b(" + StringUtils.join(nextKeywords, "|") + ")\\b";
		
		// confirmation sentences
		confirmations = new ArrayList<>();
		CollectionUtils.addAll(confirmations, loadDataFromBundle("confirmations", false));
		confirmations.add("");
		
		// regex pattern for next
		nextRegexPattern = speechBundle.getString("next-pattern");
		
		// positive sentences
		positiveSentences = loadDataFromBundle("positive-sentences", false);
		// neutral sentences
		neutralSentences = loadDataFromBundle("neutral-sentences", false);
		// negative sentences
		negativeSentences = loadDataFromBundle("negative-sentences", false);

		// load photo interaction patterns
		photoInteractionPatterns = PhotoInteractionPattern.loadPatterns();
		log.info("Ability " + ABILITY_NAME + ": " + photoInteractionPatterns.size() + " Photo Interaction Pattern(s) loaded");

		// load people interaction patterns
		peopleInteractionPatterns = PhotoInteractionPattern.loadPeoplePatterns();
		log.info("Ability " + ABILITY_NAME + ": " + peopleInteractionPatterns.size() + " People Interaction Pattern(s) loaded");
		
		// load marriage interaction patterns
		marriageInteractionPatterns = PhotoInteractionPattern.loadMarriagePatterns();
		log.info("Ability " + ABILITY_NAME + ": " + marriageInteractionPatterns.size() + " Marriage Interaction Pattern(s) loaded");
		
		// load travel interaction patterns
		travelInteractionPatterns = PhotoInteractionPattern.loadTravelPatterns();
		log.info("Ability " + ABILITY_NAME + ": " + travelInteractionPatterns.size() + " Travel Interaction Pattern(s) loaded");
		
		// load lived places interaction patterns
		livedPlacesInteractionPatterns = PhotoInteractionPattern.loadLivedPlacesPatterns();
		log.info("Ability " + ABILITY_NAME + ": " + livedPlacesInteractionPatterns.size() + " Lived Places Interaction Pattern(s) loaded");
		
		// load pet interaction patterns
		petInteractionPatterns = PhotoInteractionPattern.loadPetPatterns();
		log.info("Ability " + ABILITY_NAME + ": " + petInteractionPatterns.size() + " Pet Interaction Pattern(s) loaded");
		
		// load job interaction patterns
		jobInteractionPatterns = PhotoInteractionPattern.loadJobPatterns();
		log.info("Ability " + ABILITY_NAME + ": " + jobInteractionPatterns.size() + " Job Interaction Pattern(s) loaded");
		
		// load job interaction patterns
		schoolInteractionPatterns = PhotoInteractionPattern.loadSchoolPatterns();
		log.info("Ability " + ABILITY_NAME + ": " + schoolInteractionPatterns.size() + " School Interaction Pattern(s) loaded");

		// load default variables for EL
		defaultFunctionsVars = DefaultFunctionsAndVars.load();
		log.info("Ability " + ABILITY_NAME + ": default variables and functions loaded");

		log.info(getClass() + " activated with context " + context);
	}

	@Override
	public String getAbilityName() {
		return ABILITY_NAME;
	}

	@Override
	public String getAbilityTopic() {
		return ABILITY_TOPIC;
	}

	@Override
	public ResourceComponent[] getResources() {
		return new ResourceComponent[]{ResourceComponent.UI};
	}
	
	/**
	 *  Called from a thread created by the AbilityWrapper.
	 */
	@Override
	public void start() {

		System.out.println("\nAbility \"" + getAbilityName() + "\" is starting...\n");
		log.info("Ability " + getAbilityName() + " is starting...");

		// get current patient
		patient = patientService.getPatient();
		
		// load all data that can be used for reminiscence
		loadDataForReminiscence();
		
		// no pics that can be used for reminiscence in KB: inform person and terminate app
		if (noImagesForReminiscence()) {

			gui.showModalAndWait(speechBundle.getString("sorry"), speechBundle.getString("nopics"), getAbilityTopic());

			notifyAbilityCompletion();
			return;
			
		}
		// ok, there are images that can be used for reminiscence if there are applicable interaction patterns
		else {
			
			// init dialogue context
			dialogueContext = new DialogueContext();

			// build ELProcessor that contains beans, variables and functions supporting the dialogue
			//ELProcessor processor = buildELProcessor();

			// get a photo with the applicable interaction patterns
			Map<Image, List<PhotoInteractionPattern>> photoAndPatterns = getNextInteractionPattern(null);
			
			// there may be no images with applicable patterns...
			if (photoAndPatterns.isEmpty()) {
				
				gui.showModalAndWait(speechBundle.getString("sorry"), speechBundle.getString("nopics"), getAbilityTopic());
				
				// terminate ability
				abilityTermination();
				
				return;
			}
			
			// there's an image with an applicable pattern
			
			userInputs.clear();
			
			// unsubscribe before subscribe, if needed
			try {
				eventBus.unsubscribe("UCEvents", ABILITY_NAME);
				eventBus.unsubscribe("UIEvents", ABILITY_NAME);
			} catch (TopicNotExistsException | SubscriberNotExistsException e2) {
				log.info(ABILITY_NAME + " no need to unsubscribe from topics 'UCEvents' and 'UIEvents' before subscribing.");
			}
			// subscribe to the topics for receiving events
			try {
				// subscribe to the Understanding Component to receive user's utterances
				ucEventsSubscriber = eventBus.subscribe("UCEvents", ABILITY_NAME, new UCEventMessageListener());
				// subscribe to the UI to receive user's inputs on pressed buttons (next button)
				uiEventsSubscriber = eventBus.subscribe("UIEvents", ABILITY_NAME, new UIEventMessageListener());
			} catch (TopicNotExistsException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (SubscriberAlreadyExistsException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			// initial introductory sentence in modal dialog on screen
			gui.showModalAndWait(MessageFormat.format(introHeadings[RandomUtils.nextInt(0, introHeadings.length)], userManager.getCurrentUser().getFirstName()), introSentences[RandomUtils.nextInt(0, introSentences.length)], getAbilityTopic());
			
			// prepare pattern for 'does not know' recognition
			Pattern doesNotKnowPattern = Pattern.compile("\\b(" + StringUtils.join(doesNotKnowSentences, "|") + ")\\b");
			Matcher doesNotKnowMatcher;
			
			// prepare pattern for 'next' recognition
			nextRequestPattern = Pattern.compile(nextRegexPattern);

			Image selectedPhoto = null;
			List<PhotoInteractionPattern> applicablePatterns = null;
			PhotoInteractionPattern pattern = null;

			while (!photoAndPatterns.isEmpty()) {
				
				// get the photo and the applicable interaction patterns, and randomly select one of the patterns 
				selectedPhoto = photoAndPatterns.keySet().iterator().next();
				applicablePatterns = photoAndPatterns.get(selectedPhoto);
				pattern = applicablePatterns.get(RandomUtils.nextInt(0, applicablePatterns.size()));
				
				// formulate the question/prompt
				String questionTemplate = pattern.getQuestionForLang(currentLocale.getLanguage());
				String question = formulateQuestion(questionTemplate, dialogueContext.processor);
				lastPrompt = question;
				System.out.println("REMINISCENCE QUESTION/PROMPT: " + question);
				
				try {
					// show the picture and ask the question or prompt the user through the UI
					ShowPhotoMessage showPhotoMessage = new ShowPhotoMessage(question, HOST_PORT+BASE_URL+selectedPhoto.getMedia_url().iterator().next().toString());
					showPhotoMessage.setNextaction(NEXT_ACTION);
					showPhotoMessage.setNexttext(nextLabel);
					
					// clear user input before prompt
					userInputs.clear();
					gui.showPhoto(showPhotoMessage, getAbilityTopic());
					System.err.println("TOT NUM QUESTIONS/PROMPTS: " + dialogueContext.totalNumQuestions);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// get next user input from queue
				String utterance = getNextUserInput();
				// received stop command that causes the queue to be "poisoned" and return null
				if (utterance == null) {
					return;
				}
				
				// dichiaro sentiment
				SentimentScore sentiment = null;
				
				
				while(!isUserAskingForNext(utterance)) {
					
					System.out.println("USER INPUT: " + utterance);
					
					// get the Matcher for the expected answer type, if any
					AnswerType answerType = pattern.getAnswerType();
					// there's a answer type...the pattern is a question and user's input has to be processed
					if (answerType != null) {
						
						String strippedUtterance = utterance.replaceAll("[\\p{Punct}&&[^'-]]", "").toLowerCase();
						
						System.out.println("EXPECTED ANSWER TYPE: " + answerType);
						EntityMatcher matcher = EntityMatcher.getMatcherForType(answerType);
		
						// get the expected answers from the interaction pattern
						// the expected answer can be:
						// a list of strings, where a string in the list can be an EL expression to be evaluated; or
						// a string representing an EL expression that resolves to a list of objects
						Object expected = pattern.getExpectedAnswers();
						List<Object> expectedEntities = new ArrayList<>();
		
						// this is not robust; all this may fail if the json specification of the interaction pattern has 'problems'
						if (expected instanceof List<?>) {
							List<String> expectedList = (List<String>)expected;
							expectedEntities = new ArrayList<>(expectedList.size());
		
							for (String expectedString : expectedList) {
								if (expectedString.startsWith("{") && expectedString.endsWith("}")) {
									expectedString = expectedString.substring(1, expectedString.length()-1);
									Object eval = dialogueContext.processor.eval(expectedString);
									expectedEntities.add(eval);
								}
								else {
									expectedEntities.add(expectedString);
								}
							}
						}
						else if (expected instanceof String) {
							String expectedString = (String)expected;
							if (expectedString.startsWith("{") && expectedString.endsWith("}")) {
								expectedString = expectedString.substring(1, expectedString.length()-1);
								expectedEntities = (List<Object>) dialogueContext.processor.eval(expectedString);
							}
						}
		
						MatchResult matchResult = matcher.match(strippedUtterance, expectedEntities, currentLocale.getLanguage());
						System.out.println("MATCH STATUS: " + matchResult.getStatus());
						dialogueContext.processor.defineBean("matchedEntities", matchResult.getMatchingEntities());
						dialogueContext.processor.defineBean("missingEntities", matchResult.getMissingEntities());
		
						switch (matchResult.getStatus()) {
						case MATCH:
							String parametricSysUtterance = pattern.getUtteranceForMatch(currentLocale.getLanguage());
							String sysUtterance = formulateQuestion(parametricSysUtterance, dialogueContext.processor);
							System.out.println("ROBOT REPLIES: " + sysUtterance);
							tts.speakAndWait(sysUtterance, false, getAbilityTopic());
							break;
						case PARTIAL_MATCH:
							String parametricSysUtterancePartial = pattern.getUtteranceForPartialMatch(currentLocale.getLanguage());
							String sysUtterancePartial = formulateQuestion(parametricSysUtterancePartial, dialogueContext.processor);
							System.out.println("ROBOT REPLIES: " + sysUtterancePartial);
							tts.speakAndWait(sysUtterancePartial, false, getAbilityTopic());
							break;
						case NO_MATCH:
							// check if user does not know
							doesNotKnowMatcher = doesNotKnowPattern.matcher(strippedUtterance);
							if (doesNotKnowMatcher.find()) {
								tts.speakAndWait(noWorry[RandomUtils.nextInt(0, noWorry.length)], false, getAbilityTopic());
								// here tell the reply...
								String parametricAnswer = pattern.getAnswer(currentLocale.getLanguage());
								String answer = formulateQuestion(parametricAnswer, dialogueContext.processor);
								tts.speakAndWait(answer, false, getAbilityTopic());
							}
							break;
						default:
							break;
						}
					}
					else {
						sentiment = sentimentAnalyzer.getSentiment(utterance, currentLocale.getLanguage());
						switch (sentiment) {
						case POSITIVE:
							tts.speak(positiveSentences[RandomUtils.nextInt(0, positiveSentences.length)], false, getAbilityTopic());
							break;
						case NEGATIVE:
							tts.speak(negativeSentences[RandomUtils.nextInt(0, negativeSentences.length)], false, getAbilityTopic());
							break;
						case NEUTRAL:
							int random = RandomUtils.nextInt(1, 11);
							System.err.println("Rand " + random);
							if (random < 8) {
								tts.speak(neutralSentences[RandomUtils.nextInt(0, neutralSentences.length)], false, getAbilityTopic());
							}
							break;
						default:
							break;
						}
					}
					
					// get next user input from queue
					utterance = getNextUserInput();
					// received stop command that causes the queue to be "poisoned" and return null
					if (utterance == null) {
						return;
					}
				}
				
				// get next image with pattern
				photoAndPatterns = getNextInteractionPattern(sentiment);
				
				// all photos were used...reload them and reinitialize the dialogue context
				if (photoAndPatterns.isEmpty()) {
					userInputs.clear();
					askYesNoModal();
					boolean gotUserAnswer = false;
					// wait for user's choice between yes/no
					String userInput;
					do {
						userInput = getNextUserInput();
						// received stop command
						if (userInput == null) {
							return;
						}
						// pressed yes button
						if (userInput.equals(YES_ACTION)) {
							// reload
							loadDataForReminiscence();
							dialogueContext = new DialogueContext();
							photoAndPatterns = getNextInteractionPattern(null);
							gotUserAnswer = true;
						}
						// pressed no button
						else if (userInput.equals(NO_ACTION)) {
							gotUserAnswer = true;
						}
						else {
							EntityMatcher yesNoMatcher = EntityMatcher.getMatcherForType(AnswerType.YES_NO);
							MatchResult yesResult = yesNoMatcher.match(userInput, Arrays.asList("YES"), currentLocale.getLanguage());
							// user says yes
							if (yesResult.getStatus().equals(MatchStatus.MATCH)) {
								// reload
								loadDataForReminiscence();
								dialogueContext = new DialogueContext();
								photoAndPatterns = getNextInteractionPattern(null);
								gotUserAnswer = true;
							}
							else {
								MatchResult noResult = yesNoMatcher.match(userInput, Arrays.asList("NO"), currentLocale.getLanguage());
								// user says no
								if (noResult.getStatus().equals(MatchStatus.MATCH)) {
									gotUserAnswer = true;
								}
							}
						}
					} while(!gotUserAnswer);
				}
				
			}
			/*
			ShowModalMessage sorryMessage = new ShowModalMessage(speechBundle.getString("sorry"), speechBundle.getString("no-other-pics"));
			gui.showModal(sorryMessage, getAbilityTopic());
			pause(8000);
			*/
			abilityTermination();

		}

	}
	
	private boolean isUserAskingForNext(String userInput) {
		// pressed 'next' button
		if (userInput.equals(NEXT_ACTION)) {
			return true;
		}
		// check exact keyword matching
		if (userInput.toLowerCase().matches(nextKeywordsRegEx)) {
			String confirmation = confirmations.get(RandomUtils.nextInt(0, confirmations.size()));
			if (StringUtils.isNotBlank(confirmation)) {
				tts.speakAndWait(confirmation, false, getAbilityTopic());
			}
			return true;
		}
		// check regexp
		String userInputStripped = userInput.replaceAll("[\\p{Punct}&&[^'-]]", "").toLowerCase();
		Matcher matcher = nextRequestPattern.matcher(userInputStripped);
		if (matcher.find()) {
			String confirmation = confirmations.get(RandomUtils.nextInt(0, confirmations.size()));
			if (StringUtils.isNotBlank(confirmation)) {
				tts.speakAndWait(confirmation, false, getAbilityTopic());
			}
			return true;
		}
		return false;
	}
	
	private void abilityTermination() {
		try {
			if (ucEventsSubscriber != null) {
				eventBus.unsubscribe("UCEvents", ABILITY_TOPIC);
			}
			if (uiEventsSubscriber != null) {
				eventBus.unsubscribe("UIEvents", ABILITY_TOPIC);
			}
		} catch (TopicNotExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SubscriberNotExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// informs task manager about ability termination
		notifyAbilityCompletion();
	}
	
	private void loadDataForReminiscence() {
		
		// get images with custom propts from user-specific app settings file
		appSettings = MyMemoriesAppSettings.load(config.getFullUserDataFolder() + File.separator + userManager.getCurrentUser().getUserCode() + File.separator + "mymemories-app" + File.separator + MyMemoriesAppSettings.defaultFileName);
		List<ImagePrompts> imagesAndPrompts = appSettings.getImagesAndPrompts();
		imagesToCustomPromptsMap = new HashMap<>(imagesAndPrompts.size());
		photosWithCustomPrompts = new ArrayList<>(imagesAndPrompts.size());
		for (ImagePrompts imageAndPrompts : imagesAndPrompts) {
			imagesToCustomPromptsMap.put(imageAndPrompts.getImageURI(), imageAndPrompts.getCustomPrompts());
			photosWithCustomPrompts.add(Image.get(imageAndPrompts.getImageURI()));
		}
				
				
		// get all images which are not profile pictures and are not associated with any event
		photos = imageService.getImagesNotProfilePic();
		System.err.println("CANDIDATE IMGS: " + photos.size());
		// filter out images with custom prompts
		removeImagesWithCustomPrompts(photos);
		System.err.println("CANDIDATE IMGS AFTER REMOVAL: " + photos.size());
		
		// get all people with profile pics
		peopleWithProfilePics = personService.getPeopleWithProfilePicture();
		
		// load life events with images
		loadLifeEventsWithImages();

	}
	
	private void removeImagesWithCustomPrompts(List<Image> images) {
		if (images.isEmpty() || imagesToCustomPromptsMap.isEmpty()) {
			return;
		}
		
		List<Image> toBeRemoved = new ArrayList<>();
		for (Image img : images) {
			if (imagesToCustomPromptsMap.containsKey(img.getId())) {
				toBeRemoved.add(img);
			}
		}
		
		for (Image img : toBeRemoved) {
			
			images.remove(img);
		}
		
	}

	/**
	 * Randomly selects a photo and evaluates the applicable patterns until a photo is found with
	 * at least one applicable pattern. Selected photos are removed from the list of available photos
	 * to keep track of the fact that there are no applicable patterns or the photo is selected and should
	 * not be reused in this reminiscence session.
	 * 
	 * @return a mapping between the selected photo and the applicable patterns, or an empty map if there are no
	 * applicable patterns
	 */
	private Map<Image, List<PhotoInteractionPattern>> selectPhoto(ELProcessor processor) {

		if (photos.isEmpty() && imagesToCustomPromptsMap.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Image, List<PhotoInteractionPattern>> result = Collections.emptyMap();
		
		if (!photos.isEmpty()) {
			boolean found = false;
			List<PhotoInteractionPattern> applicablePatterns;
			do {
				int randomPicIndex = RandomUtils.nextInt(0, photos.size());
				Image photo = photos.get(randomPicIndex);
				System.err.println("TRYING PATTERNS FOR " + photo.getId());
				applicablePatterns = getApplicablePatterns(photo, processor);
				System.err.println("NUM PATTERNS " + applicablePatterns.size());
				if (!applicablePatterns.isEmpty()) {
					result = Collections.singletonMap(photo, applicablePatterns);
					found = true;
				}
				photos.remove(photo);
			} while (!found && !photos.isEmpty());
		}
		
		if (result.isEmpty()) {
			if (!photosWithCustomPrompts.isEmpty()) {
				int randomPicIndex = RandomUtils.nextInt(0, photosWithCustomPrompts.size());
				Image photoWithCustomPrompt = photosWithCustomPrompts.remove(randomPicIndex);
				List<String> customPrompts = imagesToCustomPromptsMap.remove(photoWithCustomPrompt.getId());
				// try to avoid re-using the same prompt
				if (customPrompts.size() > 1 && customPrompts.contains(lastPrompt)) {
					customPrompts.remove(lastPrompt);
				}
				processor.defineBean("photo", photoWithCustomPrompt);
				PhotoInteractionPattern interactionPattern = new PhotoInteractionPattern(currentLocale.getLanguage(), customPrompts);
				List<PhotoInteractionPattern> interactionPatternsList = new ArrayList<>();
				interactionPatternsList.add(interactionPattern);
				result = Collections.singletonMap(photoWithCustomPrompt, interactionPatternsList);
			}
		}

		return result;
	}

	/**
	 * Evaluates the applicable patterns wrt the provided image and returns the list of
	 * applicable pattern, if any.
	 * 
	 * @param photo the image for which to evaluate the applicable patterns
	 * @return the list of applicable patterns for the photo, or an empty list if there are no
	 * applicable patterns
	 */
	private List<PhotoInteractionPattern> getApplicablePatterns(Image photo, ELProcessor processor) {

		processor.defineBean("photo", photo);
		List<PhotoInteractionPattern> applicablePatterns = new ArrayList<>();
		//ELProcessor processor = buildELProcessor(photo);
		// for each pattern evaluate pattern precondition wrt the photo
		for (PhotoInteractionPattern pattern : photoInteractionPatterns) {
			System.err.println("TRYING PATTERN " + pattern.getPrecondition());
			if ((Boolean)processor.getValue(pattern.getPrecondition(), Boolean.class)) {
				applicablePatterns.add(pattern);
				System.err.println("PATTERN MATCH!");
			}
		}
		return applicablePatterns;
	}
	
	/**
	 * Determines which of the provided patterns are applicable wrt the provided image and returns the list of
	 * applicable pattern, if any.
	 * 
	 * @param photo the image for which to evaluate the applicable patterns
	 * @param patterns the patterns to be evaluated
	 * @return the list of applicable patterns for the photo, or an empty list if there are no
	 * applicable patterns
	 */
	private List<PhotoInteractionPattern> getApplicablePatterns(Image photo, ELProcessor processor, List<PhotoInteractionPattern> patterns) {

		processor.defineBean("photo", photo);
		List<PhotoInteractionPattern> applicablePatterns = new ArrayList<>();
		//ELProcessor processor = buildELProcessor(photo);
		// for each pattern evaluate pattern precondition wrt the photo
		for (PhotoInteractionPattern pattern : patterns) {
			if ((Boolean)processor.getValue(pattern.getPrecondition(), Boolean.class)) {
				applicablePatterns.add(pattern);
			}
		}
		return applicablePatterns;
	}

	/**
	 * Checks if among the mentioned people there is one having a profile pic and for which there is at least one applicable
	 * interaction pattern. If so, returns the profile pic and the applicable patterns for the person, which is set as a bean in the ELProcessor.
	 * @param processor the ELProcessor being used in the dialogue
	 * @param mentionedPeople the map (URI, Person) of people mentioned so far in the dialogue.
	 * @return
	 */
	private Map<Image, List<PhotoInteractionPattern>> selectPersonWithProfilePic(ELProcessor processor, Map<String, Person> mentionedPeople) {

		List<PhotoInteractionPattern> applicablePatterns;
		Set<String> mentionedPeopleURIs = mentionedPeople.keySet();
		// iterate over mentioned people
		for (String mentionedPersonURI : mentionedPeopleURIs) {
			// for each mentioned person, iterate over people with profile pics
			Iterator<Person> peopleIterator = peopleWithProfilePics.keySet().iterator();
			//for (Person personWithProfilePic : peopleWithProfilePics.keySet()) {
			while (peopleIterator.hasNext()) {
				Person personWithProfilePic = peopleIterator.next();
				// the mentioned person has profile pic
				if (personWithProfilePic.getId().equals(mentionedPersonURI)) {
					// check applicable person interaction patterns
					applicablePatterns = getApplicablePersonPatterns(personWithProfilePic, processor);
					// there is at least one pattern applicable for the person
					if (!applicablePatterns.isEmpty()) {
						Map<Image, List<PhotoInteractionPattern>> result = new HashMap<>();
						result.put(peopleWithProfilePics.get(personWithProfilePic), applicablePatterns);
						// remove the person, as s/he will be used for a question 
						// this removes from the keysey AND from the peopleWithProfilePics map
						peopleIterator.remove();
						return result;
					}
					else {
						// remove the person, as there are no patterns 
						// this removes from the keyset AND from the peopleWithProfilePics map
						peopleIterator.remove();
					}
				}
			}
		}

		return Collections.emptyMap();
	}
	
	private Map<Image, List<PhotoInteractionPattern>> selectPersonWithProfilePic(ELProcessor processor) {

		List<PhotoInteractionPattern> applicablePatterns;
		Iterator<Person> peopleIterator = peopleWithProfilePics.keySet().iterator();
		while (peopleIterator.hasNext()) {
			Person personWithProfilePic = peopleIterator.next();

			applicablePatterns = getApplicablePersonPatterns(personWithProfilePic, processor);
			// there is at least one pattern applicable for the person
			if (!applicablePatterns.isEmpty()) {
				Map<Image, List<PhotoInteractionPattern>> result = new HashMap<>();
				result.put(peopleWithProfilePics.get(personWithProfilePic), applicablePatterns);
				// remove the person, as s/he will be used for a question 
				// this removes from the keyset AND from the peopleWithProfilePics map
				peopleIterator.remove();
				return result;
			}
			else {
				// remove the person, as there are no patterns 
				// this removes from the keysey AND from the peopleWithProfilePics map
				peopleIterator.remove();
			}
		}

		return Collections.emptyMap();
	}

	private List<PhotoInteractionPattern> getApplicablePersonPatterns(Person person, ELProcessor processor) {
		// add person as bean in ELProcessor
		processor.defineBean("person", person);
		List<PhotoInteractionPattern> applicablePatterns = new ArrayList<>();
		// for each pattern evaluate pattern precondition
		for (PhotoInteractionPattern pattern : peopleInteractionPatterns) {
			System.err.println("Evaluating person pattern " + pattern.getPrecondition() + " for " + person.getPerson_firstName().iterator().next());
			if ((Boolean)processor.getValue(pattern.getPrecondition(), Boolean.class)) {
				applicablePatterns.add(pattern);
				System.err.println("APPLICABLE!");
			}
			else {
				System.err.println("NOT APPLICABLE!");
			}
		}
		return applicablePatterns;
	}


	@Override
	protected void doStop() {
		System.out.println("\nABILITY " + getAbilityName() + " STOPPING!\n");
		userInputs.clear();
		try {
			userInputs.put(POISON_MESSAGE);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			if (ucEventsSubscriber != null) {
				eventBus.unsubscribe("UCEvents", ABILITY_TOPIC);
				ucEventsSubscriber = null;
			}
			if (uiEventsSubscriber != null) {
				eventBus.unsubscribe("UIEvents", ABILITY_TOPIC);
				uiEventsSubscriber = null;
			}
		} catch (TopicNotExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SubscriberNotExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getNextUserInput() {
		// get input from blocking queue
		try {
			String input = userInputs.take().trim();
			if (input.equals(POISON_MESSAGE)) {
				System.err.println("POISONED!");
				return null;
			}
			return input;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private void loadResourceBundle(String folder, Locale locale) {
		// load resource bundle
		try {
			File file = new File(folder);
			URL theUrl = null;
			theUrl = file.toURI().toURL();
			URL[] urls = {theUrl};
			ClassLoader loader = new URLClassLoader(urls);
			speechBundle = ResourceBundle.getBundle("Reminiscence", locale, loader, new UTF8Control());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String[] loadDataFromBundle(String key, boolean lowerCase) {
		String[] result = null;
		if (speechBundle != null) {
			try {
				// load string pattern from bundle
				String pattern = speechBundle.getString(key);
				String[] patternElems = pattern.split("\\|");
				result = new String[patternElems.length];
				for (int i = 0; i < patternElems.length; i++) {
					if (lowerCase) {
						result[i] = patternElems[i].trim().toLowerCase();
					}
					else {
						result[i] = patternElems[i].trim();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	
	/**
	 * <p>Initializes the {@link ELProcessor} that is used for the reminiscence dialogue.</p>
	 * <p>The {@code ELProcessor} is initialized with the current patient, the current language and the set
	 * of functions and variables used to evaluate EL expressions.</p>
	 * @return the initialized {@code ELProcessor}
	 */
	private ELProcessor buildELProcessor() {

		ELProcessor processor = null;

		/*
		 * ContextClassLoader switch to avoid issues due to the inability to locate classes when an ELProcessor is
		 * built and the default functions are registered;
		 * the EL 3.0 library/jar is currently embedded in the reminiscence bundle.
		 */
		final ClassLoader orig = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(com.sun.el.ExpressionFactoryImpl.class.getClassLoader());
		//Thread.currentThread().setContextClassLoader( DefaultFunctionsAndVars.class.getClassLoader() );
		try {
			processor = new ELProcessor();

			// add default beans
			processor.defineBean("patient", patient);

			// add language String as a bean
			processor.defineBean("lang", currentLocale.getLanguage());

			// add default functions and variables
			for (ELFunction function : defaultFunctionsVars.getDefaultFunctions()) {
				try {
					processor.defineFunction(function.getPrefix(), function.getFunction(), function.getClassName(), function.getMethod());
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Map<String, String> defaultVars = defaultFunctionsVars.getDefaultVariables();
			for (String variable : defaultVars.keySet()) {
				processor.setVariable(variable, defaultVars.get(variable));
			}
		} finally {
			// reset ContextClassLoader to the initial one
			Thread.currentThread().setContextClassLoader(orig);
		}

		return processor;
	}

	private String formulateQuestion(String question, ELProcessor processor) {
		Pattern pattern = Pattern.compile("\\{(.*?)\\}");
		Matcher matcher = pattern.matcher(question);
		Map<String,String> expressions = new HashMap<String, String>();
		while (matcher.find()) {
			expressions.put(matcher.group(1), matcher.group());
		}
		for (String expr : expressions.keySet()) {
			//System.out.println("EVALUATING: " + expr);
			Object val = processor.eval(expr);
			question = question.replace(expressions.get(expr), val.toString());
		}
		return question;
	}

	private String getRandomString(String[] values) {
		return values[RandomUtils.nextInt(0, values.length)];
	}

	public static void main(String[] args) {
		String cityName = "L'aquila";
		String stripped = cityName.replaceAll("[\\p{Punct}\\s]", "-");
		System.err.println(stripped);
		
		Pattern pattern = Pattern.compile("\\b((next|another)\\s(photo|picture|image|pic|one|please))|(the\\snext$)\\b");
		Matcher matcher = pattern.matcher("please the next one".replaceAll("[\\p{Punct}&&[^'-]]", ""));
		if (matcher.find()) {
			System.err.println(true);
		}
		
	}
	
	
	private class UCEventMessageListener implements MessageListener {

		@Override
		public void handleMessage(Message message) {
			UnderstandTextMessage understandMessage;
			understandMessage = message.getBody(understandMessageConverter);
			if (understandMessage.getAbility().equalsIgnoreCase(ABILITY_NAME)) {
				String utterance = understandMessage.getText();
				System.err.println("USER SAYS: " + utterance);
				// put in queue
				try {
					userInputs.put(utterance);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		
	}
	
	private class UIEventMessageListener implements MessageListener {

		@Override
		public void handleMessage(Message message) {
			String messageBody = message.getBody();
			try {
				JsonNode root =  mapper.readTree(messageBody);
				// check if the message is for me
				if (root.has("ability") && root.get("ability").asText().equalsIgnoreCase(getAbilityName())) {
					// is it a subscription event?
					if (root.has("event") && root.get("event").asText().equalsIgnoreCase("touch") && root.has("action")) { //&& root.get("action").asText().equalsIgnoreCase(NEXT_ACTION)) {
						try {
							userInputs.put(root.get("action").asText());
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			} catch (IOException e) {
				log.error(getAbilityName() + ": unable to parse the following message body\n" + messageBody);
				e.printStackTrace();
			}
			
		}
		
	}
	
	private void loadLifeEventsWithImages() {
		// marriages
		marriagesWithImages = marriageService.getMarriageEventsWithPics();
		System.err.println("MARRIAGES WITH IMAGES: " + marriagesWithImages.size());
		// travels
		travelsWithImages = travelService.getTravelEventsWithPics();
		System.err.println("TRAVELS WITH IMAGES: " + travelsWithImages.size());
		// lived places
		livedPlacesWithImages = livedPlaceService.getLivedPlaceEventsWithPics();
		System.err.println("LIVED PLACES WITH IMAGES: " + livedPlacesWithImages.size());
		// pets
		petsWithImages = petOwnershipService.getPetEventsWithPics();
		System.err.println("PET OWNERSHIPS WITH IMAGES: " + petsWithImages.size());
		// jobs
		jobsWithImages = employmentService.getEmploymentEventsWithPics();
		System.err.println("JOBS WITH IMAGES: " + jobsWithImages.size());
		// schools
		schoolsWithImages = schoolAttendanceService.getSchoolEventsWithPics();
		System.err.println("SCHOOLS WITH IMAGES: " + schoolsWithImages.size());
	}
	
	
	/**
	 * Return true if there are no pictures that can be used for reminiscence. If this returns false, it's
	 * still important to check if there applicable interaction patterns for the available images.
	 * @return <code>true</code> if there are no pictures that can be used for reminiscence
	 */
	private boolean noImagesForReminiscence() {
		return photos.isEmpty() &&
				peopleWithProfilePics.isEmpty() &&
				marriagesWithImages.isEmpty() &&
				travelsWithImages.isEmpty() &&
				livedPlacesWithImages.isEmpty() &&
				petsWithImages.isEmpty() &&
				jobsWithImages.isEmpty() &&
				schoolsWithImages.isEmpty() &&
				imagesToCustomPromptsMap.isEmpty();
	}
	
	// Personal Contribution
	
	/*	
	 
		private Map<Image, List<PhotoInteractionPattern>> showNextPicture(ELProcessor processor) {
			List<PhotoInteractionPattern> applicablePatterns;
			
			// iterate over all the events
			Iterator<Marriage> marriageIterator = marriagesWithImages.keySet().iterator();
			Iterator<Travel> travelIterator = travelsWithImages.keySet().iterator();
			Iterator<LivingInAPlace> livedPlaceIterator = livedPlacesWithImages.keySet().iterator();
			Iterator<PetOwnership> eventsIterator = petsWithImages.keySet().iterator();
			Iterator<Employment> eventIterator = jobsWithImages.keySet().iterator();
			Iterator<SchoolAttendance> event2Iterator = schoolsWithImages.keySet().iterator();
		} 
	
	*/
	
	private Map<Image, List<PhotoInteractionPattern>> getMarriagePicWithPatterns(ELProcessor processor) {
		List<PhotoInteractionPattern> applicablePatterns;
		
		// iterate over marriage events
		Iterator<Marriage> marriageIterator = marriagesWithImages.keySet().iterator();
		while (marriageIterator.hasNext()) {
			Marriage marriageWithPics = marriageIterator.next();
			// set marriage as bean in EL processor
			processor.defineBean("marriageEvent", marriageWithPics);
			// get list of pics for marriage
			List<Image> marriagePics = marriagesWithImages.get(marriageWithPics);
			// iterate over the list of images
			Iterator<Image> imagesIterator = marriagePics.iterator();
			while (imagesIterator.hasNext()) {
				Image image = imagesIterator.next();
				applicablePatterns = getApplicablePatterns(image, processor, marriageInteractionPatterns);
				if (!applicablePatterns.isEmpty()) {
					// found an image for the event with applicable patters: return the image with the patterns
					Map<Image, List<PhotoInteractionPattern>> result = new HashMap<>();
					result.put(image, applicablePatterns);
					// remove the image as it will be used
					imagesIterator.remove();
					return result;
				}
				else {
					// no patterns for image, remove the image
					imagesIterator.remove();
				}
			}
			// if here, there are no images with applicable pattern or all images were used, so remove the event
			marriageIterator.remove();
		}
		// if here, there are no events with images having patterns or all events were used
		return Collections.emptyMap();
	
	}
	
	private Map<Image, List<PhotoInteractionPattern>> getTravelPicWithPatterns(ELProcessor processor) {
		List<PhotoInteractionPattern> applicablePatterns;
		
		// iterate over travel events
		Iterator<Travel> travelIterator = travelsWithImages.keySet().iterator();
		while (travelIterator.hasNext()) {
			Travel travelWithPics = travelIterator.next();
			// set marriage as bean in EL processor
			processor.defineBean("travelEvent", travelWithPics);
			// get list of pics for travel
			List<Image> travelPics = travelsWithImages.get(travelWithPics);
			// iterate over the list of images
			Iterator<Image> imagesIterator = travelPics.iterator();
			while (imagesIterator.hasNext()) {
				Image image = imagesIterator.next();
				applicablePatterns = getApplicablePatterns(image, processor, travelInteractionPatterns);
				if (!applicablePatterns.isEmpty()) {
					// found an image for the event with applicable patters: return the image with the patterns
					Map<Image, List<PhotoInteractionPattern>> result = new HashMap<>();
					result.put(image, applicablePatterns);
					// remove the image as it will be used
					imagesIterator.remove();
					return result;
				}
				else {
					// no patterns for image, remove the image
					imagesIterator.remove();
				}
			}
			// if here, there are no images with applicable pattern or all images were used, so remove the event
			travelIterator.remove();
		}
		// if here, there are no events with images having patterns or all events were used
		return Collections.emptyMap();
	
	}
	
	private Map<Image, List<PhotoInteractionPattern>> getLivedPlacePicWithPatterns(ELProcessor processor) {
		List<PhotoInteractionPattern> applicablePatterns;
		
		// iterate over travel events
		Iterator<LivingInAPlace> livedPlaceIterator = livedPlacesWithImages.keySet().iterator();
		while (livedPlaceIterator.hasNext()) {
			LivingInAPlace placeWithPics = livedPlaceIterator.next();
			// set marriage as bean in EL processor
			processor.defineBean("livedPlaceEvent", placeWithPics);
			// get list of pics for lived place
			List<Image> livedPics = livedPlacesWithImages.get(placeWithPics);
			// iterate over the list of images
			Iterator<Image> imagesIterator = livedPics.iterator();
			while (imagesIterator.hasNext()) {
				Image image = imagesIterator.next();
				applicablePatterns = getApplicablePatterns(image, processor, livedPlacesInteractionPatterns);
				if (!applicablePatterns.isEmpty()) {
					// found an image for the event with applicable patters: return the image with the patterns
					Map<Image, List<PhotoInteractionPattern>> result = new HashMap<>();
					result.put(image, applicablePatterns);
					// remove the image as it will be used
					imagesIterator.remove();
					return result;
				}
				else {
					// no patterns for image, remove the image
					imagesIterator.remove();
				}
			}
			// if here, there are no images with applicable pattern or all images were used, so remove the event
			livedPlaceIterator.remove();
		}
		// if here, there are no events with images having patterns or all events were used
		return Collections.emptyMap();
	
	}
	
	private Map<Image, List<PhotoInteractionPattern>> getPetPicWithPatterns(ELProcessor processor) {
		List<PhotoInteractionPattern> applicablePatterns;
		
		// iterate over pet ownership events
		Iterator<PetOwnership> eventsIterator = petsWithImages.keySet().iterator();
		while (eventsIterator.hasNext()) {
			PetOwnership eventWithPics = eventsIterator.next();
			// set marriage as bean in EL processor
			processor.defineBean("petEvent", eventWithPics);
			// get list of pics for pet
			List<Image> eventPics = petsWithImages.get(eventWithPics);
			// iterate over the list of images
			Iterator<Image> imagesIterator = eventPics.iterator();
			while (imagesIterator.hasNext()) {
				Image image = imagesIterator.next();
				applicablePatterns = getApplicablePatterns(image, processor, petInteractionPatterns);
				if (!applicablePatterns.isEmpty()) {
					// found an image for the event with applicable patters: return the image with the patterns
					Map<Image, List<PhotoInteractionPattern>> result = new HashMap<>();
					result.put(image, applicablePatterns);
					// remove the image as it will be used
					imagesIterator.remove();
					return result;
				}
				else {
					// no patterns for image, remove the image
					imagesIterator.remove();
				}
			}
			// if here, there are no images with applicable pattern or all images were used, so remove the event
			eventsIterator.remove();
		}
		// if here, there are no events with images having patterns or all events were used
		return Collections.emptyMap();
	
	}
	
	private Map<Image, List<PhotoInteractionPattern>> getJobPicWithPatterns(ELProcessor processor) {
		List<PhotoInteractionPattern> applicablePatterns;
		
		// iterate over job events
		Iterator<Employment> eventsIterator = jobsWithImages.keySet().iterator();
		while (eventsIterator.hasNext()) {
			Employment eventWithPics = eventsIterator.next();
			// set job event as bean in EL processor
			processor.defineBean("jobEvent", eventWithPics);
			// get list of pics for job
			List<Image> eventPics = jobsWithImages.get(eventWithPics);
			// iterate over the list of images
			Iterator<Image> imagesIterator = eventPics.iterator();
			while (imagesIterator.hasNext()) {
				Image image = imagesIterator.next();
				applicablePatterns = getApplicablePatterns(image, processor, jobInteractionPatterns);
				if (!applicablePatterns.isEmpty()) {
					// found an image for the event with applicable patters: return the image with the patterns
					Map<Image, List<PhotoInteractionPattern>> result = new HashMap<>();
					result.put(image, applicablePatterns);
					// remove the image as it will be used
					imagesIterator.remove();
					return result;
				}
				else {
					// no patterns for image, remove the image
					imagesIterator.remove();
				}
			}
			// if here, there are no images with applicable pattern or all images were used, so remove the event
			eventsIterator.remove();
		}
		// if here, there are no events with images having patterns or all events were used
		return Collections.emptyMap();
	
	}
	
	private Map<Image, List<PhotoInteractionPattern>> getSchoolPicWithPatterns(ELProcessor processor) {
		List<PhotoInteractionPattern> applicablePatterns;
		
		// iterate over school events
		Iterator<SchoolAttendance> eventsIterator = schoolsWithImages.keySet().iterator();
		while (eventsIterator.hasNext()) {
			SchoolAttendance eventWithPics = eventsIterator.next();
			// set job event as bean in EL processor
			processor.defineBean("schoolEvent", eventWithPics);
			// get list of pics for school
			List<Image> eventPics = schoolsWithImages.get(eventWithPics);
			// iterate over the list of images
			Iterator<Image> imagesIterator = eventPics.iterator();
			while (imagesIterator.hasNext()) {
				Image image = imagesIterator.next();
				applicablePatterns = getApplicablePatterns(image, processor, schoolInteractionPatterns);
				if (!applicablePatterns.isEmpty()) {
					// found an image for the event with applicable patters: return the image with the patterns
					Map<Image, List<PhotoInteractionPattern>> result = new HashMap<>();
					result.put(image, applicablePatterns);
					// remove the image as it will be used
					imagesIterator.remove();
					return result;
				}
				else {
					// no patterns for image, remove the image
					imagesIterator.remove();
				}
			}
			// if here, there are no images with applicable pattern or all images were used, so remove the event
			eventsIterator.remove();
		}
		// if here, there are no events with images having patterns or all events were used
		return Collections.emptyMap();
	
	}
	
	// private Map<Image, List<PhotoInteractionPattern>>
	
	private Map<Image, List<PhotoInteractionPattern>> getNextInteractionPattern(SentimentScore sentiment) {
		
		// all images where used...reset dialogue context, reload data and retry recursively!
		
		Map<Image, List<PhotoInteractionPattern>> imageWithInteractionPattern;
		
		/*
		 * Try to show pictures based on pattern and on sentiment analysis
		 * 
		 * @author Bruno Marafini
		 * @theme Personal Contribution
		 */
		
        /*SentimentScore sentiment = sentimentAnalysis.getSentiment(utterance, Locale.forLanguageTag(config.getLocale()).getLanguage());
		String userName = userManager.getCurrentUser().getFirstName();
		switch (sentiment) {
		case POSITIVE:
			tts.say(MessageFormat.format(positiveSentences[RandomUtils.nextInt(positiveSentences.length)], userName));
			imageWithInteractionPattern = getMarriagePicWithPatterns(dialogueContext.processor);
			if (!imageWithInteractionPattern.isEmpty()) {
				updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
				dialogueContext.totalNumQuestions++;
				dialogueContext.prompedAboutPerson = false;
				return imageWithInteractionPattern;
			}
			//break;
		case NEGATIVE:
			tts.say(MessageFormat.format(negativeSentences[RandomUtils.nextInt(negativeSentences.length)], userName));
			
			
			//break;
		case NEUTRAL:
			tts.say(MessageFormat.format(neutralSentences[RandomUtils.nextInt(neutralSentences.length)], userName));
			
			
			//break;
		default:
			break;
		}*/
		
		// try with marriage pattern
		imageWithInteractionPattern = getMarriagePicWithPatterns(dialogueContext.processor);
		//sentiment = sentimentAnalyzer.getSentiment(utterance, currentLocale.getLanguage());
		if (!imageWithInteractionPattern.isEmpty() && sentiment == null) { 
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL){
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.NEGATIVE){
			marriagesWithImages.clear();
			sentiment = null;
		}
		
		/*if (sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL) {
			getMarriagePicWithPatterns(dialogueContext.processor);
			// getNextInteractionPattern
			// return new HashMap<Image, List<PhotoInteractionPattern>>(0);
		} else if (sentiment == SentimentScore.NEGATIVE) { 
			marriagesWithImages.clear();
		}*/
		
		// try with mentioned person
		imageWithInteractionPattern = selectPersonWithProfilePic(dialogueContext.processor, dialogueContext.mentionedPeople);
		if (!imageWithInteractionPattern.isEmpty() && !dialogueContext.prompedAboutPerson && sentiment == null) {
			String personIntroSentence = MessageFormat.format(getRandomString(introPerson), DefaultFunctions.getPersonNameWithRelationship((Person)(dialogueContext.processor.eval("person")),patient,currentLocale.getLanguage()));
			tts.speakAndWait(personIntroSentence, false, getAbilityTopic());
			dialogueContext.prompedAboutPerson = true;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL) {
			String personIntroSentence = MessageFormat.format(getRandomString(introPerson), DefaultFunctions.getPersonNameWithRelationship((Person)(dialogueContext.processor.eval("person")),patient,currentLocale.getLanguage()));
			tts.speakAndWait(personIntroSentence, false, getAbilityTopic());
			dialogueContext.prompedAboutPerson = true;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.NEGATIVE) {
			peopleWithProfilePics.clear();
			sentiment = null;
		}
		
//		if (SentimentScore.POSITIVE != null && SentimentScore.NEUTRAL != null) {
//			selectPersonWithProfilePic(dialogueContext.processor);
//			// getNextInteractionPattern
//			return new HashMap<Image, List<PhotoInteractionPattern>>(0);
//		} else if (SentimentScore.NEGATIVE != null) { 
//			peopleWithProfilePics.clear();
//		}
		
		// try with lived place pattern
		imageWithInteractionPattern = getLivedPlacePicWithPatterns(dialogueContext.processor);
		if (!imageWithInteractionPattern.isEmpty() && sentiment == null) {
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL){
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.NEGATIVE){
			livedPlacesWithImages.clear();
			sentiment = null;
		}
		
		/*if (SentimentScore.POSITIVE != null && SentimentScore.NEUTRAL != null) {
			getMarriagePicWithPatterns(dialogueContext.processor);
			// getNextInteractionPattern
			return new HashMap<Image, List<PhotoInteractionPattern>>(0);
		} else if (SentimentScore.NEGATIVE != null) { 
			livedPlacesWithImages.clear();
		}*/
		
		imageWithInteractionPattern = selectPersonWithProfilePic(dialogueContext.processor, dialogueContext.mentionedPeople);
		if (!imageWithInteractionPattern.isEmpty() && !dialogueContext.prompedAboutPerson && sentiment == null ) {
			String personIntroSentence = MessageFormat.format(getRandomString(introPerson), DefaultFunctions.getPersonNameWithRelationship((Person)(dialogueContext.processor.eval("person")),patient,currentLocale.getLanguage()));
			tts.speakAndWait(personIntroSentence, false, getAbilityTopic());
			dialogueContext.prompedAboutPerson = true;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && (sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL)){
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.NEGATIVE){
			peopleWithProfilePics.clear();
			sentiment = null;
		}
		
		// try with travel place pattern
		imageWithInteractionPattern = getTravelPicWithPatterns(dialogueContext.processor);
		if (!imageWithInteractionPattern.isEmpty() && sentiment == null) {
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL){
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.NEGATIVE){
			travelsWithImages.clear();
			sentiment = null;
		}
		
		imageWithInteractionPattern = selectPersonWithProfilePic(dialogueContext.processor, dialogueContext.mentionedPeople);
		if (!imageWithInteractionPattern.isEmpty() && !dialogueContext.prompedAboutPerson && sentiment == null ) {
			String personIntroSentence = MessageFormat.format(getRandomString(introPerson), DefaultFunctions.getPersonNameWithRelationship((Person)(dialogueContext.processor.eval("person")),patient,currentLocale.getLanguage()));
			tts.speakAndWait(personIntroSentence, false, getAbilityTopic());
			dialogueContext.prompedAboutPerson = true;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && (sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL)){
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.NEGATIVE){
			peopleWithProfilePics.clear();
			sentiment = null;
		}
		
		// try with school pattern
		imageWithInteractionPattern = getSchoolPicWithPatterns(dialogueContext.processor);
		if (!imageWithInteractionPattern.isEmpty() && sentiment == null) {
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && (sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL)){
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.NEGATIVE){
			schoolsWithImages.clear();
			sentiment = null;
		}
		
		imageWithInteractionPattern = selectPersonWithProfilePic(dialogueContext.processor, dialogueContext.mentionedPeople);
		if (!imageWithInteractionPattern.isEmpty() && !dialogueContext.prompedAboutPerson && sentiment == null) {
			String personIntroSentence = MessageFormat.format(getRandomString(introPerson), DefaultFunctions.getPersonNameWithRelationship((Person)(dialogueContext.processor.eval("person")),patient,currentLocale.getLanguage()));
			tts.speakAndWait(personIntroSentence, false, getAbilityTopic());
			dialogueContext.prompedAboutPerson = true;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && (sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL)){
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.NEGATIVE){
			peopleWithProfilePics.clear();
			sentiment = null;
		}
		
		// try with pet pattern
		imageWithInteractionPattern = getPetPicWithPatterns(dialogueContext.processor);
		if (!imageWithInteractionPattern.isEmpty() && sentiment == null) {
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL){
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.NEGATIVE){
			petsWithImages.clear();
			sentiment = null;
		}
		
		imageWithInteractionPattern = selectPersonWithProfilePic(dialogueContext.processor, dialogueContext.mentionedPeople);
		if (!imageWithInteractionPattern.isEmpty() && !dialogueContext.prompedAboutPerson) {
			String personIntroSentence = MessageFormat.format(getRandomString(introPerson), DefaultFunctions.getPersonNameWithRelationship((Person)(dialogueContext.processor.eval("person")),patient,currentLocale.getLanguage()));
			tts.speakAndWait(personIntroSentence, false, getAbilityTopic());
			dialogueContext.prompedAboutPerson = true;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && (sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL)){
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.NEGATIVE){
			peopleWithProfilePics.clear();
			sentiment = null;
		}
		
		// try with job pattern
		imageWithInteractionPattern = getJobPicWithPatterns(dialogueContext.processor);
		if (!imageWithInteractionPattern.isEmpty()) {
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL){
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.NEGATIVE){
			jobsWithImages.clear();
			sentiment = null;
		}
		
		imageWithInteractionPattern = selectPersonWithProfilePic(dialogueContext.processor, dialogueContext.mentionedPeople);
		if (!imageWithInteractionPattern.isEmpty() && !dialogueContext.prompedAboutPerson) {
			String personIntroSentence = MessageFormat.format(getRandomString(introPerson), DefaultFunctions.getPersonNameWithRelationship((Person)(dialogueContext.processor.eval("person")),patient,currentLocale.getLanguage()));
			tts.speakAndWait(personIntroSentence, false, getAbilityTopic());
			dialogueContext.prompedAboutPerson = true;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && (sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL)){
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.NEGATIVE){
			peopleWithProfilePics.clear();
			sentiment = null;
		}
		
		// try with photo
		imageWithInteractionPattern = selectPhoto(dialogueContext.processor);
		if (!imageWithInteractionPattern.isEmpty()) {
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} if (SentimentScore.POSITIVE != null && SentimentScore.NEUTRAL != null) {
			getMarriagePicWithPatterns(dialogueContext.processor);
			// getNextInteractionPattern
			return new HashMap<Image, List<PhotoInteractionPattern>>(0);
		} else if (SentimentScore.NEGATIVE != null) { 
			photos.clear();
			sentiment = null;
		}
		
		imageWithInteractionPattern = selectPersonWithProfilePic(dialogueContext.processor, dialogueContext.mentionedPeople);
		if (!imageWithInteractionPattern.isEmpty()) {
			String personIntroSentence = MessageFormat.format(getRandomString(introPerson), DefaultFunctions.getPersonNameWithRelationship((Person)(dialogueContext.processor.eval("person")),patient,currentLocale.getLanguage()));
			tts.speakAndWait(personIntroSentence, false, getAbilityTopic());
			dialogueContext.prompedAboutPerson = true;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && (sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL)){
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.NEGATIVE){
			peopleWithProfilePics.clear();
			sentiment = null;
		}
		
		// try with person
		imageWithInteractionPattern = selectPersonWithProfilePic(dialogueContext.processor);
		if (!imageWithInteractionPattern.isEmpty()) {
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && (sentiment == SentimentScore.POSITIVE || sentiment == SentimentScore.NEUTRAL)){
			updateMentionedPeople(imageWithInteractionPattern.keySet().iterator().next());
			dialogueContext.totalNumQuestions++;
			dialogueContext.prompedAboutPerson = false;
			return imageWithInteractionPattern;
		} else if (!imageWithInteractionPattern.isEmpty() && sentiment == SentimentScore.NEGATIVE){
			peopleWithProfilePics.clear();
			sentiment = null;
		}
		
		// no image with pattern(s)
		return new HashMap<Image, List<PhotoInteractionPattern>>(0);
	}
	
	
	private void updateMentionedPeople(Image selectedPhoto) {
		List<Person> lastPeopleInPhoto = DefaultFunctions.getPeopleInPhoto(selectedPhoto, patient);
		// add to mentioned people
		for (Person mentionedPerson : lastPeopleInPhoto) {
			dialogueContext.mentionedPeople.put(mentionedPerson.getId(), mentionedPerson);
		}
		System.err.println("MENTIONED PEOPLE: " + dialogueContext.mentionedPeople.size());
	}
	
	
	private void askYesNoModal() {
		ModalButton yesButton = new ModalButton();
		yesButton.setAction(YES_ACTION);
		yesButton.setContext("neutral");
		yesButton.setName(speechBundle.getString("yes-button"));
		ModalButton noButton = new ModalButton();
		noButton.setAction(NO_ACTION);
		noButton.setContext("neutral");
		noButton.setName(speechBundle.getString("no-button"));
		List<ModalButton> buttons = new ArrayList<>(2);
		buttons.add(yesButton);
		buttons.add(noButton);
		String[] texts = loadDataFromBundle("yes-no-modal-texts", false);
		ShowModalMessage message = new ShowModalMessage(MessageFormat.format(speechBundle.getString("yes-no-modal-heading"), userManager.getCurrentUser().getFirstName()), texts[RandomUtils.nextInt(0, texts.length)]);
		message.setButtons(buttons);
		gui.showModal(message, getAbilityTopic());
	}

}
