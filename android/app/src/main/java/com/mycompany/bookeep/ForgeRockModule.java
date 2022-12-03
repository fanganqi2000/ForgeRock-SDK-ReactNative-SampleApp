package com.mycompany.bookeep;


import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.gson.Gson;

import org.forgerock.android.auth.AccessToken;
import org.forgerock.android.auth.FRAuth;
import org.forgerock.android.auth.FRDevice;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.UserInfo;
import org.forgerock.android.auth.callback.BooleanAttributeInputCallback;
import org.forgerock.android.auth.callback.ChoiceCallback;
import org.forgerock.android.auth.callback.DeviceProfileCallback;
import org.forgerock.android.auth.callback.IdPCallback;
import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PasswordCallback;
import org.forgerock.android.auth.callback.SelectIdPCallback;
import org.forgerock.android.auth.callback.WebAuthnAuthenticationCallback;
import org.forgerock.android.auth.callback.WebAuthnRegistrationCallback;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Semaphore;


public class ForgeRockModule extends ReactContextBaseJavaModule {
    ReactApplicationContext context;
    Node currentNode;
    NodeListener listener;
    Callback reactNativeCallback;

    ForgeRockModule(ReactApplicationContext context) {
        super(context);
        this.context = context;
    }

    @Override
    public String getName() {
        return "ForgeRockModule";
    }


    @ReactMethod
    public void frAuthStart() {
        Logger.set(Logger.Level.DEBUG);
        FRAuth.start(this.context);
    }

    @ReactMethod
    public void performUserLogout() {
        FRUser user = FRUser.getCurrentUser();
        if (user != null) {
            user.logout();
        }
    }

    @ReactMethod
    public void performUserLoginWithoutUIWithCallback(Callback cb) {
        try{
            reactNativeCallback = cb;
            customLogin();
        }catch (Exception e){
            cb.invoke(e.toString(), null);
        }
    }

    @ReactMethod
    public void authenticateWithTree(String tree, Callback cb) {
        try{
            reactNativeCallback = cb;
            treeLogin(tree);
        }catch (Exception e){
            cb.invoke(e.toString(), null);
        }
    }

    @ReactMethod
    public void loginWithBrowser(Promise promise) {
        launchBrowser(promise);
    }

    @ReactMethod
    public void loginWithIdP(Promise promise) {
        idpSignOn(promise);
    }

    @ReactMethod
    public void registerWebAuthN(Promise promise) {
        webAuthNRegister(promise);
    }

    @ReactMethod
    public void loginWebAuthN(Promise promise) {
        webAuthNSignOn(promise);
    }

