package xxx.joker.apps.pwdmanager.exceptions;

import xxx.joker.libs.javalibs.exception.JkException;

/**
 * Created by f.barbano on 19/11/2017.
 */
public class ModelException extends JkException {

	public ModelException(String message, Object... params) {
		super(message, params);
	}

	public ModelException(Throwable cause, String message, Object... params) {
		super(cause, message, params);
	}

	public ModelException(Throwable cause) {
		super(cause);
	}
}
