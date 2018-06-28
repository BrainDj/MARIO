package eu.marioproject.marvin.abilities.reminiscence.el;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.ontologydesignpatterns.ont.mario.action_owl.Agent;
import org.ontologydesignpatterns.ont.mario.healthrole_owl.Patient;
import org.ontologydesignpatterns.ont.mario.multimediacontent_owl.Image;
import org.ontologydesignpatterns.ont.mario.person_owl.Female;
import org.ontologydesignpatterns.ont.mario.person_owl.Male;
import org.ontologydesignpatterns.ont.mario.person_owl.Person;
import org.ontologydesignpatterns.ont.mario.person_owl.jena.PersonJena;
import org.ontologydesignpatterns.ont.mario.personalevents_owl.Employment;
import org.ontologydesignpatterns.ont.mario.personalevents_owl.Marriage;
import org.ontologydesignpatterns.ont.mario.personalevents_owl.PersonalEvent;
import org.ontologydesignpatterns.ont.mario.personalevents_owl.PetOwnership;
import org.ontologydesignpatterns.ont.mario.personalevents_owl.SchoolAttendance;
import org.ontologydesignpatterns.ont.mario.pet_owl.Pet;
import org.ontologydesignpatterns.ont.mario.spatial_owl.City;
import org.ontologydesignpatterns.ont.mario.spatial_owl.SpatialThing;
import org.ontologydesignpatterns.ont.mario.tagging_owl.Tagging;
import org.ontologydesignpatterns.ont.mario.time_owl.Instant;
import org.ontologydesignpatterns.ont.mario.time_owl.TemporalEntity;

import eu.marioproject.marvin.kb.service.GenderService;
import eu.marioproject.marvin.kb.service.PetGenderService;
import it.cnr.istc.stlab.lizard.commons.jena.RuntimeJenaLizardContext;

public final class DefaultFunctions {
	
	private final static Map<String, Map<String, Map<String, String>>> relationships;
	
	private static List<String> photoQualifications;
	
	private static ResourceBundle langBundle;
	
	private final static String[] VOWELS = {"a", "e", "i", "o", "u"};
	
	static {
		relationships = initRelationships();
	}

	private DefaultFunctions() {
	}
	

	public static String getPatientFirstName(Patient patient) {
		return getPersonFirstName(patient);
	}
	
	public static boolean isPersonInPhoto(Image photo, Person person) {
		Model m = RuntimeJenaLizardContext.getContext().getModel();
		ParameterizedSparqlString pss = new ParameterizedSparqlString("ASK {?it ?tp ?p . ?it ?fe ?ph .}");
		pss.setIri("tp", Tagging.TAGGING_TAGGED_PERSON);
		pss.setIri("p", person.getId());
		pss.setIri("fe", Tagging.TAGGING_FOR_ENTITY);
		pss.setIri("ph", photo.getId());
		
		QueryExecution qe = QueryExecutionFactory.create(pss.asQuery(),m);
		boolean result = qe.execAsk();
		
		return result;
	}
	
	public static boolean isPatientInPhoto(Image photo, Patient patient) {
		return isPersonInPhoto(photo, patient);
	}
	
	public static int getNumPeopleInPhoto(Image photo, Patient patient) {
		Model m = RuntimeJenaLizardContext.getContext().getModel();
		ParameterizedSparqlString pss = new ParameterizedSparqlString(
				"SELECT (count(distinct ?imgtag) as ?count)"
				+ "WHERE {"
				+ "?imgtag ?fe ?ph . ?imgtag ?tp ?pers . "
				+ "FILTER NOT EXISTS { ?imgtag ?tp ?p . }"
				+ "}");
		pss.setIri("tp", Tagging.TAGGING_TAGGED_PERSON);
		pss.setIri("p", patient.getId());
		pss.setIri("fe", Tagging.TAGGING_FOR_ENTITY);
		pss.setIri("ph", photo.getId());
		
		QueryExecution qe = QueryExecutionFactory.create(pss.asQuery(),m);
		ResultSet res = qe.execSelect();
		int numPeople = res.next().getLiteral("count").getInt();
		
		qe.close();
		
		return numPeople;
	}
	
	public static String getPeopleNames(Image photo, Patient patient, String lang) {
		Model m = RuntimeJenaLizardContext.getContext().getModel();
		ParameterizedSparqlString pss = new ParameterizedSparqlString(
				"SELECT ?fname"
				+ " WHERE "
				+ "{"
				+ "?imgtag ?fe ?ph ."
				+ "?imgtag ?tp ?p ."
				+ "?p ?firstname ?fname ."
				+ "FILTER NOT EXISTS { ?imgtag ?tp ?pat . }"
				+ "}");
		pss.setIri("tp", Tagging.TAGGING_TAGGED_PERSON);
		pss.setIri("pat", patient.getId());
		pss.setIri("fe", Tagging.TAGGING_FOR_ENTITY);
		pss.setIri("ph", photo.getId());
		pss.setIri("firstname", Person.PERSON_FIRST_NAME);
		
		QueryExecution qe = QueryExecutionFactory.create(pss.asQuery(),m);
		ResultSet res = qe.execSelect();
		
		List<String> elements = new ArrayList<>();
		while (res.hasNext()) {
			elements.add(res.next().getLiteral("fname").getString());
		}
		
		qe.close();
		
		return concatElems(elements, lang);
	}
	
