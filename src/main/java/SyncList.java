package fu;

import java.util.ArrayList;

import static fu.Tools.sleep;

public class SyncList {

	ArrayList<Integer> list = new ArrayList<>();


	public synchronized void add(int n) {
		sleep(10);
		list.add(n);
	}

	// El código que no está sincronizado puede aprovechar varios hilos y núcleos.
	// Quitar sincronized de este método muestra que se puede obtener más velocidad
	// (aunque en este caso aún hay riesgo de race conditions).
	public synchronized int getLast() {
		sleep(10);
		return list.get(list.size() -1);
	}

	public int size() {
		return list.size();
	}
}
