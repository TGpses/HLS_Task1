package scheduler;

public class Main {

	public static void main(String[] args) {
		RC rc = null;
		if (args.length>1){
			System.out.println("Reading resource constraints from "+args[1]+"\n");
			rc = new RC();
			rc.parse(args[1]);
		}
		
		Dot_reader dr = new Dot_reader(false);
		if (args.length < 1) {
			System.err.printf("Usage: scheduler dotfile%n");
			System.exit(-1);
		}else {
			System.out.println("Scheduling "+args[0]);
			System.out.println();
		}
		
		Graph g = dr.parse(args[0]);
		System.out.printf("%s%n", g.diagnose());
		
		/*
		Scheduler sList = new ListScheduler(rc);
		Schedule schedList = sList.schedule(g);
		System.out.printf("%nList Scheduling%n%s%n", schedList.diagnose());
		System.out.printf("cost = %s%n", schedList.cost());
		
		System.out.println("---------------------------------");
		g.reset();
		rc.parse(args[1]);
		*/
		
		Scheduler rsListM = new reverseList(rc);
		Schedule schedRListM = rsListM.schedule(g);
		System.out.printf("%nReverse List Scheduling Martin%n%s%n", schedRListM.diagnose());
		System.out.printf("cost = %s%n", schedRListM.cost());
		
		System.out.println("---------------------------------");
		g.reset();
		rc.parse(args[1]);
		
		
		Scheduler rsList = new ReverseListScheduler(rc);
		Schedule schedRList = rsList.schedule(g);
		System.out.printf("%nReverse List Scheduling%n%s%n", schedRList.diagnose());
		System.out.printf("cost = %s%n", schedRList.cost());
		
		System.out.println("---------------------------------");
		g.reset();
		rc.parse(args[1]);
		
		
		Scheduler sASAP = new ASAP();
		Schedule schedASAP = sASAP.schedule(g);
		System.out.printf("%nASAP%n%s%n", schedASAP.diagnose());
		System.out.printf("cost = %s%n", schedASAP.cost());
		/*
		sched.draw("schedules/ASAP_" + args[0].substring(args[0].lastIndexOf("/")+1));
		
		s = new ALAP();
		sched = s.schedule(g);
		System.out.printf("%nALAP%n%s%n", sched.diagnose());
		System.out.printf("cost = %s%n", sched.cost());
		
		sched.draw("schedules/ALAP_" + args[0].substring(args[0].lastIndexOf("/")+1));
		*/
		
	}
}
