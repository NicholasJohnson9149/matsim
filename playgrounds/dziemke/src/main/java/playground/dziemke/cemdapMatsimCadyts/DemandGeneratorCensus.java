/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.dziemke.cemdapMatsimCadyts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.households.Household;
import org.matsim.households.HouseholdImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;
import playground.dziemke.cemdapMatsimCadyts.oneperson.SimpleHousehold;
import playground.dziemke.cemdapMatsimCadyts.oneperson.SimplePerson;
import playground.dziemke.utils.LogToOutputSaver;

import java.io.*;
import java.util.*;

/**
 * This class is derived from "playground.dziemke.cemdapMatsimCadyts.oneperson.DemandGeneratorOnePersonV2.java"
 * In contrast to its predecessors, it creates a full population (not just car users).
 * Its main inputs are the Census and the Pendlerstatistik
 * 
 * @author dziemke
 */
public class DemandGeneratorCensus {
	private static final Logger LOG = Logger.getLogger(DemandGeneratorCensus.class);
	private static Integer counterMissingComRel = 0;

	
	/*
	 * there will be mismatches between number of employees from zensus and commuter from commuter file
	 * - because of socially secured workers (commuter file) vs. all workers (zensus)
	 * - because not exactly the same year
	 * Handle by scaling?
	 */

	// Parameters
	private int numberOfPlansPerPerson = 1;
	private String planningAreaId = "11000000"; // "Amtliche Gemeindeschlüssel (AGS)" of Berlin is "11000000"
	private double defaultAdultsToEmployeesMaleRatio = 1.3;  // This is an assumption, oriented on observed values.
	private double defaultEmployeesToCommutersRatio = 3.0;  // This is an assumption, oriented on observed values, deliberately chosen slightly too high.

	private Population pop;
	private Map<Id<Household>, Household> households;
	private CensusReader censusReader;
	private ObjectAttributes municipalities;
	private Map<String, Map<String, CommuterRelationV2>> relationsMap;
	private List<String> lors;

	private String outputBase;

	public static void main(String[] args) {

		// Input and output files
		String commuterFileOutgoing1 = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Berlin_2009/B2009Ga.txt";
		String commuterFileOutgoing2 = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Brandenburg_2009/Teil1BR2009Ga.txt";
		String commuterFileOutgoing3 = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Brandenburg_2009/Teil2BR2009Ga.txt";
		String commuterFileOutgoing4 = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Brandenburg_2009/Teil3BR2009Ga.txt";
//		String commuterFileOutgoingTest = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Brandenburg_2009/Teil1BR2009Ga_Test.txt";
//		String censusFile = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/zensus_2011/bevoelkerung/csv_Bevoelkerung/Zensus11_Datensatz_Bevoelkerung.csv";
		String censusFile = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/zensus_2011/bevoelkerung/csv_Bevoelkerung/Zensus11_Datensatz_Bevoelkerung_BE_BB.csv";
		String shapeFileLors = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/shapefiles/Bezirksregion_EPSG_25833.shp";
		String outputBase = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap_berlin/census-based_test_4/"; // TODO ...

		String[] commuterFilesOutgoing = {commuterFileOutgoing1, commuterFileOutgoing2, commuterFileOutgoing3, commuterFileOutgoing4};

		DemandGeneratorCensus demandGeneratorCensus = new DemandGeneratorCensus(commuterFilesOutgoing, censusFile,
				shapeFileLors, outputBase);
		demandGeneratorCensus.generateDemand();
	}

