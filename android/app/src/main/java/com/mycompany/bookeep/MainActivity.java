package com.mycompany.bookeep;

import android.content.Intent;

import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;

import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;

public class MainActivity extends ReactActivity {

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  @Override
  protected String getMainComponentName() {
    return "Bookeep";
  }

  public static Promise reactNativePromise;

  public void centralizedLogin() {
    CentralLoginActivity.reactNativePromise = reactNativePromise;
    Intent intent = new Intent(this, CentralLoginActivity.class);
    startActivity(intent);
  }

  public void idpSignOn(ReactApplicationContext context, Node node, NodeListener listener) {
    IdpSignOnActivity.reactNativePromise = reactNativePromise;
    IdpSignOnActivity.currentNode = node;
    IdpSignOnActivity.listener = listener;
    Intent intent = new Intent(this, IdpSignOnActivity.class);
    startActivity(intent);
  }

  public void webAuthNRegister(ReactApplicationContext context, Node node, NodeListener listener) {
    WebAuthNRegisterActivity.reactNativePromise = reactNativePromise;
    WebAuthNRegisterActivity.currentNode = node;
    WebAuthNRegisterActivity.listener = listener;
    Intent intent = new Intent(this, WebAuthNRegisterActivity.class);
    startActivity(intent);
  }

  public void webAuthNSignOn(ReactApplicationContext context, Node node, NodeListener listener) {
    WebAuthNSignOnActivity.reactNativePromise = reactNativePromise;
    WebAuthNSignOnActivity.currentNode = node;
    WebAuthNSignOnActivity.listener = listener;
    Intent intent = new Intent(this, WebAuthNSignOnActivity.class);
    startActivity(intent);
  }
}
