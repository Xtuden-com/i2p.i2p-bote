/**
 * Copyright (C) 2009  HungryHobo@mail.i2p
 *
 * The GPG fingerprint for HungryHobo@mail.i2p is:
 * 6DD3 EAA2 9990 29BC 4AD2 7486 1E2C 7B61 76DC DC12
 *
 * This file is part of I2P-Bote.
 * I2P-Bote is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * I2P-Bote is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with I2P-Bote.  If not, see <http://www.gnu.org/licenses/>.
 */

package i2p.bote.io;

import static i2p.bote.io.FileEncryptionConstants.DEFAULT_PASSWORD;
import static i2p.bote.io.FileEncryptionConstants.KEY_LENGTH;
import static i2p.bote.io.FileEncryptionConstants.PASSWORD_FILE_PLAIN_TEXT;
import i2p.bote.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import net.i2p.util.Log;

public class FileEncryptionUtil {

    /**
     * Generates a symmetric encryption key from a password and salt.
     * A given set of input parameters will always produce the same key.
     * @param password
     * @param salt
     * @param numIterations Number of key strengthening iterations
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    static byte[] getEncryptionKey(char[] password, byte[] salt, int numIterations) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (password==null || password.length<=0)
            password = DEFAULT_PASSWORD;
        
        KeySpec spec = new PBEKeySpec(password, salt, numIterations, KEY_LENGTH);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = keyFactory.generateSecret(spec).getEncoded();
        return hash;
    }
    
    /**
     * Decrypts a file with a given password and returns <code>true</code> if the decrypted
     * text is {@link FileEncryptionConstants#PASSWORD_FILE_PLAIN_TEXT}; <code>false</code>
     * otherwise.
     * @param password
     * @param passwordFile
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws IOException
     */
    public static boolean isPasswordCorrect(char[] password, File passwordFile) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        if (!passwordFile.exists())
            return true;
        
        EncryptedInputStream inputStream = null;
        try {
            inputStream = new EncryptedInputStream(new FileInputStream(passwordFile), password);
            byte[] decryptedText = Util.readBytes(inputStream);
            return Arrays.equals(PASSWORD_FILE_PLAIN_TEXT, decryptedText);
        }
        finally {
            if (inputStream != null)
                inputStream.close();
        }
    }
    
    /**
     * Encrypts the array {@link FileEncryptionConstants#PASSWORD_FILE_PLAIN_TEXT} with a
     * password and writes the encrypted data to a file.
     * @param password
     * @param passwordFile
     * @throws IOException
     */
    public static void writePasswordFile(char[] password, File passwordFile) throws IOException {
        if (password==null || password.length==0) {
            if (!passwordFile.delete())
                new Log(FileEncryptionUtil.class).error("Can't delete file: " + passwordFile.getAbsolutePath());
            return;
        }
        
        EncryptedOutputStream outputStream = null;
        try {
            outputStream = new EncryptedOutputStream(new FileOutputStream(passwordFile), password);
            outputStream.write(PASSWORD_FILE_PLAIN_TEXT);
        } catch (IOException e) {
            new Log(FileEncryptionUtil.class).error("Can't write password file <" + passwordFile.getAbsolutePath() + ">", e);
            throw e;
        }
        finally {
            if (outputStream != null)
                outputStream.close();
        }
    }
    
    /**
     * Encrypts a file with a new password. No verification of the old password is done.
     * @param file
     * @param oldPassword
     * @param newPassword
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws IOException
     */
    public static void changePassword(File file, char[] oldPassword, char[] newPassword) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        InputStream inputStream = null;
        byte[] decryptedData = null;
        try {
            inputStream = new EncryptedInputStream(new FileInputStream(file), oldPassword);
            decryptedData = Util.readBytes(inputStream);
        }
        finally {
            if (inputStream != null)
                inputStream.close();
        }
        OutputStream outputStream = null;
        try {
            outputStream = new EncryptedOutputStream(new FileOutputStream(file), newPassword);
            outputStream.write(decryptedData);
        }
        finally {
            if (outputStream != null)
                outputStream.close();
        }
    }
}