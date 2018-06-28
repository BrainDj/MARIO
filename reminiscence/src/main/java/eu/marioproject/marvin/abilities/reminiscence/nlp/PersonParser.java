package eu.marioproject.marvin.abilities.reminiscence.nlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.ontologydesignpatterns.ont.mario.person_owl.Female;
import org.ontologydesignpatterns.ont.mario.person_owl.Male;
import org.ontologydesignpatterns.ont.mario.person_owl.Person;

import it.cnr.istc.stlab.lizard.commons.jena.RuntimeJenaLizardContext;

public class PersonParser extends Parser {
	
	private final static Map<String, Map<String, Map<String, String>>> relationships;
	
	static {
		relationships = initRelationships();
	}

	public PersonParser() {
		super(AnswerType.PERSON);
	}

	@Override
	public List<ParseResult> parse(String utterance, String lang) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ParseResult> find(String utterance, List<Object> targets, String lang) {
		
		List<ParseResult> results = new ArrayList<>();
		
		Person person;
		String firstName;
		String lastName;
		List<String> regexElems;
		for (Object target : targets) {
			person = (Person)target;
			regexElems = new ArrayList<>();
			firstName = person.getPerson_firstName().iterator().next().toLowerCase();
			regexElems.add(firstName);
			lastName = person.getPerson_lastName().isEmpty() ? null : person.getPerson_lastName().iterator().next().toLowerCase();
			if (lastName != null) {
				regexElems.add(lastName);
			}
			regexElems.addAll(person.getPerson_nickname());
			regexElems.addAll(getRelationship(person.getId(), lang));
			String regex = "\\b("+ StringUtils.join(regexElems, "|") +")\\b";
			//System.err.println(regex);
			super.matchRegex(utterance, results, regex, type, person);
		}
		
		return results;
	}
	
