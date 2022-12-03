package com.mycompany.bookeep;

import org.forgerock.android.auth.Encryptor;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class EmptyEncryptor implements Encryptor {

    @Override
    public byte[] encrypt(byte[] clearText) {
        return clearText;
    }

    @Override
    public byte[] decrypt(byte[] encryptedData) {
        return encryptedData;
    }

    @Override
    public void reset() throws GeneralSecurityException, IOException {

    }
}
