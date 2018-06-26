import java.util.stream.IntStream;
import java.util.HashSet;

public class SingleMM extends Process {
	
	int publicLabel = myId, privateLabel = myId;
	HashSet<Integer> waitingForMe = new HashSet<Integer>();
	
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
		if (tag.equals("REQUEST")) {
			sendMsg(src, "BLOCK", publicLabel);
			waitingForMe.add(src);
		}
		else if (tag.equals("BLOCK")) {
			publicLabel = privateLabel = inc(publicLabel, m.getMessageInt());
			Transmit();
		}
		else if (tag.equals("ACTIVATE")) {
			waitingForMe.remove(src);
		}
		else if (tag.equals("TRANSMIT")) {
			if (m.getMessageInt() > publicLabel) {
				publicLabel = m.getMessageInt();
				Transmit();
			}
			else if (m.getMessageInt() == publicLabel && publicLabel == privateLabel)
				DeclareDeadlock();
		}
		else
			throw new IllegalArgumentException("Unknown message tag \"" + tag + "\"!");
    }
	
	private void DeclareDeadlock() {
		Util.println("DEADLOCK!!!");
	}
	
	private int inc(int x, int y) {
		// uniqueness is assured by holding  inc ≡ myId (mod N)
		int r = Util.max(x, y);
		return ((r - myId) / N + 1) * N + myId;
	}
	
	private void Transmit() {
		for (int proc : waitingForMe)
			sendMsg(proc, "TRANSMIT", publicLabel);
	}
}