	public DemandGeneratorCensus(String[] commuterFilesOutgoing, String censusFile, String shapeFileLors,
								 String outputBase) {

		// Infrastructure
		pop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		households = new HashMap<>();
		LogToOutputSaver.setOutputDirectory(outputBase);

		// Read census
		censusReader = new CensusReader(censusFile, ";");
		municipalities = censusReader.getMunicipalities();

		// Read commuter relations

		relationsMap = new HashMap<>();
		for (String commuterFileOutgoing : commuterFilesOutgoing) {
			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing, "\t");
			Map<String, Map<String, CommuterRelationV2>> currentRelationMap = commuterFileReader.getRelationsMap();
			relationsMap.putAll(currentRelationMap);
		}

//		{
//			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing1, "\t");
//			Map<String, Map<String, CommuterRelationV2>> currentRelationMap = commuterFileReader.getRelationsMap();
//			relationsMap.putAll(currentRelationMap);
//		}{
//			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing2, "\t");
//			Map<String, Map<String, CommuterRelationV2>> currentRelationMap = commuterFileReader.getRelationsMap();
//			relationsMap.putAll(currentRelationMap);
//		}{
//			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing3, "\t");
//			Map<String, Map<String, CommuterRelationV2>> currentRelationMap = commuterFileReader.getRelationsMap();
//			relationsMap.putAll(currentRelationMap);
//		}{
//			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing4, "\t");
//			Map<String, Map<String, CommuterRelationV2>> currentRelationMap = commuterFileReader.getRelationsMap();
//			relationsMap.putAll(currentRelationMap);
//		}
//		{
//			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoingTest, "\t");
//			Map<String, Map<String, CommuterRelationV2>> currentRelationMap = commuterFileReader.getRelationsMap();
//			relationsMap.putAll(currentRelationMap);
//		}

		// Read LORs
//		List<String> lors = readShape(shapeFileLors, "SCHLUESSEL", "LOR");
		lors = readShape(shapeFileLors, "SCHLUESSEL");

