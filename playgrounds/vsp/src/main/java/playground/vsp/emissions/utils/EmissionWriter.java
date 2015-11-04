/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionPrinter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.vsp.emissions.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

/**
 * @author benjamin
 *
 */
public class EmissionWriter {
	private static final Logger logger = Logger.getLogger(EmissionWriter.class);
	
	private final EmissionUtils emu;

	public EmissionWriter(){
		this.emu = new EmissionUtils();
	}
	
	public void writeHomeLocation2TotalEmissions(
			Population population,
			Map<Id, SortedMap<String, Double>> totalEmissions,
			String outFile) {
		try{
			FileWriter fstream = new FileWriter(outFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			out.append("personId \t xHome \t yHome \t");
			for (String pollutant : emu.getListOfPollutants()){
				out.append(pollutant + "[g] \t");
			}
			out.append("\n");

			for(Person person: population.getPersons().values()){
				Id personId = person.getId();
				Plan plan = person.getSelectedPlan();
				Activity homeAct = (Activity) plan.getPlanElements().get(0);
				Coord homeCoord = homeAct.getCoord();
				Double xHome = homeCoord.getX();
				Double yHome = homeCoord.getY();

				out.append(personId + "\t" + xHome + "\t" + yHome + "\t");

				Map<String, Double> emissionType2Value = totalEmissions.get(personId);
				for(String pollutant : emu.getListOfPollutants()){
					if(emissionType2Value.get(pollutant) != null){
						out.append(emissionType2Value.get(pollutant) + "\t");
					} else{
						out.append("0.0" + "\t"); // TODO: do I still need this?
					}
				}
				out.append("\n");
			}
			//Close the output stream
			out.close();
			logger.info("Finished writing output to " + outFile);
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	void writeLinkLocation2Emissions(
			Map<Id, Map<String, Double>> emissions,
			Network network,
			String outFile){
		try{
			FileWriter fstream = new FileWriter(outFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			out.append("linkId\txLink\tyLink\t");
			for (String pollutant : emu.getListOfPollutants()){
				out.append(pollutant + "[g]\t");
			}
			out.append("\n");

			for(Id linkId : emissions.keySet()){
				Link link = network.getLinks().get(linkId);
				Coord linkCoord = link.getCoord();
				Double xLink = linkCoord.getX();
				Double yLink = linkCoord.getY();

				out.append(linkId + "\t" + xLink + "\t" + yLink + "\t");

				Map<String, Double> emissionType2Value = emissions.get(linkId);
				for(String pollutant : emu.getListOfPollutants()){
					out.append(emissionType2Value.get(pollutant) + "\t");
				}
				out.append("\n");
			}
			//Close the output stream
			out.close();
			logger.info("Finished writing output to " + outFile);
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}
}