	public static String getPeopleNamesWithRelationship(Image photo, Patient patient, String lang) {
		Model m = RuntimeJenaLizardContext.getContext().getModel();
		ParameterizedSparqlString pss = new ParameterizedSparqlString(
				"SELECT ?fname ?rel ?gen"
				+ " WHERE "
				+ "{"
				+ "?imgtag ?fe ?ph ."
				+ "?imgtag ?tp ?p ."
				+ "?p ?genere ?gen ."
				+ "?p ?firstname ?fname ."
				+ "?p ?rel ?pat ."
				+ "FILTER NOT EXISTS { ?imgtag ?tp ?pat . }"
				+ "}");
		pss.setIri("tp", Tagging.TAGGING_TAGGED_PERSON);
		pss.setIri("pat", patient.getId());
		pss.setIri("fe", Tagging.TAGGING_FOR_ENTITY);
		pss.setIri("ph", photo.getId());
		pss.setIri("firstname", Person.PERSON_FIRST_NAME);
		pss.setIri("genere", Person.PERSON_HAS_GENDER);
		QueryExecution qe = QueryExecutionFactory.create(pss.asQuery(),m);
		ResultSet res = qe.execSelect();
		
		QuerySolution solution;
		String personName, relationship;
		List<String> elements = new ArrayList<>();
		
		while (res.hasNext()) {
			solution = res.next();
			personName = solution.getLiteral("fname").getString();
			relationship = relationships.get(solution.getResource("rel").getURI()).get(lang).get(solution.getResource("gen").getLocalName());
			elements.add(relationship + " " + personName);
		}
		
		qe.close();
		
		return concatElems(elements, lang);
	}
	
	public static String getPhotoDate(Image photo) {
		return "";
	}
	
	public static Person getPersonInPhoto(Image photo, Patient patient) {
		Model m = RuntimeJenaLizardContext.getContext().getModel();
		ParameterizedSparqlString pss = new ParameterizedSparqlString(
				"SELECT ?p"
				+ " WHERE "
				+ "{"
				+ "?imgtag ?fe ?ph ."
				+ "?imgtag ?tp ?p ."
				+ "FILTER NOT EXISTS { ?imgtag ?tp ?pat . }"
				+ "}");
		pss.setIri("tp", Tagging.TAGGING_TAGGED_PERSON);
		pss.setIri("pat", patient.getId());
		pss.setIri("fe", Tagging.TAGGING_FOR_ENTITY);
		pss.setIri("ph", photo.getId());
		
		QueryExecution qe = QueryExecutionFactory.create(pss.asQuery(),m);
		ResultSet res = qe.execSelect();
		
		List<String> elements = new ArrayList<>();
		
		while (res.hasNext()) {
			elements.add(res.next().getResource("p").getURI());
		}
		
		String selectedURI = elements.get(RandomUtils.nextInt(0, elements.size()));
		
		qe.close();
		
		return ((PersonJena)Person.get(selectedURI)).asMicroBean();
	}
	
	
	public static String getPersonFirstName(Person person) {
		return getFirstName(person);
	}
	
	public static String getPersonNameWithRelationship(Person person, Patient patient, String lang) {
		Model m = RuntimeJenaLizardContext.getContext().getModel();
		ParameterizedSparqlString pss = new ParameterizedSparqlString(
				"SELECT ?fname ?rel ?gen"
				+ " WHERE "
				+ "{"
				+ "?p ?genere ?gen ."
				+ "?p ?firstname ?fname ."
				+ "?p ?rel ?pat ."
				+ "}");
		pss.setIri("p", person.getId());
		pss.setIri("pat", patient.getId());
		pss.setIri("firstname", Person.PERSON_FIRST_NAME);
		pss.setIri("genere", Person.PERSON_HAS_GENDER);
		QueryExecution qe = QueryExecutionFactory.create(pss.asQuery(),m);
		ResultSet res = qe.execSelect();
		
		QuerySolution solution;
		String personName, relationship;
		
		String result = "";
		
		while (res.hasNext()) {
			solution = res.next();
			personName = solution.getLiteral("fname").getString();
			relationship = relationships.get(solution.getResource("rel").getURI()).get(lang).get(solution.getResource("gen").getLocalName());
			result += relationship + " " + personName;
		}
		
		qe.close();
		
		return result;
	}
	
	public static List<Person> getPeopleInPhoto(Image photo, Patient patient) {
		
		Model m = RuntimeJenaLizardContext.getContext().getModel();
		ParameterizedSparqlString pss = new ParameterizedSparqlString(
				"SELECT ?p"
				+ " WHERE "
				+ "{"
				+ "?imgtag ?fe ?ph ."
				+ "?imgtag ?tp ?p ."
				+ "FILTER NOT EXISTS { ?imgtag ?tp ?pat . }"
				+ "}");
		pss.setIri("tp", Tagging.TAGGING_TAGGED_PERSON);
		pss.setIri("pat", patient.getId());
		pss.setIri("fe", Tagging.TAGGING_FOR_ENTITY);
		pss.setIri("ph", photo.getId());
		QueryExecution qe = QueryExecutionFactory.create(pss.asQuery(),m);
		ResultSet res = qe.execSelect();
		List<Person> people = new ArrayList<>();
		while (res.hasNext()) {
			people.add(new PersonJena(res.next().get("p")).asMicroBean());// Person.get(res.next().getResource("p").getURI()));
		}
		
		qe.close();
		
		return people;
	}
	
