package eu.marioproject.marvin.abilities.reminiscence.el;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultFunctionsAndVars {
	
	private final static String FILE = "mario" + File.separator + "reminiscence" + File.separator + "defaultVars.json";

	private List<ELFunction> defaultFunctions;
	private Map<String, String> defaultVariables;
	
	public DefaultFunctionsAndVars() {
	}
	
	public List<ELFunction> getDefaultFunctions() {
		return defaultFunctions;
	}
	public void setDefaultFunctions(List<ELFunction> defaultFunctions) {
		this.defaultFunctions = defaultFunctions;
	}
	public Map<String, String> getDefaultVariables() {
		return defaultVariables;
	}
	public void setDefaultVariables(Map<String, String> defaultVariables) {
		this.defaultVariables = defaultVariables;
	}
	
	public static DefaultFunctionsAndVars load() {
		
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			return mapper.readValue(new File(FILE), DefaultFunctionsAndVars.class);
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
		
		return null;
	}
	
}
