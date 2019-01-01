package xxx.joker.apps.pwdmanager.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Window;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xxx.joker.apps.pwdmanager.beans.Pwd;
import xxx.joker.apps.pwdmanager.common.Configs;
import xxx.joker.apps.pwdmanager.exceptions.ModelException;
import xxx.joker.apps.pwdmanager.model.IPwdModel;
import xxx.joker.apps.pwdmanager.model.PwdModel;
import xxx.joker.libs.core.format.JkColumnFmtBuilder;
import xxx.joker.libs.core.utils.JkFiles;
import xxx.joker.libs.core.utils.JkStreams;
import xxx.joker.libs.excel.JkSheetXSSF;
import xxx.joker.libs.excel.JkWorkbookXSSF;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by f.barbano on 19/11/2017.
 */
public class PwdController extends AbstractController implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(PwdController.class);
	private static final Path RECENT_OPENED_PATH = Configs.DATA_FOLDER.resolve(".recentOpened");

	private enum ViewStatus { NoPwd, ReadOnly, Edit}

	@FXML
	private MenuItem itemNewFile;
	@FXML
	private MenuItem itemOpenFile;
	@FXML
	private MenuItem itemSaveAs;
	@FXML
	private MenuItem itemChangePwd;
	@FXML
	private MenuItem itemExportClearFile;
	@FXML
	private MenuItem itemExportClearExcel;
	@FXML
	private Menu menuRecents;
	@FXML
	private Label lblPwdPath;

	@FXML
	private TextField txtFilter;
	@FXML
	private TableView<Pwd> tablePwd;
	@FXML
	private TableColumn<Pwd, String> colKey;
	@FXML
	private TableColumn<Pwd, String> colUsername;
	@FXML
	private TableColumn<Pwd, String> colPassword;
	@FXML
	private TableColumn<Pwd, String> colNotes;

	@FXML
	private TextField fieldSelKey;
	@FXML
	private TextField fieldSelUsername;
	@FXML
	private TextField fieldSelPassword;
	@FXML
	private TextArea txtAreaSelNotes;

	@FXML
	private TextField fieldEditKey;
	@FXML
	private TextField fieldEditUsername;
	@FXML
	private TextField fieldEditPassword;
	@FXML
	private TextArea txtAreaEditNotes;

	@FXML
	private Button btnAdd;
	@FXML
	private Button btnEdit;
	@FXML
	private Button btnSave;
	@FXML
	private Button btnCancel;
	@FXML
	private Button btnDelete;

	private IPwdModel model;
	private Pwd actualEditPwd;

	private ObservableList<MenuItem> itemRecents = FXCollections.observableArrayList();

	private final ObservableList<Pwd> tableData = FXCollections.observableArrayList();
	private final FilteredList<Pwd> filteredList = new FilteredList<>(tableData);
	private final SimpleObjectProperty<Path> pwdPath = new SimpleObjectProperty<>();
	private final SimpleObjectProperty<ViewStatus> viewStatus = new SimpleObjectProperty<>();

	private final SimpleObjectProperty<Pwd> selectedPwd = new SimpleObjectProperty<>();

	public PwdController(Window window) {
		super(window);
	}


	@Override
	public void initialize(URL location, ResourceBundle resources) {
		viewStatus.setValue(ViewStatus.NoPwd);

		// Password table
		initTableView();

		// Menu items
		itemNewFile.setOnAction(this::doCreateNewFile);
		itemOpenFile.setOnAction(this::doOpenFilePath);
		itemSaveAs.setOnAction(this::doSaveFileAs);
		itemChangePwd.setOnAction(this::doChangeEncryptionPwd);
		itemExportClearFile.setOnAction(this::doExportClearFile);
		itemExportClearExcel.setOnAction(this::doExportClearExcel);

		BooleanBinding pwdPathIsNull = Bindings.createBooleanBinding(() -> pwdPath.get() == null, pwdPath);
		itemSaveAs.disableProperty().bind(pwdPathIsNull);
		itemChangePwd.disableProperty().bind(pwdPathIsNull);
		itemExportClearFile.disableProperty().bind(pwdPathIsNull);
		itemExportClearExcel.disableProperty().bind(pwdPathIsNull);

		setRecentOpenedFromFile();

		// Bind filter field
		txtFilter.textProperty().addListener((observable, oldValue, newValue) -> {
			filteredList.setPredicate(pwd -> {
				if(StringUtils.isBlank(newValue)) {
					return true;
				}

				return StringUtils.containsIgnoreCase(pwd.getKey(), newValue)
					|| StringUtils.containsIgnoreCase(pwd.getUsername(), newValue)
					|| StringUtils.containsIgnoreCase(pwd.getPassword(), newValue)
					|| StringUtils.containsIgnoreCase(pwd.getNotes(), newValue);
			});
		});
		txtFilter.disableProperty().bind(Bindings.createBooleanBinding(() -> pwdPath.get() == null || viewStatus.get() == ViewStatus.Edit, pwdPath, viewStatus));
		filteredList.setPredicate(pwd -> StringUtils.containsIgnoreCase(pwd.getKey(), txtFilter.getText()));

		// Label file path
		lblPwdPath.textProperty().bind(Bindings.createStringBinding(() -> pwdPath.get() == null ? "---" : pwdPath.get().toAbsolutePath().toString(), pwdPath));

		// Buttons disable property binding
		initButtons();

		// Fields init
		selectedPwd.bind(tablePwd.getSelectionModel().selectedItemProperty());
		selectedPwd.addListener((observable, oldValue, newValue) -> updateSelectedPwdFields());

		// Bind editable property
		BooleanBinding disableBind = Bindings.createBooleanBinding(() -> viewStatus.get() != ViewStatus.Edit, viewStatus);
		fieldEditKey.disableProperty().bind(disableBind);
		fieldEditUsername.disableProperty().bind(disableBind);
		fieldEditPassword.disableProperty().bind(disableBind);
		txtAreaEditNotes.disableProperty().bind(disableBind);

		viewStatus.addListener((observable, oldValue, newValue) -> {
		    if(newValue != oldValue) {
		        if(newValue == ViewStatus.Edit) {
		            actualEditPwd = selectedPwd.get();
		            fillEditSection(actualEditPwd);
                } else {
		            actualEditPwd = null;
		            clearEditSection();
                }
            }
        });
	}

    private void fillEditSection(Pwd pwd) {
        fieldEditKey.setText(pwd.getKey());
        fieldEditUsername.setText(pwd.getUsername());
        fieldEditPassword.setText(pwd.getPassword());
        txtAreaEditNotes.setText(pwd.getNotes());
    }

    private void clearEditSection() {
        fieldEditKey.setText("");
        fieldEditUsername.setText("");
        fieldEditPassword.setText("");
        txtAreaEditNotes.setText("");
    }

	private void initTableView() {
		tablePwd.setItems(filteredList);
		tablePwd.setEditable(false);
		tablePwd.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

		// Columns
		colKey.setCellValueFactory(new PropertyValueFactory<>("key"));
		colKey.setCellFactory(TextFieldTableCell.forTableColumn());
		colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
		colUsername.setCellFactory(TextFieldTableCell.forTableColumn());
		colPassword.setCellValueFactory(new PropertyValueFactory<>("password"));
		colPassword.setCellFactory(TextFieldTableCell.forTableColumn());
		colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));
		colNotes.setCellFactory(TextFieldTableCell.forTableColumn());
	}

	private void initButtons() {
		// Disabled property binding
        BooleanBinding isNotReadOnly = Bindings.createBooleanBinding(() -> viewStatus.get() != ViewStatus.ReadOnly, viewStatus);
        BooleanBinding isNotEdit = Bindings.createBooleanBinding(() -> viewStatus.get() != ViewStatus.Edit, viewStatus);
		btnAdd.disableProperty().bind(isNotReadOnly);
		btnEdit.disableProperty().bind(isNotReadOnly);
		btnDelete.disableProperty().bind(isNotReadOnly);
		btnSave.disableProperty().bind(isNotEdit);
		btnCancel.disableProperty().bind(isNotEdit);

		// Actions
		btnAdd.setOnAction(this::doAddPassword);
		btnEdit.setOnAction(this::doEditPassword);
		btnDelete.setOnAction(this::doDeletePassword);
		btnSave.setOnAction(this::doSaveChanges);
		btnCancel.setOnAction(this::doCancelChanges);
	}
	private void updateSelectedPwdFields() {
		// Bind text
		Pwd pwd = selectedPwd.get();
		fieldSelKey.setText(pwd == null ? "" : pwd.getKey());
		fieldSelUsername.setText(pwd == null ? "" : pwd.getUsername());
		fieldSelPassword.setText(pwd == null ? "" : pwd.getPassword());
		txtAreaSelNotes.setText(pwd == null ? "" : pwd.getNotes());
	}

	private void doCreateNewFile(ActionEvent event) {
		Path path = super.saveFile("Choose output file");
		if(path == null) 	return;
		path = path.toAbsolutePath();

		if(model != null && JkFiles.areEquals(model.getFilePath(), path)) {
			super.alertInfo("Password file already open", null);
			return;
		}

		try {
			String password = askEncryptionPwd(true);
			if(password != null) {
				setPwdFile(path, password);
				tableData.setAll(model.getPasswords());
				super.alertInfo("New pwd file created", "File: %s\nPassword: %s", path, password);
			}

		} catch (ModelException e) {
			super.alertError("Error while creating new file", "File %s", path.toString());
		}
	}

	private void setPwdFile(Path filePath, String password) throws ModelException {
		this.model = PwdModel.createModelInstance(filePath, password);
		registerRecentPath(filePath);
		pwdPath.setValue(filePath);
		viewStatus.setValue(ViewStatus.ReadOnly);
	}

	private void doOpenFilePath(ActionEvent event) {
		Path initPath = model == null ? null : model.getFilePath();
		Path path = super.chooseFile("Choose password file", initPath);
		doOpenPwdPath(path);
	}

	private void doSaveFileAs(ActionEvent event) {
		Path newPath = super.saveFile("Select file path", model.getFilePath());
		if(newPath == null) 	return;
		newPath = newPath.toAbsolutePath();

		if(model != null && JkFiles.areEquals(model.getFilePath(), newPath)) {
			super.alertInfo("Password file already open", null);
			return;
		}

		try {
			JkFiles.copyFile(pwdPath.get(), newPath, true);
			String password = model.getPassword();
			setPwdFile(newPath, password);

		} catch (ModelException e) {
			super.alertError("Error while saving password", "File %s", newPath.toString());
		}
	}

	private void doChangeEncryptionPwd(ActionEvent event) {
		try {
			String newPassword = askEncryptionPwd(true);
			if(newPassword != null) {
				String oldPwd = model.getPassword();
				model.changeEncryptionPwd(newPassword);
				super.alertInfo("Password changed", "File: %s\nOld pwd: %s\nNew pwd: %s", model.getFilePath().toString(), oldPwd, newPassword);
			}

		} catch (ModelException e) {
			super.alertError("Error while changing password", "File %s", model.getFilePath().toString());
		}
	}

	private void doExportClearFile(ActionEvent event) {
		boolean ask = true;
		Path newPath = null;
		while(ask) {
			newPath = super.saveFile("Select file path", model.getFilePath());
			if (JkFiles.areEquals(model.getFilePath(), newPath)) {
				super.alertError("ILLEGAL OUTPUT PATH", "Cannot override original password file \"%s\"", model.getFilePath());
			} else {
				ask = false;
			}
		}

		if(newPath == null) 	return;

		newPath = newPath.toAbsolutePath();

		List<Pwd> pwdList = tableData.subList(0, tableData.size());
		List<String> lines = new ArrayList<>();
		lines.add(JkStreams.join(IPwdModel.FILE_HEADER, "|"));
		lines.addAll(JkStreams.map(pwdList, this::toClearLine));

		String text = new JkColumnFmtBuilder(lines).toString("|", 3);

		try {
			JkFiles.writeFile(newPath, text, true);
			super.alertInfo("CLEAR FILE SAVED", "%s", newPath);
		} catch (Exception e) {
			super.alertError("Error while saving excel file", "File %s", newPath.toString());
		}
	}
	private String toClearLine(Pwd pwd) {
		return String.format("%s|%s|%s|%s",
			pwd.getKey(),
			pwd.getUsername(),
			pwd.getPassword(),
			pwd.getNotes().replaceAll("\t", " ").replaceAll("\n", "; ")
		);
	}

	private void doExportClearExcel(ActionEvent event) {
		boolean ask = true;
		Path newPath = null;
		while(ask) {
			newPath = super.saveFile("Select file path", model.getFilePath());
			if (JkFiles.areEquals(model.getFilePath(), newPath)) {
				super.alertError("ILLEGAL OUTPUT PATH", "Cannot override original password file \"%s\"", model.getFilePath());
			} else {
				ask = false;
			}
		}

		if(newPath == null) 	return;

		newPath = newPath.toAbsolutePath();
		if(!StringUtils.endsWithIgnoreCase(newPath.toString(), "xlsx")) {
			newPath = Paths.get(String.format("%s.xlsx", newPath.toString()));
		}

		try {
			try (JkWorkbookXSSF wb = new JkWorkbookXSSF()) {
				JkSheetXSSF sheet = wb.getSheet("Pwd");
				sheet.setValues(0, 0, IPwdModel.FILE_HEADER);
				List<Pwd> pwdList = tableData.subList(0, tableData.size());
				for (int i = 0; i < pwdList.size(); i++) {
					sheet.setValues(1 + i, 0, toExcelLine(pwdList.get(i)));
				}
				wb.persist(newPath);
				super.alertInfo("Passwords saved in clear in excel file", "Path: %s", newPath);
			}

		} catch (Exception e) {
			super.alertError("Error while creating excel file", "File %s", newPath.toString());
		}
	}

	private List<String> toExcelLine(Pwd pwd) {
		return Arrays.asList(
				pwd.getKey(),
				pwd.getUsername(),
				pwd.getPassword(),
				pwd.getNotes()
		);
	}

	private String askEncryptionPwd(boolean confirm) {
		String pwd = null;
		String password = super.askUserInput(null, "Insert password", null, StringUtils::isNotBlank, "Password cannot be blank");
		if(password != null) {
			if(!confirm) {
				pwd = password;
			} else {
				String repeatPassword = super.askUserInput(null, "Repeat password", null);
				if (!StringUtils.equals(password, repeatPassword)) {
					super.alertError("Both password must be equals", null);
				} else {
					pwd = password;
				}
			}
		}
		return pwd;
	}

	private void doOpenPwdPath(Path path) {
		if(path == null) 	return;
		path = path.toAbsolutePath();

		if(model != null && JkFiles.areEquals(model.getFilePath(), path)) {
			super.alertInfo("Password file already open", null);
			return;
		}

		String headerText = String.format("Unparseable file %s", path.toString());

		String password = super.askUserInput(null, "Insert password to decrypt file", null);
		if(StringUtils.isBlank(password)) {
			super.alertWarning(headerText, "No password insert");
			return;
		}

		try {
			IPwdModel model = PwdModel.createModelInstance(path, password);
			List<Pwd> pwdList = model.getPasswords();
			pwdList.sort(Pwd::compareTo);
			tableData.setAll(pwdList);
			registerRecentPath(path);
			pwdPath.set(path);
			this.model = model;
			viewStatus.setValue(ViewStatus.ReadOnly);

		} catch (ModelException e) {
			super.alertError(headerText, e);
		}
	}

	private void doAddPassword(ActionEvent event) {
		String resp = super.askUserInput("Insert key", "Insert new pwd key", null, StringUtils::isNotBlank, "Empty key");
		if(StringUtils.isNotBlank(resp)) {
			String key = resp.trim();
			boolean exists = !tableData.filtered(pwd -> pwd.getKey().equalsIgnoreCase(key)).isEmpty();
			if(exists) {
				super.alertWarning("Unable to add new password", "Key \"%s\" already exists", key);
			} else {
				try {
					Pwd pwd = new Pwd();
					pwd.setKey(key);
					tableData.add(pwd);
					tableData.sort(pwd);
					tablePwd.getSelectionModel().clearSelection();
					tablePwd.getSelectionModel().select(pwd);
					savePasswordChanges();
					super.alertInfo("New password add", "Key: %s", key);

				} catch(ModelException ex) {
					super.alertError("Error adding new pwd", ex);
				}
			}
		}
	}

	private void doEditPassword(ActionEvent event) {
		viewStatus.set(ViewStatus.Edit);
	}
	
	private void doDeletePassword(ActionEvent event) {
		try {
			Pwd sel = tablePwd.getSelectionModel().getSelectedItem();
			if(sel != null) {
				boolean del = super.alertConfirmation(String.format("Delete password \"%s\"?", sel.getKey()), null);
				if (del) {
					tableData.removeIf(pwd -> sel.getKey().equalsIgnoreCase(pwd.getKey()));
					tablePwd.getSelectionModel().clearSelection();
					savePasswordChanges();
					super.alertInfo("Deleted password", "Key: %s", sel.getKey());
				}
			}

		} catch(ModelException ex) {
			super.alertError("Error while delete pwd", ex);
		}
	}

	private void doSaveChanges(ActionEvent event) {
		Pwd newPwd = new Pwd();
		newPwd.setKey(fieldEditKey.getText().trim());
		newPwd.setUsername(fieldEditUsername.getText().trim());
		newPwd.setPassword(fieldEditPassword.getText().trim());
		newPwd.setNotes(txtAreaEditNotes.getText().trim());

		if(StringUtils.isBlank(newPwd.getKey())) {
			super.alertWarning("Key cannot be empty", null);
			return;
		}

		if(newPwd.equals(actualEditPwd)) {
			super.alertInfo("No changes to save", null);
			viewStatus.setValue(ViewStatus.ReadOnly);
			return;
		}

		int found = tableData.filtered(p -> p != actualEditPwd && StringUtils.equalsAnyIgnoreCase(p.getKey(), newPwd.getKey())).size();
		if(found > 0) {
			super.alertWarning("Password key already exists", null);
			return;
		}

		try {
			tableData.remove(actualEditPwd);
			tableData.add(newPwd);
			tableData.sort(newPwd);

			tablePwd.getSelectionModel().clearSelection();
			tablePwd.getSelectionModel().select(newPwd);

			savePasswordChanges();

		} catch(ModelException ex) {
			super.alertError("Error while save changes", ex);
		}

		viewStatus.setValue(ViewStatus.ReadOnly);
	}

	private void doCancelChanges(ActionEvent event) {
		viewStatus.setValue(ViewStatus.ReadOnly);
	}

	private void savePasswordChanges() throws ModelException {
		model.savePasswords(tableData.subList(0, tableData.size()));
	}


	private void registerRecentPath(Path path) throws ModelException {
		try {
			String strPath = path.toAbsolutePath().toString();
			boolean found = JkStreams.filter(itemRecents, mi -> mi.getText().equals(strPath)).size() > 0;
			if (!found) {
				MenuItem menuItem = creteMenuItem(strPath);
				itemRecents.add(0, menuItem);
				if (itemRecents.size() > 10) {
					List<MenuItem> first10 = itemRecents.subList(0, 10);
					itemRecents.clear();
					itemRecents.addAll(first10);
				}
				menuRecents.getItems().clear();
				menuRecents.getItems().addAll(itemRecents);
				List<String> lines = JkStreams.map(itemRecents, MenuItem::getText);
				JkFiles.writeFile(RECENT_OPENED_PATH, lines, true);
			}

		} catch(Exception ex) {
			throw new ModelException(ex);
		}
	}

	private void setRecentOpenedFromFile() {
		try {
			itemRecents.clear();
			if (Files.exists(RECENT_OPENED_PATH)) {
				List<String> lines = Files.readAllLines(RECENT_OPENED_PATH);
				int numOrigLines = lines.size();
				lines.removeIf(line -> StringUtils.isBlank(line) || !Files.exists(Paths.get(line)));
				for (int i = 0; i < lines.size(); i++) {
					String trim = lines.get(i).trim();
					MenuItem menuItem = creteMenuItem(trim);
					itemRecents.add(menuItem);
				}
				if(numOrigLines != lines.size()) {
					if(lines.isEmpty()) {
						Files.deleteIfExists(RECENT_OPENED_PATH);
					} else {
						JkFiles.writeFile(RECENT_OPENED_PATH, lines, true);
					}
				}
			}
			menuRecents.getItems().clear();
			menuRecents.getItems().addAll(itemRecents);

		} catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private MenuItem creteMenuItem(String strPath) {
		Path path = Paths.get(strPath);
		MenuItem menuItem = new MenuItem(strPath);
		menuItem.setOnAction(event -> doOpenPwdPath(path));
		menuItem.visibleProperty().bind(Bindings.createBooleanBinding(() -> !path.equals(pwdPath.get()), pwdPath));
		return menuItem;
	}
}
