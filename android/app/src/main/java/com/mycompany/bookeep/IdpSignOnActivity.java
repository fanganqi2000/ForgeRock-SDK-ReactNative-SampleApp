package com.mycompany.bookeep;

import android.app.Activity;
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
import org.forgerock.android.auth.exception.AlreadyAuthenticatedException;

public class IdpSignOnActivity extends AppCompatActivity {

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
        final Fragment first = new FirstFragment();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.layout, first);
        fragmentTransaction.commit();
        currentNode.getCallback(IdPCallback.class).signIn(first, null, new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Logger.warn("IdPCallback", "Social SignOn Succeeded");
                //currentNode.next(context, listener);
                WritableMap map = Arguments.createMap();
                map.putString("type", "IdPCallback");
                reactNativePromise.resolve(map);
                //finish();
            }

            @Override
            public void onException(Exception e) {
                Logger.error("IdpCallback", e, "Social SignOn Failed");
                if (e instanceof AlreadyAuthenticatedException) {
                    WritableMap map = Arguments.createMap();
                    map.putString("error", e.getMessage());
                    reactNativePromise.resolve(map);
                } else {
                    reactNativePromise.reject("error", e.getMessage(), e);
                }
                finish();
            }
        });
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
