package org.skype.test.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.skype.test.simulation.model.Driver;

public class Util {
	static Random random = new Random();
	public static List<Driver> getRandomSubset(List<Driver> list, int size) {
		Collections.shuffle(list);
		return list.subList(0, size);
	}
	public static List<Driver> getRandomSubset(List<Driver> driversSet, int size, Driver exclude, int buddyCount) {
		Collections.shuffle(driversSet);
		List<Driver> subList = new ArrayList<Driver>();
		Iterator<Driver> iterator = driversSet.iterator();
		while(subList.size() < size && iterator.hasNext()) {
			Driver driver = iterator.next();
			if(!driver.equals(exclude) && driver.getNode().getBuddies().size() < buddyCount) subList.add(driver);
		}
		return subList;
	}

}
