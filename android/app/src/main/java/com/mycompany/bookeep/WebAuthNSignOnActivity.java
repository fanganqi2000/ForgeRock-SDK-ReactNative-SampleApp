package com.mycompany.bookeep;

import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;

import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;

public class WebAuthNSignOnActivity extends AppCompatActivity {

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
        WebAuthNSignOnFragment webAuthNSignOnFragment = new WebAuthNSignOnFragment();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.layout, webAuthNSignOnFragment);
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
