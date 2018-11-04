package xxx.joker.apps.pwdmanager.common;

import xxx.joker.apps.pwdmanager.main.PwdGUI;
import xxx.joker.libs.core.utils.JkFiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by f.barbano on 09/12/2017.
 */
public class Configs {

	public static final Path ROOT_FOLDER;
	static {
		Path launcherPath = JkFiles.getLauncherPath(PwdGUI.class);
		boolean isRunOnIDE = !Files.isRegularFile(launcherPath) || !launcherPath.getFileName().toString().endsWith(".jar");
		if(!isRunOnIDE) {
			ROOT_FOLDER = JkFiles.getParent(launcherPath);
		} else {
			ROOT_FOLDER = Paths.get("");
		}
	}

	public static final Path DATA_FOLDER = ROOT_FOLDER.resolve(".appData");
	public static final Path MD5SUM_JAR_FILE = DATA_FOLDER.resolve(".md5sum");
	public static final Path TEMP_FOLDER = ROOT_FOLDER.resolve(".temp");



}
