package xxx.joker.apps.pwdmanager.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import xxx.joker.apps.pwdmanager.common.Configs;
import xxx.joker.apps.pwdmanager.controller.PwdController;
import xxx.joker.libs.core.utils.JkEncryption;
import xxx.joker.libs.core.utils.JkFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

/**
 * Created by f.barbano on 19/11/2017.
 */
public class PwdGUI extends Application {


	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		manageAppData();
		launch();
	}


	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/pwdView.fxml"));
		PwdController pwdController = new PwdController(primaryStage);
		fxmlLoader.setController(pwdController);
		BorderPane pwdPane = fxmlLoader.load();

		Scene scene = new Scene(pwdPane);

		primaryStage.setScene(scene);

		primaryStage.setMaximized(true);
		primaryStage.show();

//		ScenicView.show(scene);
	}

	@Override
	public void stop() throws Exception {
		JkFiles.removeDirectory(Configs.TEMP_FOLDER);
	}

	private static void manageAppData() throws IOException, NoSuchAlgorithmException {
		if(JkFiles.areEquals(Configs.ROOT_FOLDER, Paths.get(""))) {
			String actualHash = JkEncryption.getMD5(JkFiles.getLauncherPath(PwdGUI.class));
			// Check if running app is new or not
			boolean newVersion = true;
			if (Files.exists(Configs.MD5SUM_JAR_FILE)) {
				String oldHash = Files.readAllLines(Configs.MD5SUM_JAR_FILE).get(0);
				newVersion = !oldHash.equals(actualHash);
			}

			if(newVersion) {
				JkFiles.removeDirectory(Configs.DATA_FOLDER);
				JkFiles.writeFile(Configs.MD5SUM_JAR_FILE, actualHash, false);
			}
		}
	}

}
