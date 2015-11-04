/* *********************************************************************** *
 * project: org.matsim.*
 * DecisionModels.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package preprocess;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

public class DecisionModels {
	
	private TreeMap<Id, DecisionModel> decisionModels = new TreeMap<Id, DecisionModel>();
	
	public DecisionModel getDecisionModelForAgent(Id agentId) {
		return this.decisionModels.get(agentId);
	}
	
	public void addDecisionModelForAgent(DecisionModel decisionModel, Id agentId) {
		this.decisionModels.put(agentId, decisionModel);
	}
}