package eu.marioproject.marvin.abilities.reminiscence.nlp;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ontologydesignpatterns.ont.mario.spatial_owl.City;

public class CityParser extends Parser {

	public CityParser() {
		super(AnswerType.CITY);
	}

	@Override
	public List<ParseResult> parse(String utterance, String lang) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ParseResult> find(String utterance, List<Object> targets, String lang) {
		List<ParseResult> results = new ArrayList<>();
		
		City city;
		List<String> regexElems = new ArrayList<>();
		String regex;
		String[] nameTokens;
		for (Object target : targets) {
			city = (City)target;
			
			for (String name : city.getGen_name()) {
				
				regexElems.add(name.toLowerCase());
				
				nameTokens = name.split("\\s");
				if (nameTokens.length > 1) {
					for (String token : nameTokens) {
						if (token.length() > 1 && !regexElems.contains(token)) {
							regexElems.add(token.trim().toLowerCase());
						}
					}
				}
				nameTokens = name.split("-");
				if (nameTokens.length > 1) {
					for (String token : nameTokens) {
						if (token.length() > 1 && !regexElems.contains(token)) {
							regexElems.add(token.trim().toLowerCase());
						}
					}
				}
				nameTokens = name.split("'");
				if (nameTokens.length > 1) {
					for (String token : nameTokens) {
						if (token.length() > 1 && !regexElems.contains(token)) {
							regexElems.add(token.trim().toLowerCase());
						}
					}
				}
				
			}
			if (!regexElems.isEmpty()) {
				regex = "\\b("+ StringUtils.join(regexElems, "|") +")\\b";
				super.matchRegex(utterance, results, regex, type, city);
			}
		}
		return results;
	}
	
	public static void main(String[] args) {
		String cityName = "Cassina de' pecchi";
		List<String> regexElems = new ArrayList<>();
		regexElems.add(cityName.toLowerCase());
		String[] nameTokens = cityName.split("\\s");
		if (nameTokens.length > 1) {
			for (String token : nameTokens) {
				if (token.length() > 1 && !regexElems.contains(token)) {
					regexElems.add(token.trim().toLowerCase());
				}
			}
		}
		nameTokens = cityName.split("-");
		if (nameTokens.length > 1) {
			for (String token : nameTokens) {
				if (token.length() > 1 && !regexElems.contains(token)) {
					regexElems.add(token.trim().toLowerCase());
				}
			}
		}
		nameTokens = cityName.split("'");
		if (nameTokens.length > 1) {
			for (String token : nameTokens) {
				if (token.length() > 1 && !regexElems.contains(token)) {
					regexElems.add(token.trim().toLowerCase());
				}
			}
		}
		if (!regexElems.isEmpty()) {
			String regex = "\\b("+ StringUtils.join(regexElems, "|") +")\\b";
			System.err.println(regex);
		}
	}

}