	private Set<String> getRelationship(String personId, String lang) {
		Model m = RuntimeJenaLizardContext.getContext().getModel();
		ParameterizedSparqlString pss = new ParameterizedSparqlString(
				"SELECT ?rel ?gen"
				+ " WHERE "
				+ "{ "
				+ "?pat a ?patient ."
				+ "?p ?genere ?gen ."
				+ "?p ?rel ?pat ."
				+ " }");
		pss.setIri("patient", "http://www.ontologydesignpatterns.org/ont/mario/healthrole.owl#Patient");
		pss.setIri("p", personId);
		pss.setIri("genere", Person.PERSON_HAS_GENDER);
		QueryExecution qe = QueryExecutionFactory.create(pss.asQuery(),m);
		ResultSet res = qe.execSelect();
		
		QuerySolution solution;
		String relationship;
		
		Set<String> result = new HashSet<>();
		
		while (res.hasNext()) {
			solution = res.next();
			relationship = relationships.get(solution.getResource("rel").getURI()).get(lang).get(solution.getResource("gen").getLocalName());
			result.add(relationship);
		}
		
		return result;
	}
	
	
	private static Map<String, Map<String, Map<String, String>>> initRelationships() {
		
		Map<String, Map<String, Map<String, String>>> rels = new HashMap<String, Map<String,Map<String,String>>>();
		
		Map<String, String> closeFriendEn = new HashMap<>();
		closeFriendEn.put("Male", "friend");
		closeFriendEn.put("Female", "friend");
		
		Map<String, String> closeFriendIt = new HashMap<>();
		closeFriendIt.put("Male", "amico");
		closeFriendIt.put("Female", "amica");
		
		Map<String, Map<String, String>> closeFriendLang = new HashMap<>();
		closeFriendLang.put("en", closeFriendEn);
		closeFriendLang.put("it", closeFriendIt);
		
		rels.put(Person.PERSON_CLOSE_FRIEND_OF, closeFriendLang);
		
		
		Map<String, String> colleagueEn = new HashMap<>();
		colleagueEn.put("Male", "colleague");
		colleagueEn.put("Female", "colleague");
		
		Map<String, String> colleagueIt = new HashMap<>();
		colleagueIt.put("Male", "collega");
		colleagueIt.put("Female", "collega");
		
		Map<String, Map<String, String>> colleagueLang = new HashMap<>();
		colleagueLang.put("en", colleagueEn);
		colleagueLang.put("it", colleagueIt);
		
		rels.put(Person.PERSON_COLLEAGUE_OF, colleagueLang);
		
		
		// TODO: engaged and others?
		
		
		Map<String, String> friendEn = new HashMap<>();
		friendEn.put("Male", "friend");
		friendEn.put("Female", "friend");
		
		Map<String, String> friendIt = new HashMap<>();
		friendIt.put("Male", "amico");
		friendIt.put("Female", "amica");
		
		Map<String, Map<String, String>> friendLang = new HashMap<>();
		friendLang.put("en", friendEn);
		friendLang.put("it", friendIt);
		
		rels.put(Person.PERSON_FRIEND_OF, friendLang);
		
		
		Map<String, String> lifePartnerEn = new HashMap<>();
		lifePartnerEn.put("Male", "partner");
		lifePartnerEn.put("Female", "partner");
		
		Map<String, String> lifePartnerIt = new HashMap<>();
		lifePartnerIt.put("Male", "compagno");
		lifePartnerIt.put("Female", "compagna");
		
		Map<String, Map<String, String>> lifePartnerLang = new HashMap<>();
		lifePartnerLang.put("en", lifePartnerEn);
		lifePartnerLang.put("it", lifePartnerIt);
		
		rels.put(Person.PERSON_LIFE_PARTNER_OF, lifePartnerLang);
		
		
		Map<String, String> livesWithEn = new HashMap<>();
		livesWithEn.put("Male", "housemate");
		livesWithEn.put("Female", "housemate");
		
		Map<String, String> livesWithIt = new HashMap<>();
		livesWithIt.put("Male", "coinquilino");
		livesWithIt.put("Female", "coinquilina");
		
		Map<String, Map<String, String>> livesWithLang = new HashMap<>();
		livesWithLang.put("en", livesWithEn);
		livesWithLang.put("it", livesWithIt);
		
		rels.put(Person.PERSON_LIVES_WITH, livesWithLang);
		
		
		Map<String, String> worksWithEn = new HashMap<>();
		worksWithEn.put("Male", "co-worker|coworker");
		worksWithEn.put("Female", "co-worker|coworker");
		
		Map<String, String> worksWithIt = new HashMap<>();
		worksWithIt.put("Male", "collega");
		worksWithIt.put("Female", "collega");
		
		Map<String, Map<String, String>> worksWithLang = new HashMap<>();
		worksWithLang.put("en", worksWithEn);
		worksWithLang.put("it", worksWithIt);
		
		rels.put(Person.PERSON_WORKS_WITH, worksWithLang);
		
		/*** MALE ONLY ***/
		
		Map<String, String> brotherEn = new HashMap<>();
		brotherEn.put("Male", "brother");
		
		Map<String, String> brotherIt = new HashMap<>();
		brotherIt.put("Male", "fratello|fratellino");
		
		Map<String, Map<String, String>> brotherLang = new HashMap<>();
		brotherLang.put("en", brotherEn);
		brotherLang.put("it", brotherIt);
		
		rels.put(Male.PERSON_BROTHER_OF, brotherLang);
		
		
		Map<String, String> fatherEn = new HashMap<>();
		fatherEn.put("Male", "father|dad|daddy");
		
		Map<String, String> fatherIt = new HashMap<>();
		fatherIt.put("Male", "padre|papà|babbo");
		
		Map<String, Map<String, String>> fatherLang = new HashMap<>();
		fatherLang.put("en", fatherEn);
		fatherLang.put("it", fatherIt);
		
		rels.put(Male.PERSON_FATHER_OF, fatherLang);
		
		
		Map<String, String> grandfatherEn = new HashMap<>();
		grandfatherEn.put("Male", "grandfather|grandpa|granddad|granddaddy");
		
		Map<String, String> grandfatherIt = new HashMap<>();
		grandfatherIt.put("Male", "nonno");
		
		Map<String, Map<String, String>> grandfatherLang = new HashMap<>();
		grandfatherLang.put("en", grandfatherEn);
		grandfatherLang.put("it", grandfatherIt);
		
		
		Map<String, String> grandsonEn = new HashMap<>();
		grandsonEn.put("Male", "grandson|grandchild");
		
		Map<String, String> grandsonIt = new HashMap<>();
		grandsonIt.put("Male", "nipote|nipotino");
		
		Map<String, Map<String, String>> grandsonLang = new HashMap<>();
		grandsonLang.put("en", grandsonEn);
		grandsonLang.put("it", grandsonIt);
		
		rels.put(Male.PERSON_GRANDSON_OF, grandsonLang);
		
		
		Map<String, String> husbandEn = new HashMap<>();
		husbandEn.put("Male", "husband|darling");
		
		Map<String, String> husbandIt = new HashMap<>();
		husbandIt.put("Male", "marito");
		
		Map<String, Map<String, String>> husbandLang = new HashMap<>();
		husbandLang.put("en", husbandEn);
		husbandLang.put("it", husbandIt);
		
		rels.put(Male.PERSON_HUSBAND_OF, husbandLang);
		
		
		Map<String, String> sonEn = new HashMap<>();
		sonEn.put("Male", "son|child");
		
		Map<String, String> sonIt = new HashMap<>();
		sonIt.put("Male", "figlio");
		
		Map<String, Map<String, String>> sonLang = new HashMap<>();
		sonLang.put("en", sonEn);
		sonLang.put("it", sonIt);
		
		rels.put(Male.PERSON_SON_OF, sonLang);
		
		
		/*** FEMALE ONLY ***/
		
		Map<String, String> daughterEn = new HashMap<>();
		daughterEn.put("Female", "daughter|child");
		
		Map<String, String> daughterIt = new HashMap<>();
		daughterIt.put("Female", "figlia");
		
		Map<String, Map<String, String>> dautherLang = new HashMap<>();
		dautherLang.put("en", daughterEn);
		dautherLang.put("it", daughterIt);
		
		rels.put(Female.PERSON_DAUGHTER_OF, dautherLang);
		
		
		Map<String, String> granddaughterEn = new HashMap<>();
		granddaughterEn.put("Female", "granddaughter|grandchild");
		
		Map<String, String> granddaughterIt = new HashMap<>();
		granddaughterIt.put("Female", "nipote|nipotina");
		
		Map<String, Map<String, String>> granddautherLang = new HashMap<>();
		granddautherLang.put("en", granddaughterEn);
		granddautherLang.put("it", granddaughterIt);
		
		rels.put(Female.PERSON_GRANDDAUGHTER_OF, granddautherLang);
		
		
		Map<String, String> grandmotherEn = new HashMap<>();
		grandmotherEn.put("Female", "grandmother|grandma|granny");
		
		Map<String, String> grandmotherIt = new HashMap<>();
		grandmotherIt.put("Female", "nonna");
		
		Map<String, Map<String, String>> grandmotherLang = new HashMap<>();
		grandmotherLang.put("en", grandmotherEn);
		grandmotherLang.put("it", grandmotherIt);
		
		rels.put(Female.PERSON_GRANDMOTHER_OF, grandmotherLang);
		
		
		Map<String, String> motherEn = new HashMap<>();
		motherEn.put("Female", "mother|mom|mommy");
		
		Map<String, String> motherIt = new HashMap<>();
		motherIt.put("Female", "madre|mamma");
		
		Map<String, Map<String, String>> motherLang = new HashMap<>();
		motherLang.put("en", motherEn);
		motherLang.put("it", motherIt);
		
		rels.put(Female.PERSON_MOTHER_OF, motherLang);
		
		
		Map<String, String> sisterEn = new HashMap<>();
		sisterEn.put("Female", "sister");
		
		Map<String, String> sisterIt = new HashMap<>();
		sisterIt.put("Female", "sorella|sorellina");
		
		Map<String, Map<String, String>> sisterLang = new HashMap<>();
		sisterLang.put("en", sisterEn);
		sisterLang.put("it", sisterIt);
		
		rels.put(Female.PERSON_SISTER_OF, sisterLang);
		
		
		Map<String, String> wifeEn = new HashMap<>();
		wifeEn.put("Female", "wife|darling");
		
		Map<String, String> wifeIt = new HashMap<>();
		wifeIt.put("Female", "moglie");
		
		Map<String, Map<String, String>> wifeLang = new HashMap<>();
		wifeLang.put("en", wifeEn);
		wifeLang.put("it", wifeIt);
		
		rels.put(Female.PERSON_WIFE_OF, wifeLang);
		
		return Collections.unmodifiableMap(rels);
		
	}
	
	public static void main(String[] args) {
		PersonParser p = new PersonParser();
		List<ParseResult> results = p.find("andrea", new ArrayList<>(Person.getAll()), "it");
		for (ParseResult res : results) {
			System.err.println(((Person)(res.getResultObject())).getId());
		}
	}

}
