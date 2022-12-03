package com.mycompany.bookeep;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.callback.WebAuthnAuthenticationCallback;
import org.forgerock.android.auth.exception.AlreadyAuthenticatedException;
import org.forgerock.android.auth.exception.WebAuthnResponseException;

@SuppressLint("ValidFragment")
public class WebAuthNSignOnFragment extends Fragment {
    TextView textView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view= inflater.inflate(R.layout.fragment, container, false);
        textView=view.findViewById(R.id.text);
        textView.setText("WebAuthN Sign On ...");

        WebAuthNSignOnActivity.currentNode.getCallback(WebAuthnAuthenticationCallback.class).authenticate(this, WebAuthNSignOnActivity.currentNode, null, new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Logger.warn("WebAuthnAuthenticationCallback", "WebAuthN Authentication client side Succeeded");
                WritableMap map = Arguments.createMap();
                map.putString("type", "WebAuthnAuthenticationCallback");
                WebAuthNSignOnActivity.reactNativePromise.resolve(map);
                //getActivity().finish();
            }

            @Override
            public void onException(Exception e) {
                Logger.error("WebAuthnAuthenticationCallback", e, "WebAuthN Authentication client side Failed");
                if (e instanceof AlreadyAuthenticatedException) {
                    WritableMap map = Arguments.createMap();
                    map.putString("error", e.getMessage());
                    WebAuthNSignOnActivity.reactNativePromise.resolve(map);
                } else if (e instanceof WebAuthnResponseException) {
                    WritableMap map = Arguments.createMap();
                    map.putString("error", e.getMessage());
                    WebAuthNSignOnActivity.reactNativePromise.resolve(map);
                } else {
                    WebAuthNSignOnActivity.reactNativePromise.reject("error", e.getMessage(), e);
                }
                getActivity().finish();
            }
        });
        return view;
    }
}