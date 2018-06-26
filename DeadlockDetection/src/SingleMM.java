import java.util.stream.IntStream;

public class SingleMM extends Process {
	
	public SingleMM(Linker initComm, int initialRequests[]) {
		super(initComm);
		Initialize(initialRequests);
	}
	
	public void Initialize(int initialRequests[]) {
		for(int i=0; i<initialRequests.length; ++i) {
			sendMsg(initialRequests[i], "REQUEST", "");
			Util.mySleep(1000);
		}
	}
	
	public synchronized void handleMsg(Msg m, int src, String tag) {
		
    }
	
	private void DeclareDeadlock() {
		Util.println("DEADLOCK!!!");
	}
}