	public static String getNamesForPeople(List<Person> people, String lang) {
		List<String> elements = new ArrayList<>();
		for (Person person : people) {
			elements.add(getPersonFirstName(person));
		}
		return concatElems(elements, lang);
	}
	
	public static List<Person> unmentionedPeople(List<Person> allPeople, List<Person> mentionedPeople) {
		/*
		 * Need to compare the IDs because the elems in the lists may not be comparable according to equals,
		 * as they are retrieved in different calls
		 */
		List<Person> unmentionedPeople = new ArrayList<>(allPeople);
		
		for (Person person : allPeople) {
			for (Person mentionedPerson : mentionedPeople) {
				if (person.getId().equals(mentionedPerson.getId())) {
					unmentionedPeople.remove(person);
				}
			}
		}
		return unmentionedPeople;
		//return new ArrayList<>(CollectionUtils.subtract(allPeople, mentionedPeople));
	}
	
	public static String getNamesAndRelationshipsForPeople(List<Person> people, Patient patient, String lang) {
		List<String> peopleIds = new ArrayList<>(people.size());
		for (Person person : people) {
			peopleIds.add("<"+person.getId()+">");
		}
		String ids = StringUtils.join(peopleIds, ",");
		Model m = RuntimeJenaLizardContext.getContext().getModel();
		ParameterizedSparqlString pss = new ParameterizedSparqlString(
				"SELECT ?fname ?rel ?gen"
				+ " WHERE "
				+ "{"
				+ "?p ?genere ?gen ."
				+ "?p ?firstname ?fname ."
				+ "?p ?rel ?pat ."
				+ " FILTER(?p IN ("+ids+"))"
				//+ " VALUES ?p { "+ ids +" }"
				+ "}");
		pss.setIri("pat", patient.getId());
		pss.setIri("firstname", Person.PERSON_FIRST_NAME);
		pss.setIri("genere", Person.PERSON_HAS_GENDER);
		//System.out.println(pss.asQuery().toString(Syntax.syntaxSPARQL_11));
		QueryExecution qe = QueryExecutionFactory.create(pss.asQuery(),m);
		ResultSet res = qe.execSelect();
		
		QuerySolution solution;
		String personName, relationship;
		List<String> elements = new ArrayList<>();
		
		while (res.hasNext()) {
			solution = res.next();
			personName = solution.getLiteral("fname").getString();
			//System.err.println(solution);
			relationship = relationships.get(solution.getResource("rel").getURI()).get(lang).get(solution.getResource("gen").getLocalName());
			elements.add(relationship + " " + personName);
		}
		
		qe.close();
		
		return concatElems(elements, lang);
	}
	
	public static List<Person> getSomePeopleInPhoto(Image photo, Patient patient) {
		//System.out.println("IN: getSomePeopleInPhoto");
		List<Person> allPeopleInPhoto = getPeopleInPhoto(photo, patient);
		//System.out.println("ALL PEOPLE SIZE: " + allPeopleInPhoto.size());
		//System.out.println("SUBLISTING: ");
		
		/* Temporarily make this deterministic to return same elements...
		 * EL variables bind to function call lead the function to be called
		 * each time the variable is evaluated
		 */
		
		if (allPeopleInPhoto.size() == 2) {
			// DO NOT RETURN DIRECTLY THE RESULT OF SUBLIST, which is an inner class of ArrayList; EL calls (e.g., size()) over it will fail!! 
			return new ArrayList<>(allPeopleInPhoto.subList(0, 1)) ;
		}
		
		return new ArrayList<>(allPeopleInPhoto.subList(0, allPeopleInPhoto.size()-2));
		
		// two people in the photo: randomly get one of them
		/*if (allPeopleInPhoto.size() == 2) {
			int selectedElemIndex = RandomUtils.nextInt(0, 2);
			return allPeopleInPhoto.subList(selectedElemIndex, selectedElemIndex+1);
		}
		Collections.shuffle(allPeopleInPhoto);
		return allPeopleInPhoto.subList(0, allPeopleInPhoto.size()-2);*/
	}
	
	
	public static String qualifyPhoto(String lang) {
		if (photoQualifications == null) {
			ResourceBundle langBundle = getLangBundle(lang);
			String qualificationsString = langBundle.getString("photo-qualifications");
			String[] qualifications = qualificationsString.split("\\|");
			photoQualifications = new ArrayList<>(qualifications.length+1);
			for (String qualification : qualifications) {
				photoQualifications.add(qualification.trim().toLowerCase());
			}
			photoQualifications.add(StringUtils.EMPTY);
		}
		return photoQualifications.get(RandomUtils.nextInt(0, photoQualifications.size()-1));
	}
	
	public static String fullyQualifyPhoto(String lang) {
		String photoQualification = qualifyPhoto(lang);
		if (StringUtils.isBlank(photoQualification)) {
			if (lang.equalsIgnoreCase("en")) {
				return "a";
			}
			else if (lang.equalsIgnoreCase("it")) {
				return "una";
			}
		}
		else {
			if (lang.equalsIgnoreCase("en")) {
				if (StringUtils.startsWithAny(photoQualification, VOWELS)) {
					return "an " + photoQualification;
				}
				else {
					return "a " + photoQualification;
				}
			}
			else if (lang.equalsIgnoreCase("it")) {
				if (StringUtils.startsWithAny(photoQualification, VOWELS)) {
					return "un'" + photoQualification;
				}
				else {
					return "una " + photoQualification;
				}
			}
		}
		return "";
	}
	
