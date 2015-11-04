/* *********************************************************************** *
 * project: org.matsim.*
 * MoneyEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.busCorridorWelfareAnalysis;

import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;

/**
 * @author Ihab
 *
 */
public class MoneyEventHandler implements AgentMoneyEventHandler {

	private double revenues;
	
	@Override
	public void reset(int iteration) {
		this.revenues = 0;
	}

	@Override
	public void handleEvent(AgentMoneyEvent event) {
		this.revenues = this.revenues + (-1 * event.getAmount());
	}

	/**
	 * @return the earnings
	 */
	public double getRevenues() {
		return revenues;
	}
}