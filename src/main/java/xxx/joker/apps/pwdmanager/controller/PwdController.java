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
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import org.apache.commons.lang3.StringUtils;
import xxx.joker.apps.pwdmanager.beans.Pwd;
import xxx.joker.apps.pwdmanager.common.Configs;
import xxx.joker.apps.pwdmanager.exceptions.ModelException;
import xxx.joker.apps.pwdmanager.model.IPwdModel;
import xxx.joker.apps.pwdmanager.model.PwdModel;
import xxx.joker.libs.excel.JkExcelSheet;
import xxx.joker.libs.excel.JkExcelUtil;
import xxx.joker.libs.core.format.JkColumnFmtBuilder;
import xxx.joker.libs.core.utils.JkFiles;
import xxx.joker.libs.core.utils.JkStreams;
import xxx.joker.libs.core.utils.JkStrings;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by f.barbano on 19/11/2017.
 */
public class PwdController extends AbstractController implements Initializable {

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
		BooleanBinding pwdPathIsNull = Bindings.createBooleanBinding(() -> pwdPath.get() == null, pwdPath);
		itemNewFile.setOnAction(this::doCreateNewFile);
		itemOpenFile.setOnAction(this::doOpenFilePath);
		itemSaveAs.setOnAction(this::doSaveFileAs);
		itemSaveAs.disableProperty().bind(pwdPathIsNull);
		itemChangePwd.setOnAction(this::doChangeEncryptionPwd);
		itemChangePwd.disableProperty().bind(pwdPathIsNull);
		itemExportClearFile.setOnAction(this::doExportClearFile);
		itemExportClearFile.disableProperty().bind(pwdPathIsNull);
		itemExportClearExcel.setOnAction(this::doExportClearExcel);
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
		selectedPwd.addListener((observable, oldValue, newValue) -> updateDetailFields());
		// Bind editable property
		BooleanBinding editableBind = Bindings.createBooleanBinding(() -> viewStatus.get() == ViewStatus.Edit, viewStatus);
		fieldSelKey.editableProperty().bind(editableBind);
		fieldSelUsername.editableProperty().bind(editableBind);
		fieldSelPassword.editableProperty().bind(editableBind);
		txtAreaSelNotes.editableProperty().bind(editableBind);

	}

	private void initTableView() {
		tablePwd.setItems(filteredList);
		tablePwd.setEditable(false);
		tablePwd.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		tablePwd.setRowFactory(this::removeRowFocusIfSelected);
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
	private TableRow<Pwd> removeRowFocusIfSelected(TableView<Pwd> tableView) {
		TableRow<Pwd> row = new TableRow<>();
		row.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			final int index = row.getIndex();
			TableView.TableViewSelectionModel<Pwd> selModel = tableView.getSelectionModel();
			if (index >= 0 && index < tableView.getItems().size()) {
				viewStatus.setValue(ViewStatus.ReadOnly);
				if(selModel.getSelectedItems().size() == 1 && selModel.isSelected(index)){
					selModel.clearSelection(index);
					event.consume();
				}
			}
		});
		return row;
	}
	private void initButtons() {
		// Disabled property binding
		btnAdd.disableProperty().bind(Bindings.createBooleanBinding(() -> viewStatus.get() != ViewStatus.ReadOnly, viewStatus));
		btnEdit.disableProperty().bind(Bindings.createBooleanBinding(() -> viewStatus.get() != ViewStatus.ReadOnly || tablePwd.getSelectionModel().getSelectedItem() == null, viewStatus, tablePwd.getSelectionModel().selectedItemProperty()));
		btnDelete.disableProperty().bind(Bindings.createBooleanBinding(() -> viewStatus.get() != ViewStatus.ReadOnly || tablePwd.getSelectionModel().getSelectedItem() == null, viewStatus, tablePwd.getSelectionModel().selectedItemProperty()));
		btnSave.disableProperty().bind(Bindings.createBooleanBinding(() -> viewStatus.get() != ViewStatus.Edit, viewStatus));
		btnCancel.disableProperty().bind(Bindings.createBooleanBinding(() -> viewStatus.get() != ViewStatus.Edit, viewStatus));
		// Actions
		btnAdd.setOnAction(this::doAddPassword);
		btnEdit.setOnAction(this::doEditPassword);
		btnDelete.setOnAction(this::doDeletePassword);
		btnSave.setOnAction(this::doSaveChanges);
		btnCancel.setOnAction(this::doCancelChanges);
	}
	private void updateDetailFields() {
		// Bind text
		Pwd pwd = selectedPwd.get();
		fieldSelKey.setText(pwd == null ? "" : pwd.getKey());
		fieldSelUsername.setText(pwd == null ? "" : pwd.getUsername());
		fieldSelPassword.setText(pwd == null ? "" : pwd.getPassword());
		txtAreaSelNotes.setText(pwd == null ? "" : pwd.getNotes());
		if(pwd == null) {
			viewStatus.setValue(ViewStatus.ReadOnly);
		}
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
		lines.add(IPwdModel.FILE_HEADER);
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
		if(!JkExcelUtil.isExcelFile(newPath)) {
			newPath = Paths.get(String.format("%s.xlsx", newPath.toString()));
		}

		List<Pwd> pwdList = tableData.subList(0, tableData.size());

		List<List<String>> lines = new ArrayList<>();
		lines.add(JkStrings.splitFieldsList(IPwdModel.FILE_HEADER, "|"));
		lines.addAll(JkStreams.map(pwdList, pwd -> JkStrings.splitFieldsList(toExcelLine(pwd), "|")));

		JkExcelSheet sheet = new JkExcelSheet("Passwords");
		sheet.setLines(lines);

		try {
			JkExcelUtil.createExcelFile(newPath.toFile(), sheet, true);
			super.alertInfo("CLEAR EXCEL SAVED", "%s", newPath);
		} catch (IOException e) {
			super.alertError("Error while saving excel file", "File %s", newPath.toString());
		}
	}
	private String toExcelLine(Pwd pwd) {
		return String.format("%s|%s|%s|%s",
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
		newPwd.setKey(fieldSelKey.getText().trim());
		newPwd.setUsername(fieldSelUsername.getText().trim());
		newPwd.setPassword(fieldSelPassword.getText().trim());
		newPwd.setNotes(txtAreaSelNotes.getText().trim());

		if(StringUtils.isBlank(newPwd.getKey())) {
			super.alertWarning("Key cannot be empty", null);
			return;
		}

		Pwd sel = tablePwd.getSelectionModel().getSelectedItem();
		if(newPwd.equals(sel)) {
			super.alertInfo("No changes to save", null);
			viewStatus.setValue(ViewStatus.ReadOnly);
			return;
		}

		int found = tableData.filtered(p -> p != sel && StringUtils.equalsAnyIgnoreCase(p.getKey(), newPwd.getKey())).size();
		if(found > 0) {
			super.alertWarning("Password key already exists", null);
			return;
		}

		try {
			tableData.remove(sel);
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
		int index = tablePwd.getSelectionModel().getSelectedIndex();
		tablePwd.getSelectionModel().clearSelection();
		tablePwd.getSelectionModel().select(index);
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
