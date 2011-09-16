package edu.cmu.cs.lti.ark.fn.parsing;

import java.util.Arrays;
import java.util.Map;

import edu.cmu.cs.lti.ark.util.ds.Pair;
import gnu.trove.THashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;

import ilog.concert.*; 
import ilog.cplex.*;

public class ILPDecoding {
	private IloCplex cplex = null;
	public ILPDecoding() {
		try {
			cplex = new IloCplex(); 
		} catch (IloException e) { 
			System.err.println("Concert exception caught: " + e);
			System.exit(-1);
		}
	}
	
	public Map<String, String> decode(Map<String, Pair<int[], Double>[]> scoreMap) {
		Map<String, String> res = new THashMap<String, String>();
		if (scoreMap.size() == 0) {
			return res;
		}
		String[] keys = new String[scoreMap.size()];
		scoreMap.keySet().toArray(keys);
		Arrays.sort(keys);
		int totalCount = 0;
		int max = -Integer.MAX_VALUE;
		for (int i = 0; i < keys.length; i++) {
			Pair<int[], Double>[] arr = scoreMap.get(keys[i]);
			totalCount += arr.length;
			for (int j = 0; j < arr.length; j++) {
				int start = arr[j].getFirst()[0];
				int end = arr[j].getFirst()[1];
				if (start != -1) {
					if (start > max) {
						max = start;
					}
				}
				if (end != -1) {
					if (end > max) {
						max = end;
					}
				}
			}
		}
		System.out.println("Max index:" + max);
		TIntHashSet[] overlapArray = new TIntHashSet[max+1];
		for (int i = 0; i < max+1; i++) {
			overlapArray[i] = new TIntHashSet();
		}
		int[] lb = new int[totalCount];
		int[] ub = new int[totalCount];
		double[] objVals = new double[totalCount];
		System.out.println("Size of keys: " + keys.length);
		System.out.println("Totalcount: " + totalCount);
		try {
			int count = 0;
			for (int i = 0; i < keys.length; i++) {
				Pair<int[], Double>[] arr = scoreMap.get(keys[i]);
				for (int j = 0; j < arr.length; j++) {
					lb[count] = 0; ub[count] = 1;
					objVals[count] = arr[j].getSecond();
					int start = arr[j].getFirst()[0];
					int end = arr[j].getFirst()[1];
					if (start != -1 && end != -1) {
						for (int k = start; k <= end; k++) {
							overlapArray[k].add(count);
						}
					}
					count++;
				}
			}
			IloIntVar[] x = cplex.intVarArray(totalCount, lb, ub);
			cplex.addMaximize(cplex.scalProd(x, objVals));
			count = 0;
			// constraints indicating that an FE can have only one span
			for (int i = 0; i < keys.length; i++) {
				Pair<int[], Double>[] arr = scoreMap.get(keys[i]);
				IloNumExpr[] prods = new IloNumExpr[arr.length];
				for (int j = 0; j < arr.length; j++) {
					prods[j] = cplex.prod(1.0, x[count]);
					count++;
				}
				cplex.addEq(cplex.sum(prods), 1.0);
			}
			// constraints for overlapping spans
			for (int i = 0; i < max+1; i++) {
				if (overlapArray[i].size() == 0) {
					continue;
				}
				IloNumExpr[] prods = new IloNumExpr[overlapArray[i].size()];
				int j = 0;
				TIntIterator spanItr = overlapArray[i].iterator();				
				while (spanItr.hasNext()) {
					int s = spanItr.next();
					prods[j] = cplex.prod(1.0, x[s]);
					j++;
				}
				cplex.addLe(cplex.sum(prods), 1.0);
			}
			if (cplex.solve()) { 
				cplex.output().println("Solution status = " + cplex.getStatus()); 
				cplex.output().println("Solution value  = " + cplex.getObjValue());
				double[] val = cplex.getValues(x); 
				int ncols = cplex.getNcols();
				if (ncols != totalCount) {
					System.out.println("Problem ncols: " + ncols + " totalCount: " + totalCount);
					System.exit(-1);
				}
				count = 0;
				for (int i = 0; i < keys.length; i++) {
					Pair<int[], Double>[] arr = scoreMap.get(keys[i]);
					for (int j = 0; j < arr.length; j++) {
						if (val[count] > 0.0) {
							res.put(keys[i], arr[j].getFirst()[0] + "_" + arr[j].getFirst()[1]);
						}
						count++;
					}
				}
			}
			cplex.clearModel();
		} catch (IloException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return res;
	}

	public void decodeTrivial() {
		try { 
			int[] lb = {0, 0, 0}; 
			int[] ub = {40, Integer.MAX_VALUE, Integer.MAX_VALUE};
			IloIntVar[] x  = cplex.intVarArray(3, lb, ub);
			int[] objvals = {1, 2, 3};
			cplex.addMaximize(cplex.scalProd(x, objvals)); 
			cplex.addLe(cplex.sum(cplex.prod(-1.0, x[0]), 
					cplex.prod( 1.0, x[1]), 
					cplex.prod( 1.0, x[2])), 20.0); 
			cplex.addLe(cplex.sum(cplex.prod( 1.0, x[0]), 
					cplex.prod(-3.0, x[1]), 
					cplex.prod( 1.0, x[2])), 30.0);
			if (cplex.solve()) { 
				cplex.output().println("Solution status = " + cplex.getStatus()); 
				cplex.output().println("Solution value  = " + cplex.getObjValue());
				double[] val = cplex.getValues(x); 
				int ncols = cplex.getNcols(); 
				for (int j = 0; j < ncols; ++j) 
					cplex.output().println("Column: " + j + " Value = " + val[j]); 
			}
			cplex.clearModel();
		} catch (IloException e) { 
			System.err.println("Concert exception caught: " + e); 
		}
	}

	public void end() {
		cplex.end();
	}

	public static void main(String[] args) {
		System.out.println("Solving ILP:");
		ILPDecoding ilp = new ILPDecoding();
		ilp.decodeTrivial();
		ilp.decodeTrivial();
		ilp.end();
	}
}