package scheduler;

import java.util.HashMap;
import java.util.Map;

public class ALAP extends Scheduler {
	
	/**
	 * Maximum schedule length
	 */
	private final int lmax;
	
	public ALAP() {
		lmax = 0;
	}
	public ALAP(int lmax) {
		this.lmax = lmax-1;
	}
	
	public Schedule schedule(final Graph sg) {
		Map<Node, Interval> queue = new HashMap<Node, Interval>();
		Map<Node, Interval> qq;
		Map<Node, Interval> min_queue = new HashMap<Node, Interval>();
		Schedule schedule = new Schedule();
		Integer min = lmax;
		Graph g = sg;

		for (Node nd : g)
			if (nd.leaf())
				queue.put(nd, new Interval(lmax + 1 - nd.getDelay(), lmax));
		if(queue.size() == 0)
			System.out.println("No leaf in Graph found. Empty or cyclic graph");
		

		while (queue.size() > 0) { 				// queue -> nodes to get scheduled
			qq = new HashMap<Node, Interval>();
			
			//for all Nodes in queue
			for (Node nd : queue.keySet()) {
				Interval slot = queue.get(nd); 	// get interval of current node
				if (slot.lbound < min)			
					min = slot.lbound;			// decrease min to the lower bound of the current node's slot

				schedule.add(nd, slot);			// add the current node to the schedule
				
				// for all predecessors of the current node
				for (Node l : nd.predecessors()) {
					g.handle(l, nd); 												// in each predecessor mark the current node as handled 
					Interval ii = min_queue.get(l);									// search for current pred in min_queue
					if (ii == null || slot.lbound <= ii.ubound) {					// check interval for current pred
						ii = new Interval(slot.lbound-l.getDelay(), slot.lbound-1);	
						min_queue.put(l, ii);										// if interval needs to be shifted put it to min_queue
					}
					if (!l.bottom())												// continue with next pred if there is no unhandled succ for current pred
						continue;
					
					/* update queue entry if current entry  
						- does not already exist in current queue
						- does not already exist in queue for next iteration (qq)
						- is too late for current scheduled node (successor)
					*/
					if ((queue.get(l) == null)) {									
						if (qq.get(l) == null) {										
							qq.put(l, ii);
						} else if (qq.get(l).ubound >= slot.lbound) {
							qq.put(l, ii);
						}
					} else if (queue.get(l).ubound >= slot.lbound) {
						qq.put(l, ii);
					}
				}
				//------ end loop of predecessors
			}
			//------ end loop of queue
			
			queue = qq;
		}
		g.reset();
	
		if (lmax == 0)
			return schedule.shift(-(min));
		return schedule;
	}
}
