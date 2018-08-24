package fu;

import java.util.ArrayList;
import java.util.HashMap;

public class SqlData extends ArrayList<HashMap<String,String>> {

	public String get(int index, String column) {
		return this.get(index).get(column);
	}
}
