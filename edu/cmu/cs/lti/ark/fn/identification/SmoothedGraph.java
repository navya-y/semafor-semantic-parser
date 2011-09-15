/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * SmoothedGraph.java is part of SEMAFOR 2.0.
 * 
 * SEMAFOR 2.0 is free software: you can redistribute it and/or modify  it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * SEMAFOR 2.0 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU General Public License along
 * with SEMAFOR 2.0.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package edu.cmu.cs.lti.ark.fn.identification;
import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class SmoothedGraph implements Serializable {
	private static final long serialVersionUID = -1950841970965481100L;
	public static final double MAX_PROB = 0.8;
	private Map<String, Set<String>> fineMap;

	public Map<String, Set<String>> getFineMap() {
		return fineMap;
	}

	public void setFineMap(Map<String, Set<String>> fineMap) {
		this.fineMap = fineMap;
	}

	public Map<String, Set<String>> getCoarseMap() {
		return coarseMap;
	}

	public void setCoarseMap(Map<String, Set<String>> coarseMap) {
		this.coarseMap = coarseMap;
	}

	private Map<String, Set<String>> coarseMap;

	public SmoothedGraph(ArrayList<String> lines, 
			int t, 
			String[] typeArr, 
			String[] frameArr) {
		fineMap = new THashMap<String, Set<String>>();
		coarseMap = new THashMap<String, Set<String>>();
		System.out.println("Reading graph file...");
		for (String line: lines) {
			String[] toks = line.split("\\s+");
			if (toks.length == 1) {
				continue;
			}
			String pred = typeArr[new Integer(toks[0])];
			int li = pred.lastIndexOf(".");
			String coarsepred = pred.substring(0, li);
			int end = 0;
			if (2*t + 1 > toks.length) {
				end = toks.length;
			} else {
				end = 2*t + 1;
			}
			for (int k= 1; k < end; k = k + 2) {
				String frame = frameArr[new Integer(toks[k])];
				if (fineMap.containsKey(pred)) {
					Set<String> fineSet = fineMap.get(pred);
					fineSet.add(frame);
					fineMap.put(pred, fineSet);
				} else {
					Set<String> fineSet = new THashSet<String>();
					fineSet.add(frame);
					fineMap.put(pred, fineSet);
				}
				if (coarseMap.containsKey(coarsepred)) {
					Set<String> coarseSet = coarseMap.get(coarsepred);
					coarseSet.add(frame);
					coarseMap.put(coarsepred, coarseSet);
				} else {
					Set<String> coarseSet = new THashSet<String>();
					coarseSet.add(frame);
					coarseMap.put(coarsepred, coarseSet);
				}
			}
		}
		System.out.println("Finished reading graph file.");
	}

	public SmoothedGraph (String file, int t) {
		fineMap = new THashMap<String, Set<String>>();
		coarseMap = new THashMap<String, Set<String>>();
		System.out.println("Reading graph file...");
		try {
			BufferedReader bReader = new BufferedReader(new FileReader(file));
			String line = null;
			int count = 0;
			while ((line = bReader.readLine()) != null) {
				String[] toks = line.trim().split("\t");
				String pred = toks[0];
				int li = pred.lastIndexOf(".");
				String coarsepred = pred.substring(0, li);
				String[] frames = toks[1].split(" ");
				for (int i = 0 ; i < 2*t; i = i + 2) {
					String frame = frames[i];
					if (fineMap.containsKey(pred)) {
						Set<String> fineSet = fineMap.get(pred);
						fineSet.add(frame);
						fineMap.put(pred, fineSet);
					} else {
						Set<String> fineSet = new THashSet<String>();
						fineSet.add(frame);
						fineMap.put(pred, fineSet);
					}
					if (coarseMap.containsKey(coarsepred)) {
						Set<String> coarseSet = coarseMap.get(coarsepred);
						coarseSet.add(frame);
						coarseMap.put(coarsepred, coarseSet);
					} else {
						Set<String> coarseSet = new THashSet<String>();
						coarseSet.add(frame);
						coarseMap.put(coarsepred, coarseSet);
					}
				}
				count++;
			}
			System.out.println();
			bReader.close();
			System.out.println("Finished reading graph file.");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}	
	
	public static void main(String[] args) {
		String file = "/usr2/dipanjan/experiments/FramenetParsing/fndata-1.5/ACLSplits/5/smoothed.graph.a.0.5.k.10.mu.0.1.nu.0.000001";
		String[] tarr = {"1", "2", "3", "5", "10"};
		for (String t: tarr) {
			SmoothedGraph sg = new SmoothedGraph(file, new Integer(t));
			SerializedObjects.writeSerializedObject(sg, file + ".t." + t + ".jobj.gz");
		}
	}
}