	public static City getBirthPlace(Person person) {
		City birthCity = person.getPerson_hasBirthPlace().isEmpty() ? null : person.getPerson_hasBirthPlace().iterator().next();
		if (birthCity != null) {
			return City.get(birthCity.getId());
		}
		return null;
	}
	
	public static String getBirthPlaceName(Person person) {
		City birthCity = person.getPerson_hasBirthPlace().isEmpty() ? null : person.getPerson_hasBirthPlace().iterator().next();
		if (birthCity != null) {
			return City.get(birthCity.getId()).getGen_name().iterator().next();
		}
		return null;
	}
	
	public static City getCurrentCity(Person person) {
		City currentCity = person.getPerson_livesIn().isEmpty() ? null : City.get(person.getPerson_livesIn().iterator().next().getId());
		if (currentCity != null) {
			return City.get(currentCity.getId());
		}
		return null;
	}
	
	public static String getCurrentCityName(Person person) {
		City currentCity = getCurrentCity(person);
		if (currentCity != null) {
			return currentCity.getGen_name().iterator().next();
		}
		return null;
	}
	
	
	public static City getHometown(Person person) {
		City hometown = person.getPerson_hasHometown().isEmpty() ? null : City.get(person.getPerson_hasHometown().iterator().next().getId());
		if (hometown != null) {
			return City.get(hometown.getId());
		}
		return null;
	}
	
	public static String getHometownName(Person person) {
		City hometown = getHometown(person);
		if (hometown != null) {
			return hometown.getGen_name().iterator().next();
		}
		return null;
	}
	
	
	public static Date getPersonBirthdate(Person person) {
		if (person.getPerson_birthDate().isEmpty()) {
			return null;
		}
		XSDDateTime birthdate = person.getPerson_birthDate().iterator().next();
		return birthdate.asCalendar().getTime();
		//LocalDate.of(calendarDate.get(Calendar.YEAR), calendarDate.get(Calendar.MONTH), calendarDate.get(Calendar.DAY_OF_MONTH));
	}
	
