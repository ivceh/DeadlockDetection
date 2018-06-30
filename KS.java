import java.util.stream.IntStream;

public class KS extends Process {
	
	boolean wait; //trenutni status (blokiran ili ne)
	int t; //trenutno (lokalno) vrijeme
	int t_block; //lokalno vrijeme kad je zadnji put blokiran
	
	int in[]; //oni koji čekaju na moj REPLY
	int out[]; // oni od kojih ja čekam REPLY

	int kolikoTebaReplyjeva; //koliko ukupno replyjeva se čeka (p u p-od-q zahtjevu)
	int p; //broj REPLY-jeva koje još trebam za odblokiranje
	double w; //težina - za otkrivanje završetka algoritma
	
	
	
	
	//SNIMKA
	//snimka[i][] - čvorovi na koje čekamo u snimci koju je pokrenuo čvor i
	int snimka_out[][]; 
	int snimka_in[][]; //tko na nas čeka u snimci
	int snimka_t[]; //vrijeme kada je i pokrenuo snimanje
	boolean snimka_s[]; //lokano stanje u snimci (blokiran ili ne)
	int snimka_p[]; //koliko REQUEST-ova još treba u snimci
	
	
	
	public KS(Linker initComm, int p, int initialRequests[]) {
		
		super(initComm);
		
		wait = false; //stanje: na početku nije blokiran
		t = 1; //moje vrijeme počinje od 1
		t_block = 0; //nije još bio blokiran
		
		in = new int[N]; // all 0 by default - nitko mi još nije poslao REQUEST
		out = new int[N]; // all 0 by default - nikome još nisam poslao REQUEST
		
		this.p = 0; //na početku ne trebam niti jedan REPLY od nekog
		w = 1.0; //na početku je sva težina kod procesa
		
		kolikoTebaReplyjeva = p;
		
		snimka_out = new int[N][N];
		snimka_in = new int[N][N];
		snimka_t = new int[N];
		snimka_s = new boolean[N]; //svi na početku neblokirani (u svakoj snimci)
		snimka_p = new int[N];
		
		
		//pošalji početne zahtjeve
		if(initialRequests.length>0){
			multicast(initialRequests, "REQUEST", "");
		}
	}
	
	
	public void snapshot_initiate() {
		int init = myId; //ja pokrećem snimku
		
		w = 0; //težina je 0 (1.0 težina će biti poslana sa FLOOD-ovima)
		
		snimka_t[init] = t; //zabilježi vrijeme pokretanja
		for(int i=0; i<out.length; ++i){	
			snimka_out[init][i] = out[i];
			//ako pokrećem snimku nije me briga tko me čeka (na snimci)
			snimka_in[init][i] = 0;
		}
		snimka_s[init] = true; //zabilježi da je u blokiranom stanju
		snimka_p[init] = p; //zabilježi na koliko se još REQUEST-ova čeka
		
		slanje_FLOODova(myId, snimka_t[init], 1);
	}
	
	
	public void slanje_FLOODova(int id, int vrijeme, int tezina){
		
		int suma = 0; //izračunaj koliko imam sljedbenika u GČ
		for(int i=0; i<out.length; ++i){
			if(out[i] == 1)
				++suma;
		}	
		
		//npr. umjesto 1/3 šaljem 3 (ako suma=3, tj. imam 3 sljedbenika u GČ)
		int[] saljem = new int[]{id, vrijeme, suma*tezina};
		
		for(int i=0; i<out.length; ++i){
			if(out[i] == 1)
				super.sendMsg(i, "FLOOD", Util.writeArray(saljem));
		}
	}
	
	
	public void multicast(int[] destIds, String tag, String msg) {
		if(tag.equals("REQUEST")) {
			for(int i=0; i<destIds.length; ++i){
				++t;
				out[destIds[i]] = 1;
				super.sendMsg(destIds[i], tag, msg);	
			}
			p = kolikoTebaReplyjeva; //postavi p na broj potrebnim REPLY-jeva
			t_block = t;
			wait = true; //blokiran
			
			//malo odspavaj pa pokreni algoritam otkrivanja blokade
			Util.mySleep(5000);
			snapshot_initiate();
		}
	}
	
	
	
