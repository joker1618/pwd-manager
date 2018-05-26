package xxx.joker.apps.pwdmanager.beans;

import javafx.beans.property.SimpleStringProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;

/**
 * Created by f.barbano on 19/11/2017.
 */
public class Pwd implements Comparator<Pwd> {

	private SimpleStringProperty key;
	private SimpleStringProperty username;
	private SimpleStringProperty password;
	private SimpleStringProperty notes;

	public Pwd() {
		this.key = new SimpleStringProperty("");
		this.username = new SimpleStringProperty("");
		this.password = new SimpleStringProperty("");
		this.notes = new SimpleStringProperty("");
	}

	@Override
	public int compare(Pwd o1, Pwd o2) {
		return o1.compareTo(o2);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Pwd)) return false;

		Pwd pwd = (Pwd) o;

		if (getKey() != null ? !getKey().equalsIgnoreCase(pwd.getKey()) : pwd.getKey() != null) return false;
		if (getUsername() != null ? !getUsername().equals(pwd.getUsername()) : pwd.getUsername() != null) return false;
		if (getPassword() != null ? !getPassword().equals(pwd.getPassword()) : pwd.getPassword() != null) return false;
		return getNotes() != null ? getNotes().equals(pwd.getNotes()) : pwd.getNotes() == null;
	}

	@Override
	public int hashCode() {
		int result = getKey() != null ? getKey().toLowerCase().hashCode() : 0;
		result = 31 * result + (getUsername() != null ? getUsername().hashCode() : 0);
		result = 31 * result + (getPassword() != null ? getPassword().hashCode() : 0);
		result = 31 * result + (getNotes() != null ? getNotes().hashCode() : 0);
		return result;
	}

	public String getKey() {
		return key.get();
	}

	public SimpleStringProperty keyProperty() {
		return key;
	}

	public void setKey(String key) {
		this.key.set(key);
	}

	public String getUsername() {
		return username.get();
	}

	public SimpleStringProperty usernameProperty() {
		return username;
	}

	public void setUsername(String username) {
		this.username.set(username);
	}

	public String getPassword() {
		return password.get();
	}

	public SimpleStringProperty passwordProperty() {
		return password;
	}

	public void setPassword(String password) {
		this.password.set(password);
	}

	public String getNotes() {
		return notes.get();
	}

	public SimpleStringProperty notesProperty() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes.set(notes);
	}

	public int compareTo(Pwd o) {
		return StringUtils.compareIgnoreCase(getKey(), o.getKey());
	}
}
