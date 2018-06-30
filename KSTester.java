public class KSTester {
	
	//primjer pokretanja (za model 2 od 3 (p od q) sa ukupno 5 procesa,
	//	za proces sa id-jem 0, koji šalje poruke procesima 2, 3 i 4):
	// >java KSTester Test 0 5 2(p) 2 3 4(navedena su 3 procesa kojima se šalje)
	
	public static void main(String[] args) throws Exception {		
		Linker comm = null;
		KS proc = null;
		
		try {
			String baseName = args[0];
			int myId = Integer.parseInt(args[1]),
				numProc = Integer.parseInt(args[2]),
				p = Integer.parseInt(args[3]);
			
			int duljinaNizaRequestova = args.length - 4;

			int initialRequests[] = new int[duljinaNizaRequestova];
			for (int i=4; i<args.length; ++i)
				initialRequests[i-4] = Integer.parseInt(args[i]);
			comm = new Linker(baseName, myId, numProc);
			proc = new KS(comm, p, initialRequests);
			for (int i=0; i<numProc; ++i)
				if (i != myId)
					(new ListenerThread(i, proc)).start();
		}
		catch (Exception e) {
			System.err.println(e + "\nPozivanje: java KSTester Test myId N p idjeviProcesaKojimaIdeREQUEST");
		}
	}
}
