package com.mycompany.bookeep;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.google.gson.Gson;

import org.forgerock.android.auth.AccessToken;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.exception.AlreadyAuthenticatedException;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

import java.util.HashMap;
import java.util.Map;

public class CentralLoginActivity extends AppCompatActivity {

    public static Promise reactNativePromise;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FRUser.browser().appAuthConfigurer().authorizationRequest(r -> {
            Map<String, String> additionalParameters = new HashMap<>();
            additionalParameters.put("service", "SimpleLogin");
            //additionalParameters.put("KEY2", "VALUE2");
            r.setAdditionalParameters(additionalParameters);
            //r.setLoginHint("login");
            //r.setPrompt("login");
        }).customTabsIntent(t -> {
                    t.setShowTitle(false);
        }).done().login(this, new FRListener<FRUser>() {
            @Override
            public void onSuccess(FRUser result) {
                final AccessToken accessToken;
                try {
                    accessToken = FRUser.getCurrentUser().getAccessToken();
                    Gson gson = new Gson();
                    String json = gson.toJson(accessToken);
                    WritableMap map = Arguments.createMap();
                    map.putString("accessToken", json);
                    reactNativePromise.resolve(map);
                } catch (AuthenticationRequiredException e) {
                    Logger.error("error", e, "login Failed");
                    reactNativePromise.reject("error", e.getMessage(), e);
                }
                finish();
            }
            @Override
            public void onException(Exception e) {
                Logger.error("error", e, "login Failed");
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
}