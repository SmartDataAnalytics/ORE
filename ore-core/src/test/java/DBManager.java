import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.core.owl.Property;
import org.ini4j.IniPreferences;
import org.ini4j.InvalidFileFormatException;

public class DBManager {

	private Connection conn;

	public DBManager() {
		initDBConnection();
	}

	private void initDBConnection() {
		try {
			InputStream is = this.getClass().getClassLoader().getResourceAsStream("db_settings.ini");
			Preferences prefs = new IniPreferences(is);
			String dbServer = prefs.node("database").get("server", null);
			String dbName = prefs.node("database").get("name", null);
			String dbUser = prefs.node("database").get("user", null);
			String dbPass = prefs.node("database").get("pass", null);

			Class.forName("com.mysql.jdbc.Driver");
			String url = "jdbc:mysql://" + dbServer + "/" + dbName;
			conn = DriverManager.getConnection(url, dbUser, dbPass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InvalidFileFormatException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void createTable(String name) {
		try {
			java.sql.Statement st = conn.createStatement();
			st.execute("CREATE TABLE IF NOT EXISTS " + name + "(" + "property VARCHAR(255),"
					+ "nrOfViolations BIGINT," + "sampleViolation VARCHAR(2000),"
					+ "sampleViolationHTML VARCHAR(2000)," + "correct BOOLEAN NOT NULL DEFAULT 0,"
					+ "PRIMARY KEY(property)) DEFAULT CHARSET=utf8");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void addEntry(String name, Property p, long nrOfViolations, String sampleViolation,
			String sampleViolationHTML) {
		try {
			PreparedStatement ps = conn.prepareStatement("INSERT INTO " + name + " ("
					+ "property, nrOfViolations, sampleViolation, sampleViolationHTML) " + "VALUES(?,?,?,?)");
			ps.setString(1, p.getURI().toString());
			ps.setLong(2, nrOfViolations);
			ps.setString(3, sampleViolation);
			ps.setString(4, sampleViolationHTML);

			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void setCorrect(Property p, String tableName, boolean correct) {
		try {
			PreparedStatement ps = conn
					.prepareStatement("UPDATE " + tableName + " " + "SET correct=? WHERE property=?");
			ps.setBoolean(1, correct);
			ps.setString(2, p.getURI().toString());

			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public List<Property> loadProperties(String tableName) {
		List<Property> properties = new ArrayList<Property>();
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT property FROM " + tableName);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				properties.add(new ObjectProperty(rs.getString(1)));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return new ArrayList<Property>(new TreeSet<Property>(properties));
	}

	public boolean isCorrect(Property p, String tableName) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT correct FROM " + tableName + " WHERE property=?");
			ps.setString(1, p.getURI().toString());

			ResultSet rs = ps.executeQuery();
			rs.first();
			return rs.getBoolean(1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public String getSampleViolationHTML(Property p, String tableName) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT sampleViolationHTML FROM " + tableName
					+ " WHERE property=?");
			ps.setString(1, p.getURI().toString());

			ResultSet rs = ps.executeQuery();
			rs.first();
			return rs.getString(1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
}
