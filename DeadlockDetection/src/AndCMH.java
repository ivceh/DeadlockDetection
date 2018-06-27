import java.util.stream.IntStream;

public class AndCMH extends Process {
	boolean dependent[];
	int requests[], replies[];
	boolean blocked = false;
	
	public AndCMH(Linker initComm, int initialRequests[]) {
		super(initComm);
		dependent = new boolean[N]; // all false by default
		requests = new int[N]; // all 0 by default
		replies = new int[N]; // all 0 by default
		Initialize(initialRequests);
	}
	
	public void Initialize(int initialRequests[]) {
		for(int i=0; i<initialRequests.length; ++i) {
			sendMsg(initialRequests[i], "REQUEST", "");
			Util.mySleep(1000);
		}
	}
	
	public void sendMsg(int destId, String tag, String msg) {
		//if(!(myId==0 && tag.equals("REPLY")))
		if(!(tag.equals("REPLY")))
			super.sendMsg(destId, tag, msg);
    if (tag.equals("REQUEST")) {
    	++requests[destId];
			blocked=true;
			if(blocked) {
				if (dependent[myId])
					DeclareDeadlock();
				else {
					for (int j=0; j<N; ++j)
						if (requests[j] > 0)
							sendMsg(j, "PROBE", myId);
				}
			}
		}
       // else if (!(myId==0 && tag.equals("REPLY")))
       // 	--replies[destId];
    }
	
	public synchronized void handleMsg(Msg m, int src, String tag) {
		if (tag.equals("REQUEST")) {
			Util.mySleep(5000);
			sendMsg(src, "REPLY", "");
			++replies[src];
		}
		else if (tag.equals("REPLY")) {
			blocked=false;
			--requests[src];
			//if (IntStream.of(requests).allMatch(x -> (x==0)))
			blocked = false;
			for (int i=0; i<N; ++i)
				if (requests[i] != 0) {
					blocked = true;
					break;
				}
		}
		else if (tag.equals("PROBE")) {
			if (blocked && !dependent[m.getMessageInt()] && replies[src]>0) {
				Util.println("Tu sam");
				dependent[m.getMessageInt()] = true;
				if (myId == m.getMessageInt())
					DeclareDeadlock();
				else {
					for (int j=0; j<N; ++j)
						if (requests[j] > 0)
							sendMsg(j, "PROBE", m.getMessageInt());
				}
			}
		}
    }
	
	private void DeclareDeadlock() {
		Util.println("DEADLOCK!!!");
	}
}
