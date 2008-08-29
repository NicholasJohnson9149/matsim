/* *********************************************************************** *
 * project: org.matsim.*
 * SelectedPlans2ESRIShapeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.gis.matsim2esri.plans;

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class SelectedPlans2ESRIShapeTest extends MatsimTestCase {

	public void testSelectedPlansActsShape() {
		String populationFilename = "./test/scenarios/equil/plans100.xml";
		String networkFilename = "./test/scenarios/equil/network.xml";
		String outputDir = getOutputDirectory();
		
		String refShp = getInputDirectory() + "acts.shp";
		String refDbf = getInputDirectory() + "acts.dbf";
		
		String outShp = getOutputDirectory() + "acts.shp";
		String outDbf = getOutputDirectory() + "acts.dbf";
	
		Gbl.createConfig(null);
		Gbl.createWorld();
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFilename);
		Gbl.getWorld().setNetworkLayer(network);
		
		Population population = new Population();
		new MatsimPopulationReader(population).readFile(populationFilename);
		
		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(population, crs, outputDir);
		sp.setOutputSample(0.9);
		sp.setActBlurFactor(100);
		sp.setWriteActs(true);
		sp.setWriteLegs(false);
		sp.write();
		
		System.out.println("calculating *.shp file checksums...");
		long checksum1 = CRCChecksum.getCRCFromFile(refShp);
		long checksum2 = CRCChecksum.getCRCFromGZFile(outShp);
		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals(checksum1, checksum2);
		
		System.out.println("calculating *.dbf file checksums...");
		checksum1 = CRCChecksum.getCRCFromFile(refDbf);
		checksum2 = CRCChecksum.getCRCFromGZFile(outDbf);
		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals(checksum1, checksum2);
	}
	
	public void testSelectedPlansLegsShape() {
		String populationFilename = "./test/scenarios/equil/plans100.xml";
		String networkFilename = "./test/scenarios/equil/network.xml";
		String outputDir = getOutputDirectory();
		
		String refShp = getInputDirectory() + "legs.shp";
		String refDbf = getInputDirectory() + "legs.dbf";
		
		String outShp = getOutputDirectory() + "legs.shp";
		String outDbf = getOutputDirectory() + "legs.dbf";
	
		Gbl.createConfig(null);
		Gbl.createWorld();
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFilename);
		Gbl.getWorld().setNetworkLayer(network);
		
		Population population = new Population();
		new MatsimPopulationReader(population).readFile(populationFilename);
		
		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(population, crs, outputDir);
		sp.setOutputSample(0.85);
		sp.setLegBlurFactor(100);
		sp.setWriteActs(false);
		sp.setWriteLegs(true);
		sp.write();
		
		System.out.println("calculating *.shp file checksums...");
		long checksum1 = CRCChecksum.getCRCFromFile(refShp);
		long checksum2 = CRCChecksum.getCRCFromGZFile(outShp);
		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals(checksum1, checksum2);
		
		System.out.println("calculating *.dbf file checksums...");
		checksum1 = CRCChecksum.getCRCFromFile(refDbf);
		checksum2 = CRCChecksum.getCRCFromGZFile(outDbf);
		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals(checksum1, checksum2);
	}
	
}
