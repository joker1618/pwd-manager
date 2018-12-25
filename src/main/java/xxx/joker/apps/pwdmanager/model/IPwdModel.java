package xxx.joker.apps.pwdmanager.model;

import xxx.joker.apps.pwdmanager.beans.Pwd;
import xxx.joker.apps.pwdmanager.exceptions.ModelException;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Created by f.barbano on 19/11/2017.
 */
public interface IPwdModel {

	List<String> FILE_HEADER = Arrays.asList("KEY", "USERNAME", "PASSWORD", "NOTES");

	List<Pwd> getPasswords() throws ModelException;

	void savePasswords(List<Pwd> pwdList) throws ModelException;

	void changeEncryptionPwd(String newPassword) throws ModelException;

	Path getFilePath();
	String getPassword();

}
