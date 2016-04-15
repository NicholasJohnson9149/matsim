/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.polettif.multiModalMap.osm;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * A version of MultimodalNetworkCreator that retains the pt tags in the mode tag of the network.
 *
 * @author boescpa
 */
public class MultimodalNetworkCreatorPT implements MultimodalNetworkCreator {

	Network network;

	public MultimodalNetworkCreatorPT(Network network) {
		this.network = network;
	}

	@Override
	public void createMultimodalNetwork(String osmFile) {
		OsmNetworkReaderWithPT osmReader =	new OsmNetworkReaderWithPT(this.network, true);

		osmReader.setKeepPaths(false);

		osmReader.parse(osmFile);
	}
}