	public static String getPersonBirthday(Person person, String lang) {
		Date birthdate = getPersonBirthdate(person);
		if (birthdate == null) {
			return "";
		}
		LocalDate localDate = birthdate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		int day = localDate.getDayOfMonth();
		if (lang.equalsIgnoreCase("en")) {
			String ordinalSuffix = getOrdinalDayOfMonth(day);
			return day + ordinalSuffix + " of " + localDate.getMonth().getDisplayName(TextStyle.FULL, Locale.UK) + " " + localDate.getYear();
		}
		else if (lang.equalsIgnoreCase("it")) {
			String dayString;
			if (day == 1) {
				dayString = "primo";
			}
			else {
				dayString = localDate.getDayOfMonth()+"";
			}
			return dayString + " " + localDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ITALIAN) + " " + localDate.getYear();
		}
		return "";
	}
	
	public static boolean isMale(Person person) {
		return person.getPerson_hasGender().iterator().next().getId().equals(GenderService.MALE);
	}
	
	public static boolean isPetMale(Pet pet) {
		return pet.getPet_hasPetGender().iterator().next().getId().equals(PetGenderService.MALE_PET);
	}
	
	public static City getPhotoLocation(Image photo) {
		return photo.getSpa_hasPlace().isEmpty() ? null : City.get(photo.getSpa_hasPlace().iterator().next().getId());
	}
	
	public static String getPhotoLocationName(Image photo) {
		City photoLocation = getPhotoLocation(photo);
		if (photoLocation != null) {
			return photoLocation.getGen_name().iterator().next();
		}
		return null;
	}
	
	public static boolean isChildOfPatient(Person person, Patient patient) {
		if (isMale(person)) {
			Male male = Male.get(person.getId());
			Set<Person> sonOf =  male.getPerson_sonOf();
			if (!sonOf.isEmpty()) {
				return sonOf.iterator().next().getId().equals(patient.getId());
			}
		}
		else {
			Female female = Female.get(person.getId());
			Set<Person> daughterOf =  female.getPerson_daughterOf();
			if (!daughterOf.isEmpty()) {
				return daughterOf.iterator().next().getId().equals(patient.getId());
			}
		}
		return false;
	}
	
	public static boolean isSiblingOfPatient(Person person, Patient patient) {
		if (isMale(person)) {
			Male male = Male.get(person.getId());
			Set<Person> brotherOf =  male.getPerson_brotherOf();
			if (!brotherOf.isEmpty()) {
				return brotherOf.iterator().next().getId().equals(patient.getId());
			}
		}
		else {
			Female female = Female.get(person.getId());
			Set<Person> sisterOf =  female.getPerson_sisterOf();
			if (!sisterOf.isEmpty()) {
				return sisterOf.iterator().next().getId().equals(patient.getId());
			}
		}
		return false;
	}
	
	public static Person getMarriagePartner(Patient patient, Marriage marriage) {
		Set<Person> partners = marriage.getPersonalevents_hasSpouse();
		for (Person partner : partners) {
			if (! partner.getId().equals(patient.getId())) {
				return Person.get(partner.getId());
			}
		}
		return null;
	}
	
	public static Date getMarriageDate(Marriage marriage) {
		Set<TemporalEntity> temporalEntities = marriage.getTime_atTime();
		TemporalEntity temporalEntity = temporalEntities.isEmpty() ? null : temporalEntities.iterator().next();
		if (temporalEntity != null) {
			Instant instant = Instant.get(temporalEntity.getId());
			return instant.getTime_inXSDDateTime().iterator().next().asCalendar().getTime();
		}
		return null;
	}
	
	public static String getDateText(Date date, String lang) {
		LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		int day = localDate.getDayOfMonth();
		if (lang.equalsIgnoreCase("en")) {
			String ordinalSuffix = getOrdinalDayOfMonth(day);
			return day + ordinalSuffix + " of " + localDate.getMonth().getDisplayName(TextStyle.FULL, Locale.UK) + " " + localDate.getYear();
		}
		else if (lang.equalsIgnoreCase("it")) {
			String dayString;
			if (day == 1) {
				dayString = "primo";
			}
			else {
				dayString = localDate.getDayOfMonth()+"";
			}
			return dayString + " " + localDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ITALIAN) + " " + localDate.getYear();
		}
		return "";
	}
	
	public static boolean nobodyInPhoto(Image photo) {
		
		Model m = RuntimeJenaLizardContext.getContext().getModel();
		ParameterizedSparqlString pss = new ParameterizedSparqlString("ASK { ?photo a ?photoClass . "
				+ "FILTER NOT EXISTS { ?imagetagging ?forentity ?photo . ?imagetagging ?taggedPerson ?person . } }");
		pss.setIri("photo", photo.getId());
		pss.setIri("photoClass", Image.CLASS_IRI);
		pss.setIri("forentity", Tagging.TAGGING_FOR_ENTITY);
		pss.setIri("taggedPerson", Tagging.TAGGING_TAGGED_PERSON);
				
		QueryExecution qe = QueryExecutionFactory.create(pss.asQuery(),m);
		boolean result = qe.execAsk();
		
		qe.close();
		
		return result;
		
	}
	
	public static City getEventPlace(PersonalEvent event) {
		Set<SpatialThing> places  = event.getSpa_hasPlace();
		SpatialThing place = places.isEmpty() ? null : places.iterator().next();
		if (place != null) {
			return City.get(place.getId());
		}
		return null;
	}
	
	public static String getCityName(City city) {
		if (city == null) {
			return null;
		}
		return city.getGen_name().iterator().next();
	}
	
	public static String getYear(Date date) {
		LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		return localDate.getYear()+"";
	}
	
	public static List<Person> getEventParticipants(PersonalEvent event) {
		Set<Agent> agents = event.getAction_hasParticipant();
		if (agents.isEmpty()) {
			return new ArrayList<>(0);
		}
		List<Person> participants = new ArrayList<>();
		for (Agent agent : agents) {
			Person person = Person.get(agent.getId());
			if (person != null) {
				participants.add(person);
			}
		}
		return participants;
	}
	
	public static int numEventParticipants(PersonalEvent event) {
		return getEventParticipants(event).size();
	}
	
	public static Pet getPet(PetOwnership event) {
		return event.getPersonalevents_ownedPet().iterator().next();
	}
	
	public static String getPetName(PetOwnership event) {
		return event.getPersonalevents_ownedPet().iterator().next().getGen_name().iterator().next();
	}
	
	public static String getPetType(PetOwnership event) {
		return event.getPersonalevents_ownedPet().iterator().next().getPet_hasPetType().iterator().next().getGen_name().iterator().next();
	}
	
	public static boolean isPetInPhoto(Pet pet, Image photo) {
		Model m = RuntimeJenaLizardContext.getContext().getModel();
		ParameterizedSparqlString pss = new ParameterizedSparqlString("ASK {?it ?usingtag ?pet . ?it ?fe ?ph .}");
		pss.setIri("usingtag", Tagging.TAGGING_USING_TAG);
		pss.setIri("pet", pet.getId());
		pss.setIri("fe", Tagging.TAGGING_FOR_ENTITY);
		pss.setIri("ph", photo.getId());
		
		QueryExecution qe = QueryExecutionFactory.create(pss.asQuery(),m);
		boolean result = qe.execAsk();
		
		qe.close();
		
		return result;
	}
	
	public static String getJobPosition(Employment employmentEvent) {
		return employmentEvent.getPersonalevents_hasPosition().iterator().next().getGen_name().iterator().next();
	}
	
	public static String getEmployerName(Employment employmentEvent) {
		Set<Agent> employers = employmentEvent.getPersonalevents_hasEmployer();
		if (employers.isEmpty()) {
			return null;
		}
		return employers.iterator().next().getGen_name().iterator().next();
	}
	
	public static String getSchoolName(SchoolAttendance schoolEvent) {
		return schoolEvent.getPersonalevents_hasSchool().iterator().next().getGen_name().iterator().next();
	}
	
	private static String getFirstName(Person person) {
		return CollectionUtils.extractSingleton(person.getPerson_firstName());
	}
	
	private static Map<String, Map<String, Map<String, String>>> initRelationships() {
		
		Map<String, Map<String, Map<String, String>>> rels = new HashMap<String, Map<String,Map<String,String>>>();
		
		Map<String, String> closeFriendEn = new HashMap<>();
		closeFriendEn.put("Male", "your close friend");
		closeFriendEn.put("Female", "your close friend");
		
		Map<String, String> closeFriendIt = new HashMap<>();
		closeFriendIt.put("Male", "il tuo caro amico");
		closeFriendIt.put("Female", "la tua cara amica");
		
		Map<String, Map<String, String>> closeFriendLang = new HashMap<>();
		closeFriendLang.put("en", closeFriendEn);
		closeFriendLang.put("it", closeFriendIt);
		
		rels.put(Person.PERSON_CLOSE_FRIEND_OF, closeFriendLang);
		
		
		Map<String, String> colleagueEn = new HashMap<>();
		colleagueEn.put("Male", "your colleague");
		colleagueEn.put("Female", "your colleague");
		
		Map<String, String> colleagueIt = new HashMap<>();
		colleagueIt.put("Male", "il tuo collega");
		colleagueIt.put("Female", "la tua collega");
		
		Map<String, Map<String, String>> colleagueLang = new HashMap<>();
		colleagueLang.put("en", colleagueEn);
		colleagueLang.put("it", colleagueIt);
		
		rels.put(Person.PERSON_COLLEAGUE_OF, colleagueLang);
		
		
		// TODO: engaged and others?
		// temporary impl for engaged
		Map<String, String> engagedEn = new HashMap<>();
		engagedEn.put("Male", "your partner");
		engagedEn.put("Female", "your partner");
		
		Map<String, String> engagedIt = new HashMap<>();
		engagedIt.put("Male", "il tuo compagno");
		engagedIt.put("Female", "la tua compagna");
		
		Map<String, Map<String, String>> engagedLang = new HashMap<>();
		engagedLang.put("en", engagedEn);
		engagedLang.put("it", engagedIt);
		
		rels.put(Person.PERSON_ENGAGED_TO, engagedLang);
		
		
		Map<String, String> friendEn = new HashMap<>();
		friendEn.put("Male", "your friend");
		friendEn.put("Female", "your friend");
		
		Map<String, String> friendIt = new HashMap<>();
		friendIt.put("Male", "il tuo amico");
		friendIt.put("Female", "la tua amica");
		
		Map<String, Map<String, String>> friendLang = new HashMap<>();
		friendLang.put("en", friendEn);
		friendLang.put("it", friendIt);
		
		rels.put(Person.PERSON_FRIEND_OF, friendLang);
		
		
		Map<String, String> lifePartnerEn = new HashMap<>();
		lifePartnerEn.put("Male", "your partner");
		lifePartnerEn.put("Female", "your partner");
		
		Map<String, String> lifePartnerIt = new HashMap<>();
		lifePartnerIt.put("Male", "il tuo compagno");
		lifePartnerIt.put("Female", "la tua compagna");
		
		Map<String, Map<String, String>> lifePartnerLang = new HashMap<>();
		lifePartnerLang.put("en", lifePartnerEn);
		lifePartnerLang.put("it", lifePartnerIt);
		
		rels.put(Person.PERSON_LIFE_PARTNER_OF, lifePartnerLang);
		
		
		Map<String, String> livesWithEn = new HashMap<>();
		livesWithEn.put("Male", "your housemate");
		livesWithEn.put("Female", "your housemate");
		
		Map<String, String> livesWithIt = new HashMap<>();
		livesWithIt.put("Male", "il tuo coinquilino");
		livesWithIt.put("Female", "la tua coinquilina");
		
		Map<String, Map<String, String>> livesWithLang = new HashMap<>();
		livesWithLang.put("en", livesWithEn);
		livesWithLang.put("it", livesWithIt);
		
		rels.put(Person.PERSON_LIVES_WITH, livesWithLang);
		
		
		Map<String, String> worksWithEn = new HashMap<>();
		worksWithEn.put("Male", "your co-worker");
		worksWithEn.put("Female", "your co-worker");
		
		Map<String, String> worksWithIt = new HashMap<>();
		worksWithIt.put("Male", "il tuo collega");
		worksWithIt.put("Female", "la tua collega");
		
		Map<String, Map<String, String>> worksWithLang = new HashMap<>();
		worksWithLang.put("en", worksWithEn);
		worksWithLang.put("it", worksWithIt);
		
		rels.put(Person.PERSON_WORKS_WITH, worksWithLang);
		
		
		Map<String, String> cousinEn = new HashMap<>();
		cousinEn.put("Male", "your cousin");
		cousinEn.put("Female", "your cousin");
		
		Map<String, String> cousinIt = new HashMap<>();
		cousinIt.put("Male", "tuo cugino");
		cousinIt.put("Female", "tua cugina");
		
		Map<String, Map<String, String>> cousinLang = new HashMap<>();
		cousinLang.put("en", cousinEn);
		cousinLang.put("it", cousinIt);
		
		rels.put(Person.PERSON_COUSIN_OF, cousinLang);
		
		/*** MALE ONLY ***/
		
		Map<String, String> brotherEn = new HashMap<>();
		brotherEn.put("Male", "your brother");
		
		Map<String, String> brotherIt = new HashMap<>();
		brotherIt.put("Male", "tuo fratello");
		
		Map<String, Map<String, String>> brotherLang = new HashMap<>();
		brotherLang.put("en", brotherEn);
		brotherLang.put("it", brotherIt);
		
		rels.put(Male.PERSON_BROTHER_OF, brotherLang);
		
		
		Map<String, String> fatherEn = new HashMap<>();
		fatherEn.put("Male", "your father");
		
		Map<String, String> fatherIt = new HashMap<>();
		fatherIt.put("Male", "tuo padre");
		
		Map<String, Map<String, String>> fatherLang = new HashMap<>();
		fatherLang.put("en", fatherEn);
		fatherLang.put("it", fatherIt);
		
		rels.put(Male.PERSON_FATHER_OF, fatherLang);
		
		
		Map<String, String> grandfatherEn = new HashMap<>();
		grandfatherEn.put("Male", "your grandfather");
		
		Map<String, String> grandfatherIt = new HashMap<>();
		grandfatherIt.put("Male", "tuo nonno");
		
		Map<String, Map<String, String>> grandfatherLang = new HashMap<>();
		grandfatherLang.put("en", grandfatherEn);
		grandfatherLang.put("it", grandfatherIt);
		
		
		Map<String, String> grandsonEn = new HashMap<>();
		grandsonEn.put("Male", "your grandson");
		
		Map<String, String> grandsonIt = new HashMap<>();
		grandsonIt.put("Male", "tuo nipote");
		
		Map<String, Map<String, String>> grandsonLang = new HashMap<>();
		grandsonLang.put("en", grandsonEn);
		grandsonLang.put("it", grandsonIt);
		
		rels.put(Male.PERSON_GRANDSON_OF, grandsonLang);
		
		
		Map<String, String> husbandEn = new HashMap<>();
		husbandEn.put("Male", "your husband");
		
		Map<String, String> husbandIt = new HashMap<>();
		husbandIt.put("Male", "tuo marito");
		
		Map<String, Map<String, String>> husbandLang = new HashMap<>();
		husbandLang.put("en", husbandEn);
		husbandLang.put("it", husbandIt);
		
		rels.put(Male.PERSON_HUSBAND_OF, husbandLang);
		
		
		Map<String, String> sonEn = new HashMap<>();
		sonEn.put("Male", "your son");
		
		Map<String, String> sonIt = new HashMap<>();
		sonIt.put("Male", "tuo figlio");
		
		Map<String, Map<String, String>> sonLang = new HashMap<>();
		sonLang.put("en", sonEn);
		sonLang.put("it", sonIt);
		
		rels.put(Male.PERSON_SON_OF, sonLang);
		
		
		Map<String, String> uncleEn = new HashMap<>();
		uncleEn.put("Male", "your uncle");
		
		Map<String, String> uncleIt = new HashMap<>();
		uncleIt.put("Male", "tuo zio");
		
		Map<String, Map<String, String>> uncleLang = new HashMap<>();
		uncleLang.put("en", uncleEn);
		uncleLang.put("it", uncleIt);
		
		rels.put(Male.PERSON_UNCLE_OF, uncleLang);
		
		
		Map<String, String> nephewEn = new HashMap<>();
		nephewEn.put("Male", "your nephew");
		
		Map<String, String> nephewIt = new HashMap<>();
		nephewIt.put("Male", "tuo nipote");
		
		Map<String, Map<String, String>> nephewLang = new HashMap<>();
		nephewLang.put("en", nephewEn);
		nephewLang.put("it", nephewIt);
		
		rels.put(Male.PERSON_NEPHEW_OF, nephewLang);
		
		
		Map<String, String> fatherInLawEn = new HashMap<>();
		fatherInLawEn.put("Male", "your father-in-law");
		
		Map<String, String> fatherInLawIt = new HashMap<>();
		fatherInLawIt.put("Male", "tuo suocero");
		
		Map<String, Map<String, String>> fatherInLawLang = new HashMap<>();
		fatherInLawLang.put("en", fatherInLawEn);
		fatherInLawLang.put("it", fatherInLawIt);
		
		rels.put(Male.PERSON_FATHER_IN_LAW, fatherInLawLang);
		
		
		Map<String, String> sonInLawEn = new HashMap<>();
		sonInLawEn.put("Male", "your son-in-law");
		
		Map<String, String> sonInLawIt = new HashMap<>();
		sonInLawIt.put("Male", "tuo genero");
		
		Map<String, Map<String, String>> sonInLawLang = new HashMap<>();
		sonInLawLang.put("en", sonInLawEn);
		sonInLawLang.put("it", sonInLawIt);
		
		rels.put(Male.PERSON_SON_IN_LAW_OF, sonInLawLang);
		
		
		/*** FEMALE ONLY ***/
		
		Map<String, String> daughterEn = new HashMap<>();
		daughterEn.put("Female", "your daughter");
		
		Map<String, String> daughterIt = new HashMap<>();
		daughterIt.put("Female", "tua figlia");
		
		Map<String, Map<String, String>> dautherLang = new HashMap<>();
		dautherLang.put("en", daughterEn);
		dautherLang.put("it", daughterIt);
		
		rels.put(Female.PERSON_DAUGHTER_OF, dautherLang);
		
		
		Map<String, String> granddaughterEn = new HashMap<>();
		granddaughterEn.put("Female", "your granddaughter");
		
		Map<String, String> granddaughterIt = new HashMap<>();
		granddaughterIt.put("Female", "tua nipote");
		
		Map<String, Map<String, String>> granddautherLang = new HashMap<>();
		granddautherLang.put("en", granddaughterEn);
		granddautherLang.put("it", granddaughterIt);
		
		rels.put(Female.PERSON_GRANDDAUGHTER_OF, granddautherLang);
		
		
		Map<String, String> grandmotherEn = new HashMap<>();
		grandmotherEn.put("Female", "your grandmother");
		
		Map<String, String> grandmotherIt = new HashMap<>();
		grandmotherIt.put("Female", "tua nonna");
		
		Map<String, Map<String, String>> grandmotherLang = new HashMap<>();
		grandmotherLang.put("en", grandmotherEn);
		grandmotherLang.put("it", grandmotherIt);
		
		rels.put(Female.PERSON_GRANDMOTHER_OF, grandmotherLang);
		
		
		Map<String, String> motherEn = new HashMap<>();
		motherEn.put("Female", "your mother");
		
		Map<String, String> motherIt = new HashMap<>();
		motherIt.put("Female", "tua madre");
		
		Map<String, Map<String, String>> motherLang = new HashMap<>();
		motherLang.put("en", motherEn);
		motherLang.put("it", motherIt);
		
		rels.put(Female.PERSON_MOTHER_OF, motherLang);
		
		
		Map<String, String> sisterEn = new HashMap<>();
		sisterEn.put("Female", "your sister");
		
		Map<String, String> sisterIt = new HashMap<>();
		sisterIt.put("Female", "tua sorella");
		
		Map<String, Map<String, String>> sisterLang = new HashMap<>();
		sisterLang.put("en", sisterEn);
		sisterLang.put("it", sisterIt);
		
		rels.put(Female.PERSON_SISTER_OF, sisterLang);
		
		
		Map<String, String> wifeEn = new HashMap<>();
		wifeEn.put("Female", "your wife");
		
		Map<String, String> wifeIt = new HashMap<>();
		wifeIt.put("Female", "tua moglie");
		
		Map<String, Map<String, String>> wifeLang = new HashMap<>();
		wifeLang.put("en", wifeEn);
		wifeLang.put("it", wifeIt);
		
		rels.put(Female.PERSON_WIFE_OF, wifeLang);
		
		
		Map<String, String> auntEn = new HashMap<>();
		auntEn.put("Female", "your aunt");
		
		Map<String, String> auntIt = new HashMap<>();
		auntIt.put("Female", "tua zia");
		
		Map<String, Map<String, String>> auntLang = new HashMap<>();
		auntLang.put("en", auntEn);
		auntLang.put("it", auntIt);
		
		rels.put(Female.PERSON_AUNT_OF, auntLang);
		
		
		Map<String, String> nieceEn = new HashMap<>();
		nieceEn.put("Female", "your niece");
		
		Map<String, String> nieceIt = new HashMap<>();
		auntIt.put("Female", "tua nipote");
		
		Map<String, Map<String, String>> nieceLang = new HashMap<>();
		nieceLang.put("en", nieceEn);
		nieceLang.put("it", nieceIt);
		
		rels.put(Female.PERSON_NIECE_OF, nieceLang);
		
		
		Map<String, String> motherInLawEn = new HashMap<>();
		motherInLawEn.put("Female", "your mother-in-law");
		
		Map<String, String> motherInLawIt = new HashMap<>();
		motherInLawIt.put("Female", "tua suocera");
		
		Map<String, Map<String, String>> motherInLawLang = new HashMap<>();
		motherInLawLang.put("en", motherInLawEn);
		motherInLawLang.put("it", motherInLawIt);
		
		rels.put(Female.PERSON_MOTHER_IN_LAW, motherInLawLang);
		
		
		Map<String, String> daughterInLawEn = new HashMap<>();
		daughterInLawEn.put("Female", "your daughter-in-law");
		
		Map<String, String> daughterInLawIt = new HashMap<>();
		daughterInLawIt.put("Female", "tua nuora");
		
		Map<String, Map<String, String>> daughterInLawLang = new HashMap<>();
		daughterInLawLang.put("en", daughterInLawEn);
		daughterInLawLang.put("it", daughterInLawIt);
		
		rels.put(Female.PERSON_DAUGHTER_IN_LAW_OF, daughterInLawLang);
		
		
		return Collections.unmodifiableMap(rels);
		
	}
	
	private static ResourceBundle getLangBundle(String lang) {
		if (langBundle == null) {
			try {
	    		File file = new File("mario"+File.separator+"lang-bundles");
	        	URL theUrl = null;
				theUrl = file.toURI().toURL();
				URL[] urls = {theUrl};
		    	ClassLoader loader = new URLClassLoader(urls);
		    	langBundle = ResourceBundle.getBundle("Reminiscence", Locale.forLanguageTag(lang), loader, new UTF8Control());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return langBundle;
	}
	
	private static String concatElems(List<String> elements, String lang) {
		StringBuilder builder = new StringBuilder();
		int numElems = elements.size();
		
		for (int i = 0; i < numElems; i++) {
			
			builder.append(elements.get(i));
			if (i == numElems-2) {
				builder.append(" ");
				builder.append(getLangBundle(lang).getString("and"));
				builder.append(" ");
			}
			else if (i != numElems-1) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}
	
	private static String getOrdinalDayOfMonth(int day) {
		if (day >= 11 && day <= 13) {
			return "th";
	    }
		else {
		    switch (day % 10) {
		    case 1:
		    	return "st";
		    case 2:
		    	return "nd";
		    case 3:
		    	return "rd";
		    default:
		    	return "th";
		    }
		}
	}
	
}