		this.outputBase = outputBase;
	}

	public void generateDemand() {
		// Other
//		Integer counterMissingComRel = 0;

		// Loop over  municipalities
		for (String munId : relationsMap.keySet()) {
			Map<String, CommuterRelationV2> relationsFromMunicipality = relationsMap.get(munId);

			// Employees from Zensus seems to be all employees, not only socially-secured employees
			int employeesMale = (int) municipalities.getAttribute(munId, "employedMale");
			int employeesFemale = (int) municipalities.getAttribute(munId, "employedFemale");

			scaleRelations(relationsFromMunicipality, employeesMale, employeesFemale, defaultEmployeesToCommutersRatio);
			List<String> commuterRelationListMale = createRelationList(relationsFromMunicipality, "male");
			List<String> commuterRelationListFemale = createRelationList(relationsFromMunicipality, "female");

			int counter = 1;

			int pop18_24Male = (int) municipalities.getAttribute(munId, "pop18_24Male");
			int pop25_29Male = (int) municipalities.getAttribute(munId, "pop25_29Male");
			int pop30_39Male = (int) municipalities.getAttribute(munId, "pop30_39Male");
			int pop40_49Male = (int) municipalities.getAttribute(munId, "pop40_49Male");
			int pop50_64Male = (int) municipalities.getAttribute(munId, "pop50_64Male");
			int pop65_74Male = (int) municipalities.getAttribute(munId, "pop65_74Male");
			int pop75PlusMale = (int) municipalities.getAttribute(munId, "pop75PlusMale");

			int pop18_24Female = (int) municipalities.getAttribute(munId, "pop18_24Female");
			int pop25_29Female = (int) municipalities.getAttribute(munId, "pop25_29Female");
			int pop30_39Female = (int) municipalities.getAttribute(munId, "pop30_39Female");
			int pop40_49Female = (int) municipalities.getAttribute(munId, "pop40_49Female");
			int pop50_64Female = (int) municipalities.getAttribute(munId, "pop50_64Female");
			int pop65_74Female = (int) municipalities.getAttribute(munId, "pop65_74Female");
			int pop75PlusFemale = (int) municipalities.getAttribute(munId, "pop75PlusFemale");

			int adultsMale = pop18_24Male + pop25_29Male + pop30_39Male + pop40_49Male + pop50_64Male;
			int adultsFemale = pop18_24Female + pop25_29Female + pop30_39Female + pop40_49Female + pop50_64Female;

			// The adults-to-employees ratio is needed to determine if a given person has a job
			double adultsToEmployeesMaleRatio;
			double adultsToEmployeesFemaleRatio;
			if (employeesMale != 0) { // Avoid dividing by zero
				adultsToEmployeesMaleRatio = (double) adultsMale / (double) employeesMale;
			} else {
				adultsToEmployeesMaleRatio = defaultAdultsToEmployeesMaleRatio;
			}
			if (employeesFemale != 0) { // Avoid dividing by zero
				adultsToEmployeesFemaleRatio = (double) adultsFemale / (double) employeesFemale;
			} else {
				adultsToEmployeesFemaleRatio = defaultAdultsToEmployeesMaleRatio;
			}

			// 18-24
			{
				int gender = 0;
				int lowerAgeBound = 18;
				int upperAgeBound = 24;
				createHouseholdsAndPersons(pop, households, counter, munId, planningAreaId, lors, pop18_24Male,
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop18_24Male;
			}
			{
				int gender = 1;
				int lowerAgeBound = 18;
				int upperAgeBound = 24;
				createHouseholdsAndPersons(pop, households, counter, munId, planningAreaId, lors, pop18_24Female,
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
				counter += pop18_24Female;
			}
			// 25-29
			{
				int gender = 0;
				int lowerAgeBound = 25;
				int upperAgeBound = 29;
				createHouseholdsAndPersons(pop, households, counter, munId, planningAreaId, lors, pop25_29Male,
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop25_29Male;
			}
			{
				int gender = 1;
				int lowerAgeBound = 25;
				int upperAgeBound = 29;
				createHouseholdsAndPersons(pop, households, counter, munId, planningAreaId, lors, pop25_29Female,
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
				counter += pop25_29Female;
			}
			// 30-39
			{
				int gender = 0;
				int lowerAgeBound = 30;
				int upperAgeBound = 39;
				createHouseholdsAndPersons(pop, households, counter, munId, planningAreaId, lors, pop30_39Male,
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop30_39Male;
			}
			{
				int gender = 1;
				int lowerAgeBound = 30;
				int upperAgeBound = 39;
				createHouseholdsAndPersons(pop, households, counter, munId, planningAreaId, lors, pop30_39Female,
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
				counter += pop30_39Female;
			}
			// 40-49
			{
				int gender = 0;
				int lowerAgeBound = 40;
				int upperAgeBound = 49;
				createHouseholdsAndPersons(pop, households, counter, munId, planningAreaId, lors, pop40_49Male,
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop40_49Male;
			}
			{
				int gender = 1;
				int lowerAgeBound = 40;
				int upperAgeBound = 49;
				createHouseholdsAndPersons(pop, households, counter, munId, planningAreaId, lors, pop40_49Female,
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
				counter += pop40_49Female;
			}
			// 50-64
			{
				int gender = 0;
				int lowerAgeBound = 50;
				int upperAgeBound = 64;
				createHouseholdsAndPersons(pop, households, counter, munId, planningAreaId, lors, pop50_64Male,
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop50_64Male;
			}
			{
				int gender = 1;
				int lowerAgeBound = 50;
				int upperAgeBound = 64;
				createHouseholdsAndPersons(pop, households, counter, munId, planningAreaId, lors, pop50_64Female,
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
				counter += pop50_64Female;
			}
			// 65-74
			{
				int gender = 0;
				int lowerAgeBound = 65;
				int upperAgeBound = 74;
				createHouseholdsAndPersons(pop, households, counter, munId, planningAreaId, lors, pop65_74Male,
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop65_74Male;
			}
			{
				int gender = 1;
				int lowerAgeBound = 65;
				int upperAgeBound = 74;
				createHouseholdsAndPersons(pop, households, counter, munId, planningAreaId, lors, pop65_74Female,
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
				counter += pop65_74Female;
			}
			// 75+
			{
				int gender = 0;
				int lowerAgeBound = 75;
				int upperAgeBound = 90; // Assumption!
				createHouseholdsAndPersons(pop, households, counter, munId, planningAreaId, lors, pop75PlusMale,
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop75PlusMale;
			}
			{
				int gender = 1;
				int lowerAgeBound = 75;
				int upperAgeBound = 90; // Assumption!
				createHouseholdsAndPersons(pop, households, counter, munId, planningAreaId, lors, pop75PlusFemale,
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
//				counter += pop75PlusFemale;
			}


			// Information on unassigned commuter relations
			if (commuterRelationListMale.size() > 100) {
				LOG.info(commuterRelationListMale.size() + " male commuter relations from " + munId +
						" remain unassigned; based on census, there are " + employeesMale + " male employees.");
			}
			if (commuterRelationListFemale.size() > 100) {
				LOG.info(commuterRelationListFemale.size() + " female commuter relations from " + munId +
						" remain unassigned; based on census, there are " + employeesFemale + " female employees.");
			}


		}
		// TODO householdsFile
		writePersonsFile(pop, outputBase + "persons.dat");

		// Create copies of population, but with different work locations
		for (int i = 1; i < numberOfPlansPerPerson; i++) { // "less than" because the plan consists already in the original
			Population population2 = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();

			for (Person person : pop.getPersons().values()) {
				if ((boolean) person.getAttributes().getAttribute("employed")) {
					String locationOfWork = (String) person.getAttributes().getAttribute("locationOfWork");
					if (locationOfWork.equals("-99")) {
						throw new RuntimeException("This combination of attribute values is implaubible.");
					} else {
						if (locationOfWork.length() == 6) { // An LOR, i.e. a location inside Berlin
							person.getAttributes().putAttribute("locationOfWork", getRandomLor(lors));
						} else if (locationOfWork.length() == 8) { // An "Amtliche Gemeindeschlüssel (AGS)", i.e. a location outside Berlin
							// Do nothing; leave it as it is
						} else {
							throw new RuntimeException("The identifier of the work location cannot have a length other than 6 or 8.");
						}
					}
				}
				population2.addPerson(person);
			}
			writePersonsFile(population2, outputBase + "persons" + (i+1) + ".dat");
		}

		LOG.warn("There are " + counterMissingComRel + " employees who have been set to unemployed since no commuter relation could be assigned to them.");
	}


	private static void createHouseholdsAndPersons(Population population, Map<Id<Household>, Household> households, int counter,
			String municipalityId, String planningAreaId, List<String> lors, int numberOfPersons, int gender,
			int lowerAgeBound, int upperAgeBound, double adultsToEmployeesRatio, List<String> commuterRelationList) {
		
		for (int i = 0; i < numberOfPersons; i++) {
			Id<Household> householdId = Id.create(municipalityId + "_" + (counter + i), Household.class);
			HouseholdImpl household = new HouseholdImpl(householdId); // TODO Or use factory?
			household.getAttributes().putAttribute("numberOfAdults", 1); // always 1; no household structure
			household.getAttributes().putAttribute("totalNumberOfHouseholdVehicles", 1);
			household.getAttributes().putAttribute("homeTSZLocation", getLocation(municipalityId, planningAreaId, lors));
			household.getAttributes().putAttribute("numberOfChildren", 0); // none, ignore them in this version
			household.getAttributes().putAttribute("householdStructure", 1); // 1 = single, no children
			
			Id<Person> personId = Id.create(householdId + "_1", Person.class);
			Person person = population.getFactory().createPerson(personId);
			// Following attribute names inspired by "PersonUtils.java": "sex", "hasLicense", "carAvail", "employed", "age", "travelcards"
			person.getAttributes().putAttribute("householdId", householdId);
			boolean employed = false;
			if (lowerAgeBound < 65 && upperAgeBound > 17) { // younger and older people are never employed
				employed = getEmployed(adultsToEmployeesRatio);
			}
			person.getAttributes().putAttribute("employed", employed);
			person.getAttributes().putAttribute("student", false); // TODO certain share of young adults?
			person.getAttributes().putAttribute("hasLicense", true); // for CEMDAP's "driversLicence" variable
			
			if (employed) {
				if (commuterRelationList.size() == 0) { // No relations left in list, which employee could choose from
					counterMissingComRel++;
					person.getAttributes().putAttribute("locationOfWork", "-99");
					person.getAttributes().putAttribute("employed", false);
				} else {
					person.getAttributes().putAttribute("locationOfWork", getRandomWorkLocation(commuterRelationList, planningAreaId, lors));
				}
			} else {
				person.getAttributes().putAttribute("locationOfWork", "-99");
			}
			
			person.getAttributes().putAttribute("locationOfSchool", "-99"); // TODO ?
			person.getAttributes().putAttribute("gender", gender); // for CEMDAP's "female" variable
			person.getAttributes().putAttribute("age", getAgeInBounds(lowerAgeBound, upperAgeBound));
			person.getAttributes().putAttribute("parent", false);
			
			population.addPerson(person);
			
			List<Id<Person>> personIds = new ArrayList<>(); // does in current implementation (only 1 p/hh) not make much sense
			personIds.add(personId);
			household.setMemberIds(personIds);
			households.put(householdId, household);
		}
	}	
	
			
	private static void scaleRelations(Map<String, CommuterRelationV2> relationsFromMunicipality, int employeesMale,
			int employeesFemale, double defaultEmployeesToCommutersRatio) {
		// Count all commuters starting in the given municipality
		int commutersMale = 0;
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			if (relation.getTripsMale() == null) { // This is the case when there are very few people traveling on that relation
				if (relation.getTrips() == null || relation.getTrips() == 0) {
					throw new RuntimeException("No travellers at all on this relation! This should not happen.");
				} else {
					relation.setTripsMale((relation.getTrips() / 2));
				}
			}
			commutersMale += relation.getTripsMale();
		}
		int commutersFemale = 0;
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			if (relation.getTripsFemale() == null) { // This is the case when there are very few people traveling on that relation
				if (relation.getTrips() == null || relation.getTrips() == 0) {
					throw new RuntimeException("No travellers at all on this relation! This should not happen.");
				} else {
					relation.setTripsFemale((relation.getTrips() / 2));
				}
			}
			commutersFemale += relation.getTripsFemale();
		}
		
		// Compute ratios
		double employeesToCommutersMaleRatio;
		double employeesToCommutersFemaleRatio;
		if (employeesMale != 0) {
			employeesToCommutersMaleRatio = (double) employeesMale / (double) commutersMale;
		} else {
			employeesToCommutersMaleRatio = defaultEmployeesToCommutersRatio;
		}
		if (employeesFemale != 0) {
			employeesToCommutersFemaleRatio = (double) employeesFemale / (double) commutersFemale;
		} else {
			employeesToCommutersFemaleRatio = defaultEmployeesToCommutersRatio;
		}
		
		// Scale
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			relation.setTripsMale((int) Math.ceil(relation.getTripsMale() * employeesToCommutersMaleRatio));
		}
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			relation.setTripsFemale((int) Math.ceil(relation.getTripsFemale() * employeesToCommutersFemaleRatio));
		}
	}


	private static List<String> createRelationList(Map<String, CommuterRelationV2> relationsFromMunicipality, String gender) {
		List<String> commuterRealtionsList = new ArrayList<>();
		for (String destination : relationsFromMunicipality.keySet()) {
			int trips;
			switch (gender) {
				case "male":
					trips = relationsFromMunicipality.get(destination).getTripsMale();
					break;
				case "female":
					trips = relationsFromMunicipality.get(destination).getTripsFemale();
					break;
				default:
					throw new IllegalArgumentException("Must either be male or female.");
			}
			for (int i = 0; i < trips ; i++) {
				commuterRealtionsList.add(destination);
			}
		}
		return commuterRealtionsList;
	}


	private static String getRandomWorkLocation(List<String> commuterRelationList, String planningAreaId, List<String> lors) {
		Random random = new Random();
		int position = random.nextInt(commuterRelationList.size());
		String workMunicipalityId = commuterRelationList.get(position);
		commuterRelationList.remove(position);
		return getLocation(workMunicipalityId, planningAreaId, lors);
	}


	private static String getLocation(String municipalityId, String planningAreaId, List<String> lors) {
		String locationId;
		if (municipalityId.equals(planningAreaId)){ // Berlin
			locationId = getRandomLor(lors);
		} else { // Other municipalities
			locationId = municipalityId;
		}
		return locationId;
	}


	private static String getRandomLor(List<String> lors) {
		Random random = new Random();
		return lors.get(random.nextInt(lors.size()));
	}


	private static boolean getEmployed(double adultsToEmployeesRatio) {
		return Math.random() * adultsToEmployeesRatio < 1;
	}
	
	
	private static int getAgeInBounds(int lowerBound, int upperBound) {
		return (int) (lowerBound + Math.random() * (upperBound - lowerBound + 1));
	}


	private static List<String> readShape(String shapeFile, String attributeKey) {
		List<String> lors = new ArrayList<>();
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);

		for (SimpleFeature feature : features) {
			String key = (String) feature.getAttribute(attributeKey);
			lors.add(key);
		}
		return lors;
	}
	
	
	private static void writePersonsFile(Population population, String fileName) {
		BufferedWriter bufferedWriterPersons = null;
		
		try {
			File personFile = new File(fileName);
    		FileWriter fileWriterPersons = new FileWriter(personFile);
    		bufferedWriterPersons = new BufferedWriter(fileWriterPersons);
    		    		    		
    		for (Person person : population.getPersons().values()) {
    			
    			
    			Id<Household> householdId = (Id<Household>) person.getAttributes().getAttribute("householdId");
    			Id<Person> personId = person.getId();
    			
    			int employed;
    			if ((boolean) person.getAttributes().getAttribute("employed")) {
    				employed = 1;
    			} else {
    				employed = 0;
    			}
    			
    			int student;
    			if ((boolean) person.getAttributes().getAttribute("student")) {
    				student = 1;
    			} else {
    				student = 0;
    			}
    			
    			int driversLicence;
    			if ((boolean) person.getAttributes().getAttribute("hasLicense")) {
    				driversLicence = 1;
    			} else {
    				driversLicence = 0;
    			}
    			
    			String locationOfWork = (String) person.getAttributes().getAttribute("locationOfWork");
    			String locationOfSchool = (String) person.getAttributes().getAttribute("locationOfSchool");
    			
    			int female = (Integer) person.getAttributes().getAttribute("gender"); // assumes that female = 1
    			int age = (Integer) person.getAttributes().getAttribute("age");
    			
    			int parent;
    			if ((boolean) person.getAttributes().getAttribute("parent")) {
    				parent = 1;
    			} else {
    				parent = 0;
    			}
    			
    			// Altogether this creates 59 columns = number in query file
    			bufferedWriterPersons.write(householdId + "\t" + personId + "\t" + employed  + "\t" + student
    					+ "\t" + driversLicence + "\t" + locationOfWork + "\t" + locationOfSchool
    					+ "\t" + female + "\t" + age + "\t" + parent + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
    					+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
    					+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
    					+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
    					+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
    					+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
    					+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 );
    			bufferedWriterPersons.newLine();
    		}
		} catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the BufferedWriter
            try {
                if (bufferedWriterPersons != null) {
                    bufferedWriterPersons.flush();
                    bufferedWriterPersons.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
		LOG.info("Persons file " + fileName + " written.");
    }
	

	private static void writeHouseholdsFile(Map <String, SimplePerson> persons, Map<Integer, SimpleHousehold> households,
			String fileName) {
		BufferedWriter bufferedWriterHouseholds = null;
		
		try {
            File householdsFile = new File(fileName);
    		FileWriter fileWriterHouseholds = new FileWriter(householdsFile);
    		bufferedWriterHouseholds = new BufferedWriter(fileWriterHouseholds);

    		int householdIdFromPersonBefore = 0;
    		
    		// Use map of persons to write a household for every person under the condition that the household does not
    		// already exist (written from another persons); used to enable the potential use of multiple-person households.
    		// TODO use proper household sizes
    		for (String key : persons.keySet()) {
    			int householdId = persons.get(key).getHouseholdId();
    			
    			if (householdId != householdIdFromPersonBefore) {
    				int numberOfAdults = households.get(householdId).getNumberOfAdults();
    				int totalNumberOfHouseholdVehicles = households.get(householdId).getTotalNumberOfHouseholdVehicles();
    				int homeTSZLocation = households.get(householdId).getHomeTSZLocation();
    				int numberOfChildren = households.get(householdId).getNumberOfChildren();
    				int householdStructure = households.get(householdId).getHouseholdStructure();
    				
    				// Altogether this creates 32 columns = number in query file
    				bufferedWriterHouseholds.write(householdId + "\t" + numberOfAdults + "\t" + totalNumberOfHouseholdVehicles
    						 + "\t" + homeTSZLocation + "\t" + numberOfChildren + "\t" + householdStructure + "\t" + 0
    						 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    						 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    						 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    						 + "\t" + 0);
	    			bufferedWriterHouseholds.newLine();
	    			householdIdFromPersonBefore = householdId;
    			}
    		}
    	} catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the BufferedWriter
            try {
                if (bufferedWriterHouseholds != null) {
                    bufferedWriterHouseholds.flush();
                    bufferedWriterHouseholds.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
		LOG.info("Households file " + fileName + " written.");
    }

    public Population getPop() {
    	return pop;
	}
}