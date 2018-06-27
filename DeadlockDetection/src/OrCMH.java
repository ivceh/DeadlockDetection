import java.util.stream.IntStream;

public class OrCMH extends Process {
	boolean dependent[], wait[];
	int num[];
	boolean blocked = false;
	
	public OrCMH(Linker initComm, int initialRequests[]) {
		super(initComm);
		dependent = new boolean[N]; // all false by default
    num = new int[N];  // number of query messages sent for diffussion initiated by process i
    wait = new boolean[N]; // denots that it has been blocked since the last engaging query from process i  		
    Initialize(initialRequests);
	}
	
	public void Initialize(int initialRequests[]) {
	  for(int i=0; i<initialRequests.length; ++i) {
		  sendMsg(initialRequests[i], "REQUEST", "");
      dependent[initialRequests[i]] = true;
	  }
    // process 0 initiates difussion computation
    if(myId == 0) {
      Util.println("sleeping 1s before initiating diffusion computation...");
      Util.mySleep(1000);
      for(int j=0; j<N; ++j)
        if(dependent[j]) {
          sendMsg(j, "QUERY", myId);
          num[myId]++;
        }
      wait[myId] = true;
    }
	}
	
	public void sendMsg(int destId, String tag, String msg) {
		super.sendMsg(destId, tag, msg);
  }
	
	public synchronized void handleMsg(Msg m, int src, String tag) {
    if(tag.equals("QUERY")) {
      int initiator = m.getMessageInt();
      if(!wait[initiator]) {
        // this is the engaging query for process src, send queries to all dependent
        for(int j=0; j<N; ++j)
          if(dependent[j]) {
            sendMsg(j, "QUERY", initiator);
            num[initiator]++;
          }
        wait[initiator] = true;
      } else {
        sendMsg(src, "REPLY", initiator);  
      }
    }
    if(tag.equals("REPLY")) {
      int initiator = m.getMessageInt();
      if(wait[initiator]) {
        num[initiator]--;
        if(num[initiator] == 0) {
          if(initiator == myId)
            DeclareDeadlock();
          else
            sendMsg(initiator, "REPLY", initiator);        
        }
      }
    }
  }
	
	private void DeclareDeadlock() {
		Util.println("!> DEADLOCK <!");
	}
}
