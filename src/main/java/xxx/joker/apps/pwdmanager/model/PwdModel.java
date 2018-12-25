package xxx.joker.apps.pwdmanager.model;

import org.apache.commons.lang3.StringUtils;
import xxx.joker.apps.pwdmanager.beans.Pwd;
import xxx.joker.apps.pwdmanager.common.Configs;
import xxx.joker.apps.pwdmanager.exceptions.ModelException;
import xxx.joker.libs.core.utils.JkFiles;
import xxx.joker.libs.core.utils.JkStreams;
import xxx.joker.libs.core.utils.JkStrings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Created by f.barbano on 19/11/2017.
 */
public class PwdModel implements IPwdModel {

//	public static final String FILE_HEADER = "KEY|USERNAME|PASSWORD|NOTES";
	private static final String PH_TAB = "##TAB##";
	private static final String PH_NEWLINE = "##NEWLINE##";
	private static final Path TEMP_FILE_PATH = Configs.TEMP_FOLDER.resolve(".generic.temp");

	private final Path pwdPath;
	private final String password;

	public static IPwdModel createModelInstance(Path pwdPath, String password) throws ModelException {
		return new PwdModel(pwdPath, password);
	}

	private PwdModel(Path pwdPath, String password) throws ModelException {
		this.pwdPath = pwdPath;
		this.password = password;

		if(!Files.exists(pwdPath)) {
			savePasswords(Collections.emptyList());
		}
	}

	@Override
	public List<Pwd> getPasswords() throws ModelException {
		if(!Files.exists(pwdPath)) {
			return Collections.emptyList();
		}

		try {
			EncryptionUtil.decryptFile(pwdPath, TEMP_FILE_PATH, EncryptionUtil.getMD5(password), true);
			List<String> lines = Files.readAllLines(TEMP_FILE_PATH);
			Files.delete(TEMP_FILE_PATH);
			lines.removeIf(line -> StringUtils.isBlank(line) || line.equals(FILE_HEADER));
			return JkStreams.map(lines, this::parsePwd);

		} catch (Exception e) {
			throw new ModelException(e);
		}
	}

	@Override
	public void savePasswords(List<Pwd> pwdList) throws ModelException {
		try {
			List<String> lines = JkStreams.map(pwdList, this::pwdToString);
			lines.add(0, JkStreams.join(FILE_HEADER, "|"));
			JkFiles.writeFile(TEMP_FILE_PATH, lines, true);
			EncryptionUtil.encryptFile(TEMP_FILE_PATH, pwdPath, EncryptionUtil.getMD5(password), true);
			Files.delete(TEMP_FILE_PATH);

		} catch (Exception e) {
			throw new ModelException(e);
		}
	}

	@Override
	public void changeEncryptionPwd(String newPassword) throws ModelException {
		try {
			EncryptionUtil.decryptFile(pwdPath, TEMP_FILE_PATH, EncryptionUtil.getMD5(password), true);
			EncryptionUtil.encryptFile(TEMP_FILE_PATH, pwdPath, EncryptionUtil.getMD5(newPassword), true);
			Files.delete(TEMP_FILE_PATH);
			
		} catch (Exception e) {
			throw new ModelException(e);
		}
	}

	@Override
	public Path getFilePath() {
		return pwdPath;
	}

	@Override
	public String getPassword() {
		return password;
	}

	private Pwd parsePwd(String line) {
		String[] split = JkStrings.splitAllFields(line, "|");
		if(split.length != 4) {
			throw new IllegalArgumentException(String.format("Wrong line format: %s", line));
		}
		Pwd pwd = new Pwd();
		pwd.setKey(split[0]);
		pwd.setUsername(split[1]);
		pwd.setPassword(split[2]);
		pwd.setNotes(split[3].replace(PH_TAB, "\t").replace(PH_NEWLINE, "\n"));
		return pwd;
	}

	private String pwdToString(Pwd pwd) {
		return String.format("%s|%s|%s|%s",
			pwd.getKey(),
			pwd.getUsername(),
			pwd.getPassword(),
			pwd.getNotes().replaceAll("\t", PH_TAB).replaceAll("\n", PH_NEWLINE)
		);
	}
}
