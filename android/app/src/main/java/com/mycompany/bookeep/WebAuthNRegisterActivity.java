package com.mycompany.bookeep;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.callback.IdPCallback;
import org.forgerock.android.auth.callback.WebAuthnRegistrationCallback;
import org.forgerock.android.auth.exception.AlreadyAuthenticatedException;

public class WebAuthNRegisterActivity extends AppCompatActivity {

    public static Promise reactNativePromise;
    public static Node currentNode;
    public static NodeListener listener;
    public static ReactApplicationContext context;

    public static Activity fa;
    public static boolean active = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fa = this;
        setContentView(R.layout.activity_main);
        WebAuthNRegisterFragment webAuthNRegisterFragment = new WebAuthNRegisterFragment();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.layout, webAuthNRegisterFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }
}
