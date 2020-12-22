package scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map.Entry;

public class reverseList extends Scheduler{
	Set<RT> allRT = new HashSet<RT>();								
	Map<String, Set<RT>> freeRes = new HashMap<String, Set<RT>>();	// resources free to be used
	Map<String, Set<RT>> usedRes = new HashMap<String, Set<RT>>(); 	// resources currently in use
	Map<String, Interval> usageInterval = new HashMap<String, Interval>(); 	// time interval a resource is currently used
	Map<String, Node> usageNode = new HashMap<String, Node>();		// Node by which a resource is currently used
	Map<Node, Integer> mobility = new HashMap<Node, Integer>(); 	// time interval a resource is currently used
	Graph g = new Graph();
	
	/**
	 * Constructor for the List Scheduler under a Resource Constraint
	 * @param rc - resource constraint given by parsing the resource definition file
	 */
	public reverseList(final RC rc) {
		freeRes = rc.getAllRes();
	}
	
	private void setMobility(final Graph sg) {
		Scheduler asap = new ASAP();
		Schedule asaps = asap.schedule(sg);
		Scheduler alap = new ALAP();
		Schedule alaps = alap.schedule(sg);
		for(Node n: alaps.nodes()) {
			mobility.put(n,alaps.slot(n).lbound); //Adds to starting time of ALAP to the Map
		}
		for(Entry<Node, Integer> e: mobility.entrySet()) {
			int mob;
			mob=mobility.get(e.getKey());
			mob=mob - asaps.slot(e.getKey()).lbound;
			
			mobility.put(e.getKey(),mob); //Adds to starting time of ALAP to the Map
		}
		System.out.println(mobility);
		
		
		return;
	}
	/**
	 * find the Priority value for all Nodes(longest path)
	 * @param sg - Graph to analyse
	 * @param mode - 0: longest path, 1: latency, 2: successors 3: delay-mobility
	 * @return Map of Nodes and their priority
	 */
	private Map<Node, Integer> findPriorities(final Graph sg, int mode){
		Queue<Node> queue = new LinkedList<Node>();
		Map<Node, Integer> result = new HashMap<Node, Integer>();
		for (Node n : sg) {
			if(n.leaf()) {	//Finds all leafs, adds to queue, prio=1
				switch(mode){
		        case 0: 
		        		result.put(n, 1); 
		        		break;
		        case 1:
		        		result.put(n, n.getDelay());
		        		break;
		        case 2:
		        		result.put(n, 0);
		        		break;
		        case 3: 
		        		result.put(n, n.getDelay()-mobility.get(n));
		        		break;
				}
				
				queue.add(n);
			}
		}
		while (!queue.isEmpty()) {
			Node nq=queue.element();
			//System.out.println(nq);
			if(nq.allPredecessors().size()!=0) {
				for(Entry<Node, Integer> e : nq.allPredecessors().entrySet()) {
					queue.add(e.getKey()); 	//Adds all Predecessors of the node to the Queue
				}
			}
			if(nq.allSuccessors().size()!=0) {
				int prio= 0;
				for(Entry<Node, Integer> e : nq.allSuccessors().entrySet()) {
					//System.out.println(e);
					if(result.containsKey(e.getKey())) {
						if(result.get(e.getKey())>prio){
			        		prio =result.get(e.getKey()); //Gets the max priority of the successors
			        	}
				    
					}
				}
				switch(mode){
		        case 0: 
		        		prio =prio+1; //Gets the max priority of the successors adds 1 and set it to the node
		        		break;
		        case 1:
		        		prio =prio+nq.getDelay(); //adds delay to max prio
		        		break;
		        case 2:
		        		prio =prio+nq.allSuccessors().size(); //adds # of Successors to prio
		        		break;
		        case 3://nicht notwendigerweise in for-Schleife
	        		result.put(nq, nq.getDelay()-mobility.get(nq));
	        		break;
			
				}
				result.put(nq, prio);
			}
			queue.remove(nq);
		}
			//prio = n.allPredecessors().size();		// fehler werden nur vorgänger von einem node angegeben aber nicht alle bis zur wurzel
			//result.put(n, prio);
		System.out.println(result);
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
				for(Node n: cur.predecessors()) {
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
			} else if(map.get(n) <= minPrio) {
				minPrio = map.get(n);
				minNode = n;
			}
		count++; //hat gefehlt
		}
			
		return minNode;
	}
	
	private Node getMaxPrioNode(Map<Node, Integer> map) {
		
		Node maxNode = null;
		int maxPrio = 0;
		int count = 0;
		
		for(Node n : map.keySet()) {
			if(count == 0) {
				maxPrio = map.get(n);
				maxNode = n;
			} else if(map.get(n) >= maxPrio) {
				maxPrio = map.get(n);
				maxNode = n;
			}	
			count++; //hat gefehlt
		}
		
		return maxNode;
	}
	
	
	
	public Schedule schedule(final Graph sg) {
		Schedule schedule = new Schedule();
		g=sg;
		setMobility(sg);
		Map<Node, Integer> prioNodes = findPriorities(g,0); 				// Set of Nodes with their Priority as value
		Map<Node, Interval> candidates = new HashMap<Node, Interval>(); 	// candidate set to schedule in the current iteration/time step
		Map<Node, Integer> candidatesPrio = new HashMap<Node, Integer>(); 	// candidate priority
		
		for (Node n : g) {
			allRT.add(n.getRT());
			n.reset();
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
						//System.out.println(n);
						// if current node is of type k and all predecessors are handled
						if(n.getRT().equals(k) && n.bottom()) {
							candidates.put(n, new Interval(t-n.getDelay()+1,t)); // put to candidates as it would start now 
							candidatesPrio.put(n, prioNodes.get(n));
							// store only candidates with the highest priority //höchste oder kleinste bei reverse?
							if(candidates.size() > maxNodes) { 
								minPrioNode = getMinPrioNode(candidatesPrio);
								candidates.remove(minPrioNode);
								candidatesPrio.remove(minPrioNode);
							}
							//System.out.println(candidates);
						}else {
							continue;
						}						
					}
					
					// schedule remaining candidates
					for(Node n : candidates.keySet()) {
						useResource(k, candidates.get(n),n);
						schedule.add(n,candidates.get(n));
						prioNodes.remove(n);
						//System.out.println(candidates);
					}
					
				}else {
					continue;
				}
				
			}			
			
			t = t-1;
		}
		schedule.shift(-t);
		
		return schedule;
	}
	
}
