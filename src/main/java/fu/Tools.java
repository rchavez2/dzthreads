package fu;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tools {

	public static String bcrypt(String text) {
		return BCrypt.withDefaults().hashToString(12,  text.toCharArray());
	}


	public static String readFile(Path file) {
		try {
			return String.join("\n", Files.readAllLines(file));
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}

	public static ArrayList<String> match(String text, String pattern) {
		Pattern pat = Pattern.compile(pattern);
		Matcher matcher = pat.matcher(text);
		ArrayList<String> matches = new ArrayList<>();
		while (matcher.find()) {
			matches.add(matcher.group());
		}
		return matches;
	}

	public static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void parallelThreads(int n) {
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", ""+n);
	}

	public static void waitAll(ExecutorService executorService) {
		try {
			executorService.shutdown();
			executorService.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			throw new RuntimeException();
		}
	}

	public static fu.SqlData sql(String query) {

		Connection connection = null;
		try {
			connection = DriverManager.getConnection(
					"jdbc:mysql://ha-datacap-prod.alldata.com/cca_dz" +
							"?user=cca_dz_usr&password=d085d3c0");
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			ResultSetMetaData metaData = resultSet.getMetaData();
			int cols = metaData.getColumnCount();
			fu.SqlData rows = new fu.SqlData();
			while (resultSet.next()) {
				HashMap<String, String> row = new HashMap<>();
				for (int i = 1; i <= cols; i++) {
					row.put(metaData.getColumnName(i), resultSet.getString(i));
				}
				rows.add(row);
			}

			if (resultSet != null) {
				resultSet.close();
			}
			statement.close();
			connection.close();

			return rows;

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void test(Runnable r) {
		test(x -> {
			r.run();
			return "";
		});
	}

	public static void test(int n, Runnable r) {
		Instant start = Instant.now();
		for (int i = 0; i < n; i++) {
			Instant lap = Instant.now();
			r.run();
			System.out.println(Duration.between(lap, Instant.now()).toMillis());
		}
		Instant end = Instant.now();
		long time = Duration.between(start, end).toMillis();
		System.out.println("\n---------------\nTime: " + time);
	}

	public static <T,R> R test(Function<T,R> f) {
		Instant start = Instant.now();
		R result = f.apply(null);
		Instant end = Instant.now();
		long time = Duration.between(start, end).toMillis();
		System.out.println("\n---------------\nTime: " + time);
		return result;
	}
}
