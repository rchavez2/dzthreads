
import fu.SqlData;
import fu.SyncList;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static fu.Tools.*;
import static java.util.stream.Collectors.toList;




public class Main {

	public static void main(String[] args) {

		test(Main::raceConditionAddToList);

	}

	static void noThreads() {
		int n = 30;

		for (int i = 0; i < n; i++) {
			String s = bcrypt(""+i);
			System.out.println(i + "/" + n + " - " + s);
		}
	}

	/**
	 * Ejecuta n tareas en n hilos.
	 */
	static void withThreads() {
		int n = 30;

		for (int i = 0; i < n; i++) {
			final int fi = i;
			Thread thread = new Thread(() -> {
				String result = bcrypt("" + fi);
				System.out.println(fi + "/" + n + " >> " + Thread.currentThread().getName() + " - " + result );
			});
			//thread.run();
			thread.start();
		}
	}

	/**
	 * Ejecuta n tareas, repartiendolas en varios hilos.
	 */
	static void executeRunnables() {

		ExecutorService executorService = Executors.newFixedThreadPool(4);
		int n = 30;

		for (int i = 0; i < n; i++) {
			final int fi = i;
			executorService.execute(() -> {
				String result = bcrypt("" + fi);
				System.out.println(fi + "/" + n + " >> " + Thread.currentThread().getName() + " - " + result);
			});
		}

		waitAll(executorService);
	}

	/**
	 * submit() regresa la tarea ejectutándose en forma de Future<T>.
	 * Para esperar su resultado se llama a get()
	 */
	static void submitCallables() {

		ExecutorService executorService = Executors.newFixedThreadPool(4);
		int n = 30;

		// Submit (start) tasks
		ArrayList<Future<String>> futures = new ArrayList<>();

		for (int i = 0; i < n; i++) {
			final int fi = i;
			Future<String> future = executorService.submit(
					() -> bcrypt("" + fi));
			futures.add(future);
		}

		// Wait for results (in order) and print them.
		try {
			for (int i = 0; i < futures.size(); i++) {
				Future<String> future = futures.get(i);
				String result = future.get();
				System.out.println(i + "/" + n + " >> " + Thread.currentThread().getName() + " - " + result);
			}
		}
		catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		waitAll(executorService);
	}

	/**
	 * Ejecuta n tareas usando stream.parallel
	 */
	static void parallel() {
		int n = 30;
		//System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "4");

		IntStream.range(0, n)
			.parallel()
			.forEach(i -> {
				String result = bcrypt("" + i);
				System.out.println(i + "/" + n + " >> " + Thread.currentThread().getName() + " - " + result );
			});
	}

	/**
	 * Cuando un hilo llama a barrier.await(), espera a que todos los hilos que admite la barrera terminen.
	 */
	static void barrier() {
		ExecutorService executor = Executors.newFixedThreadPool(4); // Debe ser el mismo número que admite CyclicBarrier.

		CyclicBarrier barrier = new CyclicBarrier(4, () -> System.out.println("\n|| Opening barrier. ||\n"));

		List<Future<String>> futures = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			futures.add(
					executor.submit(() -> {

						Random random = new Random();
						sleep(random.nextInt(20)*100+100);

						System.out.println("Completed my task, waiting for others.");
						barrier.await();
						System.out.println("I passed the barrier.");

						return "ok";
					}));
		}

		futures.forEach(future -> {
			try {
				String result = future.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		});

		waitAll(executor);
	}


	/**
	 * ArrayList no es un objeto thread-safe. Las inserciones pueden no hacerse
	 * o se puede acceder a un índice fuera de rango.
	 */
	static void raceConditionArrayList() {

		ArrayList<Integer> results = new ArrayList<>();

		// CopyOnWriteArrayList es thread-safe, funciona, pero cada vez que se modifica
		// la lista hace una copia completa de todos los elementos (para que otros
		// hilos puedan seguir leyéndola sin errores). Sirve para cuando se necesitan
		// muchas lecturas y muy pocas escrituras. Si son muchas escrituras (como este caso),
		// el desempeño es muy malo.
		//CopyOnWriteArrayList<String> results = new CopyOnWriteArrayList<>();

		IntStream.range(0, 10_000)
				.parallel()
				.map(i -> i * 2)
				.forEach(i -> results.add(i));

		System.out.println(results.size());
	}

	/**
	 * El singleton debe regresar una sola instancia. Aquí varios hilos tratan de leer
	 * el singleton, y como todavía no se ha creado, se crean varios diferentes.
	 */
	static void raceConditionSingleton() {

		IntStream.range(1, 5)
			.parallel()
			.forEach(x ->
				System.out.println(x + ": " + Singleton.getInstance().hashCode()));
	}


	/**
	 * Stack tampoco es una colección thread-safe. Varios hilos quitan
	 * elementos hasta llegar a 10, pero se pueden exceder o algunas veces
	 * fallar en quitar elementos.
	 */
	static void raceConditionStack() {
		ExecutorService executorService = Executors.newFixedThreadPool(4);

		Stack<Integer> stack = new Stack<>();
		for (int i = 0; i < 1_000; i++) {
			stack.push(i);
		}

		while (stack.size() > 10) {
			executorService.execute(() -> stack.pop());
		}

		waitAll(executorService);
		System.out.println("\n" + stack.size());
	}


	/**
	 * Repartir varios ítems de una lista al azar. Con hilos puede
	 * que a varios le toque un ítem repetido.
	 */
	static void raceConditionItems() {

		ExecutorService executor = Executors.newFixedThreadPool(4);

		ArrayList<String> items = new ArrayList<>(
				Arrays.asList("a,b,c,d,e,f,g,h,i,j".split(","))
		);

		Random random = new Random();

		for (int i = 0; i < 10; i++) {
			final int fi = i;
			executor.execute(() -> {
				String item = items.get(random.nextInt(items.size()));
				System.out.println(fi + ": " + item);
				bcrypt("wait");
				items.remove(item);
			});
		}

		waitAll(executor);
	}

