/* *********************************************************************** *
 * project: org.matsim.*
 * JointModesDepartureHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.VehicleUsingAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.PassengerRoute;

/**
 * @author thibautd
 */
public class JointModesDepartureHandler implements DepartureHandler , MobsimEngine {
	private final QNetsimEngine netsimEngine;
	private final PassengersWaitingPerDriver passengersWaitingPerDriver = new PassengersWaitingPerDriver();
	// map driverId -> driver info
	private final Map<Id , WaitingDriver> waitingDrivers =
		new LinkedHashMap<Id , WaitingDriver>();
	
	public JointModesDepartureHandler(
			final QNetsimEngine netsimEngine) {
		this.netsimEngine = netsimEngine;
	}

	// /////////////////////////////////////////////////////////////////////////
	// departure handler
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public boolean handleDeparture(
			final double now,
			final MobsimAgent agent,
			final Id linkId) {
		final String mode = agent.getMode();

		if ( mode.equals( JointActingTypes.DRIVER ) ) {
			handleDriverDeparture( now , agent , linkId );
			return true;
		}

		if ( mode.equals( JointActingTypes.PASSENGER ) ) {
			handlePassengerDeparture( now , agent , linkId );
			return true;
		}

		return netsimEngine.getDepartureHandler().handleDeparture( now , agent , linkId );
	}

	private void handleDriverDeparture(
			final double now,
			final MobsimAgent agent,
			final Id linkId) {
		final Id driverId = agent.getId();
		final Collection<Id> passengerIds = getPassengerIds( agent );
		final Id vehicleId = getVehicleId( agent );
		final MobsimVehicle vehicle = netsimEngine.getVehicles().get( vehicleId );

		final Map<Id, PassengerAgent> passengersWaiting =
			passengersWaitingPerDriver.getPassengersWaitingDriverAtLink(
					driverId , linkId );
		final Collection<Id> presentPassengers = new ArrayList<Id>();
		presentPassengers.addAll( passengersWaiting.keySet() );
		addAll( vehicle.getPassengers() , presentPassengers );

		if ( presentPassengers.containsAll( passengerIds ) ) {
			// all passengers are or already in the car,
			// or waiting. Board waiting passengers and depart.
			for (Id passengerId : passengerIds) {
				final PassengerAgent passenger = passengersWaiting.remove( passengerId );
				// say to the driver to board the passenger before leaving the first
				// link. We cannot add the passengers to the vehicle here, as the driver
				// may have to wait for the vehicle to come before departing.
				((PassengerUnboardingDriverAgent) agent).addPassengerToBoard( passenger );
			}

			final boolean handled =
				netsimEngine.getDepartureHandler().handleDeparture(
						now,
						agent,
						linkId );

			if ( !handled ) {
				throw new RuntimeException( "failed to handle departure. Check the main modes?" );
			}

			waitingDrivers.remove( driverId );
		}
		else {
			waitingDrivers.put( driverId , new WaitingDriver( agent , linkId ) );
		}
	}

	private static void addAll(
			final Collection<? extends PassengerAgent> passengers,
			final Collection<Id> collectionToFill) {
		for ( PassengerAgent p : passengers ) collectionToFill.add( p.getId() );
	}

	private static Id getVehicleId(final MobsimAgent agent) {
		if ( !(agent instanceof VehicleUsingAgent) ) throw new RuntimeException( agent.getClass().toString() );
		return ((VehicleUsingAgent) agent).getPlannedVehicleId();
	}

	private static Collection<Id> getPassengerIds(final MobsimAgent agent) {
		if ( !(agent instanceof PlanAgent) ) throw new RuntimeException( agent.getClass().toString() );
		final Leg currentLeg = (Leg) ((PlanAgent) agent).getCurrentPlanElement();
		final DriverRoute route = (DriverRoute) currentLeg.getRoute();
		return route.getPassengersIds();
	}

	private void handlePassengerDeparture(
			final double now,
			final MobsimAgent agent,
			final Id linkId) {
		final Id driverId = getDriverId( agent );

		// go in the "queue"
		final Map<Id, PassengerAgent> waiting =
			passengersWaitingPerDriver.getPassengersWaitingDriverAtLink(
					driverId,
					linkId );

		waiting.put( agent.getId() , (PassengerAgent) agent );

		// if the driver is waiting, as him to depart.
		// departure will succeed only of all passengers are here;
		// otherwise, everybody will wait for everybody to be here.
		final WaitingDriver wDriver = waitingDrivers.get( driverId );
		if ( wDriver != null && wDriver.linkId.equals( linkId ) ) {
			handleDriverDeparture(
					now,
					wDriver.driverAgent,
					linkId );
		}
	}

	private static Id getDriverId(final MobsimAgent agent) {
		if ( !(agent instanceof PlanAgent) ) throw new RuntimeException( agent.getClass().toString() );
		final Leg currentLeg = (Leg) ((PlanAgent) agent).getCurrentPlanElement();
		final PassengerRoute route = (PassengerRoute) currentLeg.getRoute();
		return route.getDriverId();
	}

	// /////////////////////////////////////////////////////////////////////////
	// engine
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public void doSimStep(final double time) {
		// do nothing
	}

	@Override
	public void onPrepareSim() {
		// do nothing
	}

	@Override
	public void afterSim() {
		// do nothing
	}

	@Override
	public void setInternalInterface(final InternalInterface internalInterface) {
	}

	// /////////////////////////////////////////////////////////////////////////
	// nested classes
	// /////////////////////////////////////////////////////////////////////////
	private final class WaitingDriver {
		public final MobsimAgent driverAgent;
		public final Id linkId;

		public WaitingDriver(
				final MobsimAgent agent,
				final Id link) {
			this.driverAgent = agent;
			this.linkId = link;
		}
	}

	private final class PassengersWaitingPerDriver {
		private final Map<Id, PassengersWaitingForDriver> map =
			new LinkedHashMap<Id, PassengersWaitingForDriver>();

		public Map<Id, PassengerAgent> getPassengersWaitingDriverAtLink(
				final Id driverId,
				final Id linkId) {
			PassengersWaitingForDriver ps = map.get( driverId );

			if ( ps == null ) {
				ps = new PassengersWaitingForDriver();
				map.put( driverId , ps );
			}

			return ps.getPassengersWaitingAtLink( linkId );
		}
	}

	private final class PassengersWaitingForDriver {
		private final Map<Id, Map<Id, PassengerAgent>> agentsAtLink = new LinkedHashMap<Id, Map<Id, PassengerAgent>>();

		public Map<Id, PassengerAgent> getPassengersWaitingAtLink(final Id linkId) {
			Map<Id, PassengerAgent> ps = agentsAtLink.get( linkId );

			if ( ps == null ) {
				ps = new LinkedHashMap<Id, PassengerAgent>();
				agentsAtLink.put( linkId , ps );
			}

			return ps;
		}
	}
}
