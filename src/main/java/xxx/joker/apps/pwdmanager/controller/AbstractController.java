package xxx.joker.apps.pwdmanager.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Created by f.barbano on 19/11/2017.
 */
abstract class AbstractController {

	private	final Window mainWindow;

	AbstractController(Window window) {
		this.mainWindow = window;
	}

	protected Path chooseFile(String title) {
		return chooseFile(title, null);
	}
	protected Path chooseFile(String title, Path initialPath) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		if(initialPath != null) {
			Path init = Files.isDirectory(initialPath) ? initialPath : initialPath.toAbsolutePath().getParent();
			fileChooser.setInitialDirectory(init.toFile());
		}
		File file = fileChooser.showOpenDialog(mainWindow);
		return file == null ? null : file.toPath();
	}

	protected Path saveFile(String title) {
		return saveFile(title, null);
	}
	protected Path saveFile(String title, Path initialPath) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		if(initialPath != null) {
			Path init = Files.isDirectory(initialPath) ? initialPath : initialPath.toAbsolutePath().getParent();
			fileChooser.setInitialDirectory(init.toFile());
		}
		File file = fileChooser.showSaveDialog(mainWindow);
		return file == null ? null : file.toPath();
	}

	protected String askUserInput(String title, String header, String contentLabel) {
		return askUserInput(title, header, contentLabel, str -> true, "");
	}
	protected String askUserInput(String title, String header, String contentLabel, Predicate<String> checkInput, String checkInfo) {
		boolean doInputCheck = true;
		String resp = null;

		while(doInputCheck) {
			TextInputDialog dialog = new TextInputDialog();
			dialog.setTitle(title);
			dialog.setHeaderText(header);
			dialog.setContentText(contentLabel);
			Optional<String> result = dialog.showAndWait();
			if(result.isPresent()) {
				if(checkInput.test(result.get())) {
					resp = result.get();
					doInputCheck = false;
				} else {
					alertWarning(StringUtils.isBlank(checkInfo) ? "Invalid input" : checkInfo, null);
				}
			} else {
				doInputCheck = false;
			}
		}

		return resp;
	}


	protected void alertError(Throwable t) {
		alertError(t.getMessage(), Arrays.toString(t.getStackTrace()));
	}
	protected void alertError(String header, Throwable t) {
		if(t == null) {
			displayAlert(AlertType.ERROR, header, null);
		} else {
			displayAlert(AlertType.ERROR, header, "%s\n%s", t.getMessage(), Arrays.toString(t.getStackTrace()));
		}
	}
	protected void alertError(String header, String content, Object... params) {
		displayAlert(AlertType.ERROR, header, content, params);
	}

	protected void alertWarning(String header, String content, Object... params) {
		displayAlert(AlertType.WARNING, header, content, params);
	}

	protected void alertInfo(String header, String content, Object... params) {
		displayAlert(AlertType.INFORMATION, header, content, params);
	}

	protected boolean alertConfirmation(String header, String content, Object... params) {
		Optional<ButtonType> buttonType = displayAlert(AlertType.CONFIRMATION, header, content, params);
		return buttonType.isPresent() && buttonType.get() == ButtonType.OK;
	}

	private Optional<ButtonType> displayAlert(AlertType alertType, String header, String contentFormat, Object... contentParams) {
		Alert alert = new Alert(alertType);
		alert.setTitle(alertType.name());
		alert.setHeaderText(header);
		alert.setContentText(contentFormat == null ? null : String.format(contentFormat, contentParams));
		return alert.showAndWait();
	}

}
