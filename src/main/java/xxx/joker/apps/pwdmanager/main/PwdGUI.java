package xxx.joker.apps.pwdmanager.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.scenicview.ScenicView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xxx.joker.apps.pwdmanager.common.Configs;
import xxx.joker.apps.pwdmanager.controller.PwdController;
import xxx.joker.libs.core.utils.JkEncryption;
import xxx.joker.libs.core.utils.JkFiles;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by f.barbano on 19/11/2017.
 */
public class PwdGUI extends Application {

	public static final Logger logger = LoggerFactory.getLogger(PwdGUI.class);

	public static boolean scenicView;

	public static void main(String[] args) throws IOException {
		manageAppData();
		scenicView = args.length == 1 && "-sv".equals(args[0]);
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
		primaryStage.setTitle("F1 VIDEO PLAYER");
		primaryStage.show();

		if(scenicView) {
			ScenicView.show(scene);
			primaryStage.setOnCloseRequest(e -> Platform.exit());
		}
	}

	@Override
	public void stop() throws Exception {
		logger.info("Closing app");
		if(Files.exists(Configs.TEMP_FOLDER)) {
			JkFiles.removeDirectory(Configs.TEMP_FOLDER);
			logger.info("Removed folder {}", Configs.TEMP_FOLDER);
		}
	}

	private static void manageAppData() throws IOException {
		if(!Configs.RUN_ON_IDE) {
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
