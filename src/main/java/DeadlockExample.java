import java.time.Instant;
import java.util.Stack;

public class DeadlockExample {

	Stack<String> ids = new Stack<>();

	Object lock = new Object();
	Object lock2 = new Object();


	public void add(int n) {
		synchronized (lock) {
			System.out.println(Thread.currentThread().getName() + " calls add(int)");
			String s = n + "";
			add(s);
		}
	}

	public void add(String s) {

		synchronized (lock2) {
			System.out.println(Thread.currentThread().getName() + " calls add(String)");
			ids.add(s);
			log(s);
		}
	}

	void log(Object s) {
		synchronized (lock) {
			System.out.println(Thread.currentThread().getName() + ": " +  s);
		}
	}

}