	public void sendMsg(int destId, String tag, String msg) {
		if(tag.equals("REPLY")) {
			++t;
			in[destId] = 0;
			super.sendMsg(destId, tag, msg);
		}
    }
	
	public synchronized void handleMsg(Msg m, int src, String tag) {
		if (tag.equals("REQUEST")) {
			in[src] = 1;
			++t;
		}
		else if (tag.equals("REPLY")) {
			++t;
			out[src] = 0;
			--p;
			if(p == 0) {
				wait = false; //nije više u blokadi
				for(int i = 0; i < out.length; ++i){
					if(out[i] == 1) {
						++t;
						super.sendMsg(i, "CANCEL", "");
						out[i] = 0;
					}
				}
			}
		}
		else if (tag.equals("CANCEL")) {
			in[src] = 0;
			++t;
		}
		else if (tag.equals("FLOOD")) {
			int[] dijeloviPoruke = new int[3];
			Util.readArray(m.getMessage(), dijeloviPoruke);
			
			int init = dijeloviPoruke[0]; //tko je pokrenuo ovo snimanje
			int t_init = dijeloviPoruke[1]; //kada je pokrenuo svoje snimanje
			int tezina = dijeloviPoruke[2]; //primljena težina
			
			
			if(snimka_t[init] < t_init && in[src] == 1){
				//valjana FLOOD poruka za novu snimku (dobivena od prethodnika)
				//snimi stanje:
				for(int i=0; i<out.length; ++i){
					snimka_out[init][i] = out[i];
					//nova snimka pa je samo src prethodnik
					if(i == src)
						snimka_in[init][i] = 1;
					else
						snimka_in[init][i] = 0;
				}
				snimka_t[init] = t_init;
				snimka_s[init] = wait;
				
				if(wait == true){
					//čvor je blokiran
					snimka_p[init] = p;
					slanje_FLOODova(init, t_init, tezina);
				}
				else if(wait == false){
					//čvor je aktivan
					snimka_p[init] = 0;
					int[] saljem = new int[]{init, t_init, tezina};
					super.sendMsg(src, "ECHO", Util.writeArray(saljem));
					snimka_in[init][src] = 0; //jer je prema src simuliran REQUEST
				}
			}
			else if(snimka_t[init] < t_init && in[src] == 0){
				//neispravna FLOOD poruka za novu snimku
				int[] saljem = new int[]{init, t_init, tezina};
				super.sendMsg(src, "ECHO", Util.writeArray(saljem));
			}
			else if(snimka_t[init] == t_init && in[src] == 0){
				//neispravna FLOOD poruka za trenutnu snimku
				int[] saljem = new int[]{init, t_init, tezina};
				super.sendMsg(src, "ECHO", Util.writeArray(saljem));
			}
			else if(snimka_t[init] == t_init && in[src] == 1){
				//valjana FLOOD poruka za trenutnu snimku
				if(snimka_s[init] == false){
					int[] saljem = new int[]{init, t_init, tezina};
					super.sendMsg(src, "ECHO", Util.writeArray(saljem));
				}
				else if(snimka_s[init] == true){
					//dodaj da netko čeka na mene u simulaciji REQUEST-a, ali
					//se graf ne produljuje (već snimljeno da sam blokiran)
					//pa vrati težinu procesu pokretaču
					snimka_in[init][src] = 1;
					int[] saljem = new int[]{init, t_init, tezina};
					if(init == myId)
							short_salje_sam_sebi(Util.writeArray(saljem));
					else
						super.sendMsg(init, "SHORT", Util.writeArray(saljem));
				}
			}
			else if(snimka_t[init] > t_init){
				//snimci je istekao rok trajanja - zanemari to!
			}
		}
		
		
		
		else if(tag.equals("ECHO")){
			int[] dijeloviPoruke = new int[3];
			Util.readArray(m.getMessage(), dijeloviPoruke);
			
			int init = dijeloviPoruke[0]; //tko je pokrenuo ovo snimanje
			int t_init = dijeloviPoruke[1]; //kada je pokrenuo svoje snimanje
			int tezina = dijeloviPoruke[2]; //primljena težina
			
			if(snimka_t[init] > t_init){
				//ECHO za snimku kojoj je istekao rok trajanja - zanemari to!
			}
			else if(snimka_t[init] < t_init){
				//ECHO za još neviđenu snimku - ne može se dogoditi
			}
			else if(snimka_t[init] == t_init){
				//ECHO za trenutnu snimku
				snimka_out[init][src] = 0; //reduciraj brid prema tom sljedbeniku
				if(snimka_s[init] == false){ //već sam reduciran
					int[] saljem = new int[]{init, t_init, tezina};
					if(init == myId)
							short_salje_sam_sebi(Util.writeArray(saljem));
					else
						super.sendMsg(init, "SHORT", Util.writeArray(saljem));
				}
				else if(snimka_s[init] == true){
					--snimka_p[init]; //reduciraj (jedan manje treba u simulaciji)
					if(snimka_p[init] == 0){
						//upravo se reducira
						snimka_s[init] = false; //reducirano
						if(init == myId){
							//aha, ali ja sam to pokrenu i reducirao sam se
							//dakle, nisam u blokadi (jeee)
							DeclareNotDeadlocked();
							return;
						}
						
						
						
						int suma = 0; //izračunaj koliko imam prethodnika u snimci
						for(int i=0; i<snimka_in[init].length; ++i){
							if(snimka_in[init][i] == 1)
								++suma;
						}
						
						//npr. umjesto 1/3 šaljem 3 (ako suma=3, tj. imam 3 prethodnika u snimci)
						int[] saljem = new int[]{init, t_init, suma*tezina};
		
						for(int i=0; i<snimka_in[init].length; ++i){
							if(snimka_in[init][i] == 1)
								super.sendMsg(i, "ECHO", Util.writeArray(saljem));
						}
					}
					else if(snimka_p[init] != 0){
						//znači trebam još odgovora (tj. njihovih simulacija)
						int[] saljem = new int[]{init, t_init, tezina};
						if(init == myId)
							short_salje_sam_sebi(Util.writeArray(saljem));
						else
							super.sendMsg(init, "SHORT", Util.writeArray(saljem));
					}
				}
			}
		}
		
		
		else if(tag.equals("SHORT")){
			//primijetimo da ovo izvršava samo onaj tko je pokrenuo tu snimku
			//(dakle init u nastavku)
			
			int[] dijeloviPoruke = new int[3];
			Util.readArray(m.getMessage(), dijeloviPoruke);
			
			int init = dijeloviPoruke[0]; //tko je pokrenuo ovo snimanje
			int t_init = dijeloviPoruke[1]; //kada je pokrenuo svoje snimanje
			int tezina = dijeloviPoruke[2]; //primljena težina
			
			if(t_init < t_block){
				//prastara snimka - zanemari je!
			}
			else if (t_init > t_block){
				//poruka za još nepokrenutu snimku - nemoguće
			}
			else if(t_init == t_block && snimka_s[init] == false){
			    //za trenutno pokrenutu snimku
				//zanemari (init je već aktivan)
			}
			else if(t_init == t_block && snimka_s[init] == true){
				w = w + 1.0/tezina;
				if(w == 1){
					DeclareDeadlock();
				}
			}
		}
    }
	
	private void short_salje_sam_sebi(String poruka){
		int[] dijeloviPoruke = new int[3];
			Util.readArray(poruka, dijeloviPoruke);
			
			int init = dijeloviPoruke[0]; //tko je pokrenuo ovo snimanje
			int t_init = dijeloviPoruke[1]; //kada je pokrenuo svoje snimanje
			int tezina = dijeloviPoruke[2]; //primljena težina
			
			if(t_init < t_block){
				//prastara snimka - zanemari je!
			}
			else if (t_init > t_block){
				//poruka za još nepokrenutu snimku - nemoguće
			}
			else if(t_init == t_block && snimka_s[init] == false){
			    //za trenutno pokrenutu snimku
				//zanemari (init je već aktivan)
			}
			else if(t_init == t_block && snimka_s[init] == true){
				w = w + 1.0/tezina;
				if(w == 1){
					DeclareDeadlock();
				}
			}
	}
	
	private void DeclareNotDeadlocked() {
		Util.println("Nisam u blokadi!!!");
	}
	
	private void DeclareDeadlock() {
		Util.println("DEADLOCK!!!");
	}
}