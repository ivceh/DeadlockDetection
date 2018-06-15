public class AndCMHTester {
	public static void main(String[] args) throws Exception {
		String baseName = args[0];
		int myId = Integer.parseInt(args[1]),
			numProc = Integer.parseInt(args[2]);
		int initialRequests[] = new int[args.length - 3];
		for (int i=3; i<args.length; ++i)
			initialRequests[i-3] = Integer.parseInt(args[i]);
		Linker comm = new Linker(baseName, myId, numProc);
		AndCMH proc = new AndCMH(comm, initialRequests);
		for (int i=0; i<numProc; ++i)
			if (i != myId)
				(new ListenerThread(i, proc)).start();
	}
}
