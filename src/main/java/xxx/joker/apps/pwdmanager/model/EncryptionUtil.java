package xxx.joker.apps.pwdmanager.model;


import xxx.joker.libs.javalibs.utils.JkFiles;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class EncryptionUtil {

	private static final byte[] salt = new byte[]{(byte)67, (byte)118, (byte)-107, (byte)-57, (byte)91, (byte)-41, (byte)69, (byte)23};

	private EncryptionUtil() {
	}

	public static void encryptFile(Path inputPath, Path outputPath, String password, boolean overwrite) throws IOException, GeneralSecurityException {
		if(!overwrite && Files.exists(outputPath)) {
			throw new IOException("File [" + outputPath + "] already exists!");
		}

		Cipher cipher = makeCipher(password, Boolean.valueOf(true));
		File inputFile = inputPath.toFile();
		FileInputStream inStream = new FileInputStream(inputFile);
		byte blockSize = 8;
		int paddedCount = blockSize - (int)inputFile.length() % blockSize;
		int padded = (int)inputFile.length() + paddedCount;
		byte[] decData = new byte[padded];
		inStream.read(decData);
		inStream.close();

		for(int encData = (int)inputFile.length(); encData < padded; ++encData) {
			decData[encData] = (byte)paddedCount;
		}

		byte[] var11 = cipher.doFinal(decData);
		JkFiles.writeFile(outputPath, var11, overwrite);
		JkFiles.copyAttributes(inputPath, outputPath);
	}

	public static void decryptFile(Path inputPath, Path outputPath, String password, boolean overwrite) throws GeneralSecurityException, IOException {
		if (!overwrite && Files.exists(outputPath)) {
			throw new IOException("File [" + outputPath + "] already exists!");
		}

		Cipher cipher = makeCipher(password, Boolean.valueOf(false));
		File inputFile = inputPath.toFile();
		FileInputStream inStream = new FileInputStream(inputFile);
		byte[] encData = new byte[(int) inputFile.length()];
		inStream.read(encData);
		inStream.close();
		byte[] decData = cipher.doFinal(encData);
		byte padCount = decData[decData.length - 1];
		if (padCount >= 1 && padCount <= 8) {
			decData = Arrays.copyOfRange(decData, 0, decData.length - padCount);
		}

		JkFiles.writeFile(outputPath, decData, overwrite);
		JkFiles.copyAttributes(inputPath, outputPath);
	}

	public static String getMD5(Path inputPath) throws NoSuchAlgorithmException, IOException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		FileInputStream fis = new FileInputStream(inputPath.toFile());

		byte[] dataBytes = new byte[1024];

		int nread;
		while ((nread = fis.read(dataBytes)) != -1) {
			md.update(dataBytes, 0, nread);
		}

		return computeMD5(md);
	}

	public static String getMD5(String s) throws NoSuchAlgorithmException, IOException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(s.getBytes());
		return computeMD5(md);
	}

	private static Cipher makeCipher(String password, Boolean decryptMode) throws GeneralSecurityException {
		PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
		SecretKey key = keyFactory.generateSecret(keySpec);
		PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 42);
		Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
		if(decryptMode.booleanValue()) {
			cipher.init(1, key, pbeParamSpec);
		} else {
			cipher.init(2, key, pbeParamSpec);
		}

		return cipher;
	}

	private static String computeMD5(MessageDigest md) throws NoSuchAlgorithmException, IOException {
		byte[] mdbytes = md.digest();

		//convert the byte to hex format method 1
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < mdbytes.length; i++) {
			sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		}

		return sb.toString();
	}
}