    @ReactMethod
    public void getDeviceInformation(Promise promise) {
        FRDevice.getInstance().getProfile(new FRListener<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                WritableMap productMap = null;
                try {
                    Gson gson = new Gson();
                    String jsonStr = gson.toJson(result);
                    productMap = convertJsonToMap(new
                            JSONObject(jsonStr));
                    promise.resolve(productMap);
                } catch (JSONException e) {
                    Logger.error("error", e, "getDeviceInformation Failed");
                    promise.reject("error", e.getMessage(), e);
                }
            }

            @Override
            public void onException(Exception e) {
                Logger.warn("getDeviceInformation", e, "Failed to retrieve device profile");
                promise.reject("error", e.getMessage(), e);
            }
        });
    }

    @ReactMethod
    public void getUserInfo(Promise promise) {
        if (FRUser.getCurrentUser() != null) {
            FRUser.getCurrentUser().getUserInfo(new FRListener<UserInfo>() {
                @Override
                public void onSuccess(final UserInfo result) {
                    WritableMap productMap = null;
                    JSONObject jsonResult = result.getRaw();
                    try {
                        productMap = convertJsonToMap(jsonResult);
                        promise.resolve(productMap);
                    } catch (JSONException e) {
                        Logger.error("error", e, "getUserInfo Failed");
                        promise.reject("error", e.getMessage(), e);
                    }
                }

                @Override
                public void onException(final Exception e) {
                    Logger.error("error", e, "getUserInfo Failed");
                    promise.reject("error", e.getMessage(), e);
                }
            });
        }
    }

    @ReactMethod
    public void getAccessToken(Promise promise) {
        if (FRUser.getCurrentUser() != null) {
            final AccessToken accessToken;
            WritableMap map = Arguments.createMap();
            try {
                accessToken = FRUser.getCurrentUser().getAccessToken();
                Gson gson = new Gson();
                String json = gson.toJson(accessToken);
                map.putString("accessToken", json);
                promise.resolve(map);
            } catch (AuthenticationRequiredException e) {
                Logger.error("error", e, "getUserInfo Failed");
                promise.reject("error", e.getMessage(), e);
            }
        }
    }

    @ReactMethod
    public void nextWithUserCompletion(ReadableArray array, Callback cb) throws InterruptedException {
        reactNativeCallback = cb;
        if (array.size() == 0) {
            for (org.forgerock.android.auth.callback.Callback callback : currentNode.getCallbacks()) {
                String currentCallbackType = callback.getType();
                if (currentCallbackType == "DeviceProfileCallback") {
                    final Semaphore available = new Semaphore(1, true);
                    available.acquire();
                    currentNode.getCallback(DeviceProfileCallback.class).execute(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Logger.warn("DeviceProfileCallback", "Device Profile Collection Succeeded");
                            available.release();
                        }

                        @Override
                        public void onException(Exception e) {
                            Logger.warn("DeviceProfileCallback", e, "Device Profile Collection Failed");
                            available.release();
                        }
                    });
                    currentNode.next(this.context, listener);
                } else if (currentCallbackType == "IdPCallback") {
                }
            }
        } else {
            ArrayList<Object> list = array.toArrayList();
            for (Object map : list) {
                String callbackType = ((HashMap<String, Object>) map).get("identifier").toString();
                int cnt = 0;
                for (org.forgerock.android.auth.callback.Callback callback : currentNode.getCallbacks()) {
                    String currentCallbackType = callback.getType();
                    if ((currentCallbackType == "NameCallback") && (callbackType.equals(currentCallbackType + cnt))) {
                        String value = ((HashMap<String, String>) map).get("text");
                        currentNode.getCallback(NameCallback.class).setName(value);
                    }
                    if ((currentCallbackType == "PasswordCallback") && (callbackType.equals(currentCallbackType + cnt))) {
                        String value = ((HashMap<String, String>) map).get("text");
                        currentNode.getCallback(PasswordCallback.class).setPassword(value.toCharArray());
                    }
                    if ((currentCallbackType == "ChoiceCallback") && (callbackType.equals(currentCallbackType + cnt))) {
                        String value = ((HashMap<String, String>) map).get("text");
                        currentNode.getCallback(ChoiceCallback.class).setSelectedIndex(Integer.parseInt(value));
                    }
                    if ((currentCallbackType == "BooleanAttributeInputCallback") && (callbackType.equals(currentCallbackType + cnt))) {
                        boolean value = ((HashMap<String, Boolean>) map).get("text").booleanValue();
                        ((BooleanAttributeInputCallback)callback).setValue(value);
                    }
                    if ((currentCallbackType == "SelectIdPCallback") && (callbackType.equals(currentCallbackType))) {
                        String provider = ((HashMap<String, String>) map).get("text");
                        currentNode.getCallback(SelectIdPCallback.class).setValue(provider);
                    }
                    cnt++;
                }
            }
        }
        currentNode.next(this.context, listener);
    }

    public void customLogin() {
        NodeListener<FRUser> nodeListenerFuture = new NodeListener<FRUser>() {
            @Override
            public void onSuccess(FRUser user) {
                final AccessToken accessToken;
                WritableMap map = Arguments.createMap();
                try {
                    accessToken = FRUser.getCurrentUser().getAccessToken();
                    Gson gson = new Gson();
                    String json = gson.toJson(accessToken);
                    map.putString("accessToken", json);
                    reactNativeCallback.invoke(map);
                } catch (AuthenticationRequiredException e) {
                    Logger.warn("customLogin", e, "Login Failed");
                    map.putString("error", e.getLocalizedMessage());
                    reactNativeCallback.invoke(map);
                }
            }

            @Override
            public void onException(Exception e) {
                // Handle Exception
                Logger.warn("customLogin", e, "Login Failed");
                WritableMap map = Arguments.createMap();
                map.putString("error", e.getLocalizedMessage());
                reactNativeCallback.invoke(map);
            }

            @Override
            public void onCallbackReceived(Node node) {
                listener = this;
                currentNode = node;
                WritableMap callbacksMap = Arguments.createMap();
                LinkedHashMap<String, WritableMap> linkedMap = handleCallbacks();
                String[] keys = linkedMap.keySet().toArray(new String[linkedMap.size()]);
                for (int i = keys.length - 1; i >= 0; i--) {
                    callbacksMap.putMap(keys[i], linkedMap.get(keys[i]));
                }
                reactNativeCallback.invoke(callbacksMap);
            }
        };

        FRUser.login(this.context, nodeListenerFuture);
    }

    public void launchBrowser(Promise promise) {
        MainActivity activity = (MainActivity) getCurrentActivity();
        activity.reactNativePromise = promise;
        activity.centralizedLogin();
    }

    public void idpSignOn(Promise promise) {
        MainActivity activity = (MainActivity) getCurrentActivity();
        activity.reactNativePromise = promise;
        activity.idpSignOn(this.context, currentNode, this.listener);
    }

    public void webAuthNRegister(Promise promise) {
        MainActivity activity = (MainActivity) getCurrentActivity();
        activity.reactNativePromise = promise;
        activity.webAuthNRegister(this.context, currentNode, this.listener);
    }

    public void webAuthNSignOn(Promise promise) {
        MainActivity activity = (MainActivity) getCurrentActivity();
        activity.reactNativePromise = promise;
        activity.webAuthNSignOn(this.context, currentNode, this.listener);
    }

    public void treeLogin(String tree) {
        NodeListener<FRSession> nodeListenerFuture = new NodeListener<FRSession>() {
            @Override
            public void onSuccess(FRSession token) {
                final AccessToken accessToken;
                WritableMap map = Arguments.createMap();
                try {
                    accessToken = FRUser.getCurrentUser().getAccessToken();
                    Gson gson = new Gson();
                    String json = gson.toJson(accessToken);
                    map.putString("accessToken", json);
                    reactNativeCallback.invoke(map);
                    if (IdpSignOnActivity.active) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        IdpSignOnActivity.fa.finish();
                    }
                    if (WebAuthNSignOnActivity.active) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        WebAuthNSignOnActivity.fa.finish();
                    }
                } catch (AuthenticationRequiredException e) {
                    Logger.warn("treeLogin", e, "Login Failed");
                    map.putString("error", e.getLocalizedMessage());
                    reactNativeCallback.invoke(map);
                }
            }

            @Override
            public void onException(Exception e) {
                // Handle Exception
                if (IdpSignOnActivity.active) {
                    IdpSignOnActivity.fa.finish();
                }
                if (WebAuthNSignOnActivity.active) {
                    WebAuthNSignOnActivity.fa.finish();
                }
                Logger.warn("treeLogin", e, "Login Failed");
                WritableMap map = Arguments.createMap();
                map.putString("error", e.getLocalizedMessage());
                reactNativeCallback.invoke(map);
            }

            @Override
            public void onCallbackReceived(Node node) {
                if (IdpSignOnActivity.active) {
                    IdpSignOnActivity.fa.finish();
                }
                if (WebAuthNSignOnActivity.active) {
                    WebAuthNSignOnActivity.fa.finish();
                }
                listener = this;
                currentNode = node;
                WritableMap callbacksMap = Arguments.createMap();
                LinkedHashMap<String, WritableMap> linkedMap = handleCallbacks();
                String[] keys = linkedMap.keySet().toArray(new String[linkedMap.size()]);
                for (int i = keys.length - 1; i >= 0; i--) {
                    callbacksMap.putMap(keys[i], linkedMap.get(keys[i]));
                }
                reactNativeCallback.invoke(callbacksMap);
            }
        };

        FRSession.authenticate(context, tree, nodeListenerFuture);
    }

    private LinkedHashMap<String, WritableMap> handleCallbacks() {
        Node node = currentNode;
        LinkedHashMap<String, WritableMap> linkedMap = new LinkedHashMap<String, WritableMap>();
        int cnt = 0;
        for (org.forgerock.android.auth.callback.Callback callback : node.getCallbacks()) {
            WritableMap map = Arguments.createMap();
            if (callback.getType() == "NameCallback") {
                NameCallback currCallback = node.getCallback(NameCallback.class);
                map.putString("prompt",currCallback.prompt);
                map.putString("type",currCallback.getType());
                linkedMap.put("NameCallback" + cnt, map);
            } else if (callback.getType() == "PasswordCallback") {
                PasswordCallback currCallback = node.getCallback(PasswordCallback.class);
                map.putString("prompt",currCallback.prompt);
                map.putString("type",currCallback.getType());
                linkedMap.put("PasswordCallback" + cnt, map);
            } else if (callback.getType() == "ChoiceCallback") {
                ChoiceCallback currCallback = node.getCallback(ChoiceCallback.class);
                map.putString("prompt",currCallback.prompt);
                map.putString("type",currCallback.getType());
                String[] choices = currCallback.getChoices().toArray(new String[0]);
                WritableArray array = Arguments.fromArray(choices);
                map.putArray("choices", array);
                linkedMap.put("ChoiceCallback" + cnt, map);
            } else if (callback.getType() == "BooleanAttributeInputCallback") {
                BooleanAttributeInputCallback currCallback = (BooleanAttributeInputCallback)callback;
                map.putString("name", currCallback.getName());
                map.putString("prompt", currCallback.getPrompt());
                map.putString("type", currCallback.getType());
                map.putBoolean("checked", currCallback.getValue());
                linkedMap.put("BooleanAttributeInputCallback" + cnt, map);
            } else if (callback.getType() == "DeviceProfileCallback") {
                DeviceProfileCallback currCallback = node.getCallback(DeviceProfileCallback.class);
                map.putString("type",currCallback.getType());
                linkedMap.put("DeviceProfileCallback" + cnt, map);
            } else if (callback.getType() == "SelectIdPCallback") {
                SelectIdPCallback currCallback = (SelectIdPCallback)node.getCallback(SelectIdPCallback.class);
                List<SelectIdPCallback.IdPValue> providers = currCallback.getProviders();
                List<String> providerStrList = new ArrayList<String>();
                for (SelectIdPCallback.IdPValue provider: providers) {
                    if (!provider.getProvider().equals("localAuthentication")) {
                        providerStrList.add(provider.getProvider());
                    }
                }
                WritableArray array = Arguments.fromArray(providerStrList.toArray(new String[0]));
                map.putArray("providers", array);
                map.putString("type", currCallback.getType());
                linkedMap.put("SelectIdPCallback" + cnt, map);
            } else if (callback.getType() == "IdPCallback") {
                IdPCallback currCallback = node.getCallback(IdPCallback.class);
                map.putString("type", currCallback.getType());
                linkedMap.put("IdPCallback" + cnt, map);
            } else if (callback.getType() == "WebAuthnRegistrationCallback") {
                WebAuthnRegistrationCallback currCallback = node.getCallback(WebAuthnRegistrationCallback.class);
                map.putString("type", currCallback.getType());
                linkedMap.put("WebAuthnRegistrationCallback" + cnt, map);
            } else if (callback.getType() == "WebAuthnAuthenticationCallback") {
                WebAuthnAuthenticationCallback currCallback = node.getCallback(WebAuthnAuthenticationCallback.class);
                map.putString("type", currCallback.getType());
                linkedMap.put("WebAuthnAuthenticationCallback" + cnt, map);
            }
            cnt++;
        }
        return linkedMap;
    }

    private static WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {
        WritableMap map = new WritableNativeMap();

        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.putMap(key, convertJsonToMap((JSONObject) value));
            } else if (value instanceof  JSONArray) {
                map.putArray(key, convertJsonToArray((JSONArray) value));
            } else if (value instanceof  Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof  Integer) {
                map.putInt(key, (Integer) value);
            } else if (value instanceof  Double) {
                map.putDouble(key, (Double) value);
            } else if (value instanceof String)  {
                map.putString(key, (String) value);
            } else {
                map.putString(key, value.toString());
            }
        }
        return map;
    }

    private static WritableArray convertJsonToArray(JSONArray jsonArray) throws JSONException {
        WritableArray array = new WritableNativeArray();

        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                array.pushMap(convertJsonToMap((JSONObject) value));
            } else if (value instanceof  JSONArray) {
                array.pushArray(convertJsonToArray((JSONArray) value));
            } else if (value instanceof  Boolean) {
                array.pushBoolean((Boolean) value);
            } else if (value instanceof  Integer) {
                array.pushInt((Integer) value);
            } else if (value instanceof  Double) {
                array.pushDouble((Double) value);
            } else if (value instanceof String)  {
                array.pushString((String) value);
            } else {
                array.pushString(value.toString());
            }
        }
        return array;
    }

    private static JSONObject convertMapToJson(ReadableMap readableMap) throws JSONException {
        JSONObject object = new JSONObject();
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            switch (readableMap.getType(key)) {
                case Null:
                    object.put(key, JSONObject.NULL);
                    break;
                case Boolean:
                    object.put(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    object.put(key, readableMap.getDouble(key));
                    break;
                case String:
                    object.put(key, readableMap.getString(key));
                    break;
                case Map:
                    object.put(key, convertMapToJson(readableMap.getMap(key)));
                    break;
                case Array:
                    object.put(key, convertArrayToJson(readableMap.getArray(key)));
                    break;
            }
        }
        return object;
    }

    private static JSONArray convertArrayToJson(ReadableArray readableArray) throws JSONException {
        JSONArray array = new JSONArray();
        for (int i = 0; i < readableArray.size(); i++) {
            switch (readableArray.getType(i)) {
                case Null:
                    break;
                case Boolean:
                    array.put(readableArray.getBoolean(i));
                    break;
                case Number:
                    array.put(readableArray.getDouble(i));
                    break;
                case String:
                    array.put(readableArray.getString(i));
                    break;
                case Map:
                    array.put(convertMapToJson(readableArray.getMap(i)));
                    break;
                case Array:
                    array.put(convertArrayToJson(readableArray.getArray(i)));
                    break;
            }
        }
        return array;
    }
}