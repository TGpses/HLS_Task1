package scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class ListScheduler extends Scheduler{
	
	Set<RT> allRT = new HashSet<RT>();								
	Map<String, Set<RT>> freeRes = new HashMap<String, Set<RT>>();	// resources free to be used
	Map<String, Set<RT>> usedRes = new HashMap<String, Set<RT>>(); 	// resources currently in use
	Map<String, Interval> usageInterval = new HashMap<String, Interval>(); 	// time interval a resource is currently used
	Map<String, Node> usageNode = new HashMap<String, Node>();		// Node by which a resource is currently used
	Graph g = new Graph();
	
	/**
	 * Constructor for the List Scheduler under a Resource Constraint
	 * @param rc - resource constraint given by parsing the resource definition file
	 */
	public ListScheduler(final RC rc) {
		freeRes = rc.getAllRes();
	}
	
	/**
	 * find the Priority value for all Nodes
	 * @param sg - Graph to analyse
	 * @return Map of Nodes and their priority
	 */
	private Map<Node, Integer> findPriorities(final Graph sg){
		Map<Node, Integer> result = new HashMap<Node, Integer>();
		for (Node n : sg) {
			int prio = n.allPredecessors().size();		
			result.put(n, prio);
		}		
		return result;
	}
	
	
	/**
	 * marks resources as free if the current time step is not part of their interval they are used currently
	 * marks nodes as handled if the run interval is exceeded
	 * @param timeStep - current time step
	 */
	private void updateResources(int timeStep) {
		LinkedList<String> sList = new LinkedList<String>();
		for(String s: usageInterval.keySet()) {
			if(!usageInterval.get(s).contains(timeStep)) {
				freeRes.put(s, usedRes.get(s));
				usedRes.remove(s);
				sList.add(s);
				Node cur = usageNode.get(s);
				for(Node n: cur.successors()) {
					g.handle(n, cur);
				}
				usageNode.remove(s);
			}
		}
		
		for(String s: sList) {
			usageInterval.remove(s);
		}
		
		
	}
	
	/**
	 * marks a resource of a type as used for the given interval of usage
	 * @param rt - Resource Type of the resource will be used for
	 * @param i - time interval in which the resource will be used
	 * @param nd - Node which uses the resource in this interval
	 */
	private void useResource(RT rt, Interval i, Node nd) {
		for(String s: freeRes.keySet()) {
			if(freeRes.get(s).contains(rt)) {
				usedRes.put(s, freeRes.get(s));
				usageInterval.put(s, i);
				usageNode.put(s, nd);
				freeRes.remove(s);
				break;
			}
		}		
	}
	
	/**
	 * count free resources on which the RT can be executed
	 * @param rt - RT to be executed on a resource
	 * @return number of resource that can execute a operation of type rt
	 */
	private int countFreeResOfType(RT rt) {
		int count = 0;
		for(String s: freeRes.keySet()) {
			if(freeRes.get(s).contains(rt))
				count++;
		}		
		return count;
	}
	
	/**
	 * determines the minimum value of priority for a Node within a Map
	 * @param map - Map to analyze
	 * @return Node with the minimal priority
	 */
	private Node getMinPrioNode(Map<Node, Integer> map) {
		
		Node minNode = null;
		int minPrio = 0;
		int count = 0;
		
		for(Node n : map.keySet()) {
			if(count == 0) {
				minPrio = map.get(n);
				minNode = n;
				count++;
			} else if(map.get(n) <= minPrio) {
				minPrio = map.get(n);
				minNode = n;
			}	
		}
			
		return minNode;
	}
	
	
	public Schedule schedule(final Graph sg) {
		Schedule schedule = new Schedule();
		g=sg;
		Map<Node, Integer> prioNodes = findPriorities(g); 					// Set of Nodes with their Priority as value
		Map<Node, Interval> candidates = new HashMap<Node, Interval>(); 	// candidate set to schedule in the current iteration/time step
		Map<Node, Integer> candidatesPrio = new HashMap<Node, Integer>(); 	// candidate priority
		
		for (Node n : g) {
			allRT.add(n.getRT());
		}
		

		int t = 0;
		// iteration over time steps 
		while(prioNodes.size() > 0) {

			updateResources(t);
			
			for(RT k : allRT) {
				candidates.clear();
				candidatesPrio.clear();
				int maxNodes = countFreeResOfType(k);
				
				if(maxNodes > 0) {
					
					Node minPrioNode = null;
					
					// walk through all nodes to find the candidate set for the number of free resources for RT in the given time step
					for(Node n : prioNodes.keySet()) {
						
						// if current node is of type k and all predecessors are handled
						if(n.getRT().equals(k) && n.top()) {
							candidates.put(n, new Interval(t,t+n.getDelay()-1)); // put to candidates as it would start now 
							candidatesPrio.put(n, prioNodes.get(n));
							
							// store only candidates with the highest priority
							if(candidates.size() > maxNodes) { 
								minPrioNode = getMinPrioNode(candidatesPrio);
								candidates.remove(minPrioNode);
								candidatesPrio.remove(minPrioNode);
							}
							
						}else {
							continue;
						}						
					}
					
					// schedule remaining candidates
					for(Node n : candidates.keySet()) {
						useResource(k, candidates.get(n),n);
						schedule.add(n,candidates.get(n));
						prioNodes.remove(n);
					}
					
				}else {
					continue;
				}
				
			}			
			
			t = t+1;
		}
		
		return schedule;
	}
	
	
}