	// Igual que raceConditionArrayList.
	static void raceConditionAddToList() {
		ExecutorService executor = Executors.newFixedThreadPool(8);

		ArrayList<Integer> numbers = new ArrayList<>();

		for (int i = 0; i < 1_000; i++) {
			final int fi = i;
			executor.submit(() -> numbers.add(fi));
		}

		waitAll(executor);

		System.out.println(numbers.size());
	}


	/**
	 * add(int) llama a add(String), y este llama a log().
	 * Con tres métodos y dos llaves, puede que dos hilos se queden
	 * esperando a que el otro suelte la llave y nunca llegar a log().
	 */
	static void deadlock() {

		DeadlockExample de = new DeadlockExample();

		Thread t1 = new Thread(() -> de.add(10));
		Thread t2 = new Thread(() -> de.add("im string"));

		t1.start();
		t2.start();

		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	/**
	 * La clase SyncList trata de agregar sincronización al ArrayList para evitar
	 * errores de hilos. Pero sincronizar la lectura de la lista (getLast()) no es
	 * necesario y esto hace perder el beneficio de los hilos.
	 */
	static void testSyncList() {
		SyncList list = new SyncList();

		ExecutorService executor = Executors.newFixedThreadPool(8);

		for (int i = 0; i < 100; i++) {
			final int fi = i;
			executor.execute(() -> list.add(fi));
		}

		sleep(2);

		for (int i = 0; i < 100; i++) {
			executor.execute(() -> list.getLast());
		}

		waitAll(executor);
		System.out.println(list.size());
	}

	/**
	 * Buscar números primos de 10 millones de números.
	 * 1. Sin hilos
	 * 2. Con parallel y usando una lista compartida y sincronizada.
	 * 3. Con parallel y sin variables compartidas.
	 */
	static void findPrimes() {
		final int from = 0;
		final int to = 10_000_000;
		test(() -> primes(from, to));
		test(() -> primesParallel(from, to));
		test(() -> primesParallelCollected(from, to));
	}

	/**
	 * Cuenta hrefs de varios documentos. El uso de CPU se mantiene bajo porque
	 * el acceso al disco es cuello de botella.
	 * @throws IOException
	 */
	static void countHrefs() throws IOException {
		ExecutorService executorService = Executors.newFixedThreadPool(20);

		List<Path> files = Files.walk(Paths.get("C:\\Capture\\BMWAIR\\NA\\F32"))
				.filter(x -> x.toString().endsWith(".html"))
				.collect(toList());

		for (int i = 0; i < files.size(); i++) {
			final int count = i;
			Path f = files.get(i);
			executorService.execute(() -> {
				String text = readFile(f);
				int hrefs = match(text, "href=\".*?\"").size();
				System.out.print("\r" + count + ":" + files.size() + " - " + hrefs);
			});
		}

		waitAll(executorService);
	}

	/**
	 * Obtiene los títulos por ID de dos fuentes, la DB y archivos.
	 * El uso de CPU es bajo, pero esperar a que la DB regrese mientras se leen los archivos
	 * ayuda a ahorrar tiempo.
	 */
	static void sqlAndFiles() {

		ExecutorService executor = Executors.newFixedThreadPool(4);

		Future<SqlData> sqlFuture =
			executor.submit(() -> {
				System.out.println("Running SQL query");
				SqlData result = sql(
						"select article_id, title " +
								"from ca_na_bmw_dnr_ainf " +
								"where model = 'F39' " +
								"group by article_id;");
				System.out.println("Query completed");
				return result;
			});

		System.out.println("Getting HTML titles");
		try {
			List<String[]> fileNamesAndTitles =
				Files.walk(Paths.get("C:\\Capture\\BMWAIR\\NA\\F39\\DnR\\content"))
					.filter(x -> x.toString().endsWith(".html"))
					.limit(2000)
					.map(h -> new String[] {
							match(h.getFileName().toString(), "[^\\.]+").get(0),
							Jsoup.parse(readFile(h)).select(".air-breadcrumb-title").text()
					}).collect(toList());
		}
		catch (IOException e) {}
		System.out.println("HTML titles completed");

		try {
			// Espera el resultado del query. Si ya terminó, regresa inmediatamente.
			SqlData result = sqlFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		waitAll(executor);
	}










	static String getBodyWithJsoup(String url) {
		try {
			return Jsoup.connect(url).execute().body();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}



	static boolean isPrime(int n) {
		if (n % 2 == 0 && n != 2) return false;
		int root = (int)Math.ceil(Math.sqrt(n));
		for (int i = 3; i <= root ; i+=2) {
			if (n % i == 0 && n != i) return false;
		}
		return true;
	}

	static List<Integer> primes(int from, int to) {
		ArrayList<Integer> result = new ArrayList<>();

		for (int i = from; i <= to; ++i) {
			if (isPrime(i)) {
				result.add(i);
			}
		}

		return result;
	}

	static List<Integer> primesParallel(int from, int to) {
		ArrayList<Integer> result = new ArrayList<>();

		IntStream.range(from, to).parallel().forEach(i -> {
			if (isPrime(i)) {
				synchronized (result) {
					result.add(i);
				}
			}
		});

		return result;
	}

	static List<Integer> primesParallelCollected(int from, int to) {
		return IntStream.range(from, to)
				.parallel()
				.filter(i -> isPrime(i))
				.boxed()
				.collect(toList());
	}


}
