package com.joulespersecond.seattlebusbot;

import org.json.JSONObject;

public class ObaData {
	JSONObject mData;
	
	/**
	 * Constructor for ObaData
	 * 
	 * @param The JSON object representing the response.
	 */
	public ObaData(JSONObject obj) {
		mData = obj;
	}
	/**
	 * Retrieves the list of stops, if they exist.
	 * 
	 * @return The list of stops, or an empty array.
	 */
	public ObaArray 
	getStops() {
		return new ObaArray(ObaApi.getChildArray(mData, "stops"));
	}
	/**
	 * Retrieves the Stop for this response.
	 * 
	 * @return The list of stops, or an empty object.
	 */
	public ObaStop 
	getStop() {
		return new ObaStop(ObaApi.getChildObj(mData, "stop"));
	}
	/**
	 * Retrieves the list of Nearby Stops for the stop.
	 * 
	 * @return The list of nearby stops, or an empty array.
	 */
	public ObaArray
	getNearbyStops() {
		return new ObaArray(ObaApi.getChildArray(mData, "nearbyStops"));
	}
	
	/**
	 * Retrieves the list of arrivals and departures.
	 * 
	 * @return The list of arrivals/departures, or an empty array.
	 */
	public ObaArray
	getArrivalsAndDepartures() {
		return new ObaArray(ObaApi.getChildArray(mData, "arrivalsAndDepartures"));
	}
	/*
	public ObaPolylinesArray
	getPolylines() {
	}
	*/
}
