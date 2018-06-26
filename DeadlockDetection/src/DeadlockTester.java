public class DeadlockTester {
	public static void main(String[] args) throws Exception {
		String baseName = args[0];
		int myId = Integer.parseInt(args[1]),
			numProc = Integer.parseInt(args[2]);
		int initialRequests[] = new int[args.length - 4];
		for (int i=4; i<args.length; ++i)
			initialRequests[i-4] = Integer.parseInt(args[i]);
		Linker comm = new Linker(baseName, myId, numProc);
		
		Process proc;
		if (args[3].equals("SingleMM"))
			proc = new SingleMM(comm, initialRequests);
		else if (args[3].equals("AndCMH"))
			proc = new AndCMH(comm, initialRequests);
		else
			throw new IllegalArgumentException("Unknown algorithm!");
		
		for (int i=0; i<numProc; ++i)
			if (i != myId)
				(new ListenerThread(i, proc)).start();
	}
}
