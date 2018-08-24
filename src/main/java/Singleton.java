
import static fu.Tools.*;

public class Singleton {

	private Singleton() {}

	private static Singleton instance;

	public static Singleton getInstance() {
		if (instance == null) {
			//sleep(1);
			instance = new Singleton();
		}
		return instance;
	}
}