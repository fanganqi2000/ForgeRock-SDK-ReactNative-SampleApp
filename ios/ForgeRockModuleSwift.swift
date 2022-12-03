//
//  ForgeRockModuleSwift.swift
//  Bookeep
//
//  Copyright (c) 2021 ForgeRock. All rights reserved.
//
//  This software may be modified and distributed under the terms
//  of the MIT license. See the LICENSE file for details.
//

import Foundation
import FRAuth
import FRCore
import UIKit

@objc(ForgeRockModuleSwift)
public class ForgeRockModuleSwift: NSObject {
  var currentNode: Node?
  
  var excludeConsentResult: WebAuthnUserConsentResult?
  var createNewKeyConsentResult: WebAuthnUserConsentResult?
  
  @objc static func requiresMainQueueSetup() -> Bool {
      return false
  }
  
  @objc func frAuthStart() {
    // Set log level according to your needs
    FRLog.setLogLevel([.all])
    
    do {
        try FRAuth.start()
    }
    catch {
      FRLog.e(error.localizedDescription)
    }
  }
  
  @objc func performUserLoginWithoutUIWithCallback(_ callback: @escaping RCTResponseSenderBlock) {
    FRUser.login { (user, node, error) in
        if let node = node {
          self.handleNode(user, node, error, callback: callback)
        } else {
          if let error = error {
            callback([["error": error.localizedDescription]])
            return
          }
        }
    }
  }
  
  @objc func authenticateWithTree(_ tree: String, callback: @escaping RCTResponseSenderBlock) {
    FRSession.authenticate(authIndexValue: tree) { (token: Token?, node, error) in
      if let node = node {
        self.handleNode(token, node, error, callback: callback)
      } else {
        if let error = error {
          callback([["error": error.localizedDescription]])
          return
        }
        if let token = token {
          guard let user = FRUser.currentUser else {
              // If no currently authenticated user is found, log error
              FRLog.e("FRDevice.currentDevice does not exist - SDK not initialized")
              callback([["error": "FRDevice.currentDevice does not exist - SDK not initialized"]])
              return
          }
          user.getAccessToken { (user, error) in
              if let error = error {
                FRLog.e(String(describing: error))
                callback([["error", error.localizedDescription]])
                return
              } else if let user = user {
                let encoder = JSONEncoder()
                encoder.outputFormatting = .prettyPrinted
                if let token = user.token, let data = try? encoder.encode(token), let jsonAccessToken = String(data: data, encoding: .utf8) {
                  FRLog.i(jsonAccessToken)
                  callback([["accessToken": jsonAccessToken, "error": "User is already authenticated"]])
                }
              }
              else {
                FRLog.e("Invalid state: AccessToken returns no result")
                callback([["error", "Invalid state: AccessToken returns no result"]])
              }
          }
        }
      }
    }
  }
  
  @objc(loginWithBrowser:rejecter:)
  func loginWithBrowser(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      let keyWindow = UIApplication.shared.windows.filter {$0.isKeyWindow}.first

      if var topController = keyWindow?.rootViewController {
          while let presentedViewController = topController.presentedViewController {
              topController = presentedViewController
          }
        FRUser.browser()?
            .set(presentingViewController: topController)
            .set(browserType: .authSession)
            .setCustomParam(key: "service", value: "SimpleLogin")
            .build().login { (user, error) in
              let encoder = JSONEncoder()
              encoder.outputFormatting = .prettyPrinted
  
              if let authError = error as? AuthError {
                switch authError {
                case .userAlreadyAuthenticated:
                    Browser.currentBrowser = nil
                    guard let user = FRUser.currentUser else {
                        // If no currently authenticated user is found, log error
                        FRLog.e("FRDevice.currentDevice does not exist - SDK not initialized")
                        resolve(["error": "FRDevice.currentDevice does not exist - SDK not initialized"])
                        return
                    }
                    user.getAccessToken { (user, error) in
                        if let error = error {
                          FRLog.e(String(describing: error))
                          resolve(["error", error.localizedDescription])
                          return
                        } else if let user = user {
                          let encoder = JSONEncoder()
                          encoder.outputFormatting = .prettyPrinted
                          if let token = user.token, let data = try? encoder.encode(token), let jsonAccessToken = String(data: data, encoding: .utf8) {
                            FRLog.i(jsonAccessToken)
                            resolve(["accessToken": jsonAccessToken, "error": "User is already authenticated"])
                          }
                        }
                        else {
                          FRLog.e("Invalid state: AccessToken returns no result")
                          resolve(["error", "Invalid state: AccessToken returns no result"])
                        }
                    }
                    break
                default:
                    reject("error", authError.localizedDescription, authError)
                    break
                }
                return
              } else if let error = error {
                reject("error", error.localizedDescription, error)
                return
              }
              if let user = user, let token = user.token, let data = try? encoder.encode(token), let jsonAccessToken = String(data: data, encoding: .utf8) {
                
                resolve(["accessToken": jsonAccessToken])
              } else {
                reject("error", "Unable to encode token response", nil)
              }
        }
      } else {
        reject("error", "Top Controller not found", nil)
      }
    }
  }
 
  @objc(loginWithIdP:rejecter:)
  func loginWithIdP(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      let keyWindow = UIApplication.shared.windows.filter {$0.isKeyWindow}.first

      if var topController = keyWindow?.rootViewController {
          while let presentedViewController = topController.presentedViewController {
              topController = presentedViewController
          }
          if let node = self.currentNode {
            for nodeCallback in node.callbacks {
              if let thisCallback = nodeCallback as? IdPCallback {
                thisCallback.signIn(presentingViewController: topController) { (token, tokenType, error) in
                    
                    if let error = error {
                        FRLog.e("An error occurred from IdPHandler.signIn: \(error.localizedDescription)")
                        reject("error", error.localizedDescription, error)
                        return
                    }
                    else {
                        if let _ = token, let tokenType = tokenType {
                            FRLog.v("Credentials received - Token Type: \(tokenType) from \(thisCallback.idpClient.provider)")
                        }
                        FRLog.v("Social Login Provider credentials received; submitting the authentication tree to proceed")
                        resolve(["type": "IdpCallback"])
                    }
                }
              }
            }
          }
      } else {
        reject("error", "Top Controller not found", nil)
      }
    }
  }
  
  @objc(registerWebAuthN:rejecter:)
  func registerWebAuthN(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    if let node = self.currentNode {
      for nodeCallback in node.callbacks {
        if let thisCallback = nodeCallback as? WebAuthnRegistrationCallback {
          thisCallback.delegate = self
          thisCallback.register(node: node, onSuccess: { (attestation) in
            DispatchQueue.main.async {
              resolve(["type": "WebAuthnRegistrationCallback"])
            }
          }) { (error) in
            DispatchQueue.main.async {
              reject("error", error.localizedDescription, error)
            }
            FRLog.e(error.localizedDescription)
          }
        }
      }
    }
  }
  
  @objc(loginWebAuthN:rejecter:)
  func loginWebAuthN(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    if let node = self.currentNode {
      for nodeCallback in node.callbacks {
        if let thisCallback = nodeCallback as? WebAuthnAuthenticationCallback {
          thisCallback.delegate = self
          thisCallback.authenticate(node: node, onSuccess: { (assertion) in
            DispatchQueue.main.async {
              resolve(["type": "WebAuthnAuthenticationCallback"])
            }
          }) { (error) in
            DispatchQueue.main.async {
              resolve(["error": error.localizedDescription])
              //reject("error", error.localizedDescription, error)
            }
            FRLog.e(error.localizedDescription)
          }
        }
      }
    }
  }
  
  @objc(getDeviceInformation:rejecter:)
  func getDeviceInformation(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    guard let _ = FRDevice.currentDevice else {
        // If SDK is not initialized, then don't perform
        FRLog.e("FRDevice.currentDevice does not exist - SDK not initialized")
        reject("error", "FRDevice.currentDevice does not exist - SDK not initialized", nil)
        return
    }
    
    FRDeviceCollector.shared.collect { (result) in
        FRLog.i("\(result)")
        resolve([result])
    }
  }
  
  @objc func nextWithUserCompletion(_ array: NSArray, callback: @escaping RCTResponseSenderBlock) {
    if let node = self.currentNode {
      // If the array is empty there are no user inputs. This can happen in callbacks like the DeviceProfileCallback, that do not require user interaction.
      // Other callbacks like SingleValueCallback, will return the user inputs in an array of dictionaries [[String:String]] with the keys: identifier and text
      if array.count == 0 {
        for nodeCallback in node.callbacks {
          if let thisCallback = nodeCallback as? DeviceProfileCallback {
            //let semaphore = DispatchSemaphore(value: 1)
            //semaphore.wait()
            thisCallback.execute { _ in
              //semaphore.signal()
              node.next(completion: { (token: Token?, node, error) in
                if let node = node {
                  self.handleNode(token, node, error, callback: callback)
                } else {
                  if let error = error {
                    callback([["error": error.localizedDescription]])
                    return
                  }
                  
                  let encoder = JSONEncoder()
                  encoder.outputFormatting = .prettyPrinted
                  if let token = token, let data = try? encoder.encode(token), let jsonAccessToken = String(data: data, encoding: .utf8) {
                    
                    callback([["accessToken": jsonAccessToken]])
                  } else {
                    callback([["error": "Unable to encode token response"]])
                  }
                }
              })
            }
          } else if nodeCallback.isKind(of: IdPCallback.self) || nodeCallback.isKind(of: WebAuthnRegistrationCallback.self) || nodeCallback.isKind(of: WebAuthnAuthenticationCallback.self) {
            node.next(completion: { (token: Token?, node, error) in
              if let node = node {
                self.handleNode(token, node, error, callback: callback)
              } else {
                if let error = error {
                  callback([["error": error.localizedDescription]])
                  return
                }
                
                let encoder = JSONEncoder()
                encoder.outputFormatting = .prettyPrinted
                if let token = token, let data = try? encoder.encode(token), let jsonAccessToken = String(data: data, encoding: .utf8) {
                  
                  callback([["accessToken": jsonAccessToken]])
                } else {
                  callback([["error": "Unable to encode token response"]])
                }
              }
            })
          }
        }
      } else {
        for dict in array {
          if let dictionary = dict as? NSDictionary, let callbackType = dictionary["identifier"] as? String {
            var cnt = 0;
            for nodeCallback in node.callbacks {
              if nodeCallback.isKind(of: NameCallback.self) || nodeCallback.isKind(of: PasswordCallback.self) || nodeCallback.isKind(of: ChoiceCallback.self) {
                let value = dictionary["text"] as? String
                let thisCallback = nodeCallback as! SingleValueCallback
                if (thisCallback.type + String(cnt)) == callbackType {
                  thisCallback.setValue(value)
                }
              } else if nodeCallback.isKind(of: BooleanAttributeInputCallback.self) {
                let value = dictionary["text"] as? Bool
                let thisCallback = nodeCallback as! BooleanAttributeInputCallback
                if (thisCallback.type + String(cnt)) == callbackType {
                  thisCallback.setValue(value)
                }
              } else if nodeCallback.isKind(of: SelectIdPCallback.self) {
                let value = dictionary["text"] as? String
                let thisCallback = nodeCallback as! SelectIdPCallback
                if thisCallback.type == callbackType {
                  thisCallback.setValue(value)
                }
              }
              cnt+=1;
            }
          }
        }
        
        node.next(completion: { (token: Token?, node, error) in
          if let node = node {
            self.handleNode(token, node, error, callback: callback)
          } else {
            if let error = error {
              callback([["error": error.localizedDescription]])
              return
            }
            
            let encoder = JSONEncoder()
            encoder.outputFormatting = .prettyPrinted
            if let token = token, let data = try? encoder.encode(token), let jsonAccessToken = String(data: data, encoding: .utf8) {
              
              callback([["accessToken": jsonAccessToken]])
            } else {
              callback([["error": "Unable to encode token response"]])
            }
          }
        })
      }
    }
  }
  
  @objc func performUserLogout() {
    FRUser.currentUser?.logout()
  }
  
  @objc(getAccessToken:rejecter:)
  func getAccessToken(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    guard let user = FRUser.currentUser else {
        // If no currently authenticated user is found, log error
        FRLog.e("FRDevice.currentDevice does not exist - SDK not initialized")
        reject("error", "FRDevice.currentDevice does not exist - SDK not initialized", nil)
        return
    }
    
    user.getAccessToken { (user, error) in
        if let error = error {
          FRLog.e(String(describing: error))
          reject("error", error.localizedDescription, error)
        } else if let user = user {
          let encoder = JSONEncoder()
          encoder.outputFormatting = .prettyPrinted
          if let token = user.token, let data = try? encoder.encode(token), let jsonAccessToken = String(data: data, encoding: .utf8) {
            FRLog.i(jsonAccessToken)
            resolve(["accessToken": jsonAccessToken])
          }
        }
        else {
          FRLog.e("Invalid state: AccessToken returns no result")
          reject("error", "Invalid state: AccessToken returns no result", nil)
        }
    }
  }
  
  @objc(getUserInfo:rejecter:)
  func getUserInfo(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
      
      guard let user = FRUser.currentUser else {
          // If no currently authenticated user is found, log error
          FRLog.e("FRDevice.currentDevice does not exist - SDK not initialized")
          reject("error", "FRDevice.currentDevice does not exist - SDK not initialized", nil)
          return
      }

      // If FRUser.currentUser exists, perform getUserInfo
      user.getUserInfo { (userInfo, error) in
          if let error = error {
            FRLog.e(String(describing: error))
            reject("error", error.localizedDescription, error)
          }
          else if let userInfo = userInfo {
            FRLog.i(userInfo.debugDescription)
            resolve(userInfo.userInfo)
          }
          else {
            FRLog.e("Invalid state: UserInfo returns no result")
            reject("error", "Invalid state: UserInfo returns no result", nil)
          }
      }
  }
  
  private func handleNode(_ result: Any?, _ node: Node?, _ error: Error?, callback: @escaping RCTResponseSenderBlock) {
    if let node = node {
      var callbacksDictionary = Dictionary<String, Dictionary<String, Any?>>()
      var indexArr: [String] = []
      var cnt = 0;
      //var disabled = false;
      for authCallback in node.callbacks {
        if authCallback.isKind(of: NameCallback.self) || authCallback.isKind(of: ValidatedCreateUsernameCallback.self) || authCallback.isKind(of: PasswordCallback.self) || authCallback.isKind(of: ValidatedCreateUsernameCallback.self) {
          self.currentNode = node
          let thisCallback: SingleValueCallback = authCallback as! SingleValueCallback
          callbacksDictionary[thisCallback.type + String(cnt)] = ["prompt": thisCallback.prompt, "type": thisCallback.type]
          indexArr.append(thisCallback.type + String(cnt))
        } else if (authCallback.isKind(of: ChoiceCallback.self)) {
          self.currentNode = node
          let thisCallback: ChoiceCallback = authCallback as! ChoiceCallback
          callbacksDictionary[thisCallback.type + String(cnt)] = ["prompt": thisCallback.prompt, "type": thisCallback.type, "choices": thisCallback.choices]
          indexArr.append(thisCallback.type + String(cnt))
        } else if (authCallback.isKind(of: BooleanAttributeInputCallback.self)) {
          self.currentNode = node
          let thisCallback: BooleanAttributeInputCallback = authCallback as! BooleanAttributeInputCallback
          callbacksDictionary[thisCallback.type + String(cnt)] = ["name": thisCallback.name, "prompt": thisCallback.name, "type": thisCallback.type, "checked": thisCallback.getValue()]
          indexArr.append(thisCallback.type + String(cnt))
        } else if (authCallback.isKind(of: DeviceProfileCallback.self)) {
          self.currentNode = node
          let thisCallback: DeviceProfileCallback = authCallback as! DeviceProfileCallback
          callbacksDictionary[thisCallback.type] = ["prompt": nil, "type": thisCallback.type, "choices": nil]
          indexArr.append(thisCallback.type)
        } else if (authCallback.isKind(of: SelectIdPCallback.self)) {
          self.currentNode = node
          let thisCallback: SelectIdPCallback = authCallback as! SelectIdPCallback
          var providerArr: [String] = []
          for theProvider in thisCallback.providers {
            if (theProvider.provider != "localAuthentication") {
              providerArr.append(theProvider.provider);
            }
          }
          callbacksDictionary[thisCallback.type + String(cnt)] = ["prompt": nil, "type": thisCallback.type, "providers": providerArr]
          indexArr.append(thisCallback.type + String(cnt))
        } else if (authCallback.isKind(of: IdPCallback.self)) {
          self.currentNode = node
          let thisCallback: IdPCallback = authCallback as! IdPCallback
          callbacksDictionary[thisCallback.type + String(cnt)] = ["type": thisCallback.type]
          indexArr.append(thisCallback.type + String(cnt))
        } else if (authCallback.isKind(of: WebAuthnRegistrationCallback.self)) {
          self.currentNode = node
          let thisCallback: WebAuthnRegistrationCallback = authCallback as! WebAuthnRegistrationCallback
/*          disabled = true
          thisCallback.delegate = self
          thisCallback.register(node: node, onSuccess: { (attestation) in
            DispatchQueue.main.async {
              disabled = false
              callback([callbacksDictionary])
            }
          }) { (error) in
            DispatchQueue.main.async {
              disabled = false
              callback([callbacksDictionary])
            }
            FRLog.e(error.localizedDescription)
          }*/
          callbacksDictionary[thisCallback.type + String(cnt)] = ["type": thisCallback.type]
          indexArr.append(thisCallback.type + String(cnt))
        } else if (authCallback.isKind(of: WebAuthnAuthenticationCallback.self)) {
          self.currentNode = node
          let thisCallback: WebAuthnAuthenticationCallback = authCallback as! WebAuthnAuthenticationCallback
/*          disabled = true
          thisCallback.delegate = self
          thisCallback.authenticate(node: node, onSuccess: { (assertion) in
              DispatchQueue.main.async {
                disabled = false
                callback([callbacksDictionary])
              }
          }) { (error) in
              DispatchQueue.main.async {
                disabled = false
                callback([callbacksDictionary])
              }
              FRLog.e(error.localizedDescription)
          }*/
          callbacksDictionary[thisCallback.type + String(cnt)] = ["type": thisCallback.type]
          indexArr.append(thisCallback.type + String(cnt))
        }
        cnt+=1;
      }
      callbacksDictionary["indexArray"] = ["indexArr": indexArr]
      //if (!disabled) {
        callback([callbacksDictionary])
      //}
    }
  }
}

extension ForgeRockModuleSwift: PlatformAuthenticatorRegistrationDelegate {
    public func excludeCredentialDescriptorConsent(consentCallback: @escaping WebAuthnUserConsentCallback) {
      DispatchQueue.main.async {
        let keyWindow = UIApplication.shared.windows.filter {$0.isKeyWindow}.first

        if var topController = keyWindow?.rootViewController {
          while let presentedViewController = topController.presentedViewController {
            topController = presentedViewController
          }
          let alert = UIAlertController(title: "Exclude Credentials", message: nil, preferredStyle: .alert)
          let cancelAction = UIAlertAction(title: "Cancel", style: .cancel, handler: { (_) in
              consentCallback(.reject)
          })
          let allowAction = UIAlertAction(title: "Allow", style: .default) { (_) in
              consentCallback(.allow)
          }
          alert.addAction(cancelAction)
          alert.addAction(allowAction)

          topController.present(alert, animated: true, completion: nil)
        } 
      }
    }
    
    public func createNewCredentialConsent(keyName: String, rpName: String, rpId: String?, userName: String, userDisplayName: String, consentCallback: @escaping WebAuthnUserConsentCallback) {
      DispatchQueue.main.async {
        let keyWindow = UIApplication.shared.windows.filter {$0.isKeyWindow}.first

        if var topController = keyWindow?.rootViewController {
          while let presentedViewController = topController.presentedViewController {
            topController = presentedViewController
          }
          let alert = UIAlertController(title: "Create Credentials", message: "KeyName: \(keyName) | rpName: \(rpName) | userName: \(userName)", preferredStyle: .alert)
          let cancelAction = UIAlertAction(title: "Cancel", style: .cancel, handler: { (_) in
              consentCallback(.reject)
          })
          let allowAction = UIAlertAction(title: "Allow", style: .default) { (_) in
              consentCallback(.allow)
          }
          alert.addAction(cancelAction)
          alert.addAction(allowAction)

          topController.present(alert, animated: true, completion: nil)
        }
      }
    }
}

extension ForgeRockModuleSwift: PlatformAuthenticatorAuthenticationDelegate {
    public func selectCredential(keyNames: [String], selectionCallback: @escaping WebAuthnCredentialsSelectionCallback) {
      DispatchQueue.main.async {
        let presentedViewController = RCTPresentedViewController()
          
        let actionSheet = UIAlertController(title: "Select Credentials", message: nil, preferredStyle: .actionSheet)
        
        for keyName in keyNames {
            actionSheet.addAction(UIAlertAction(title: keyName, style: .default, handler: { (action) in
                selectionCallback(keyName)
            }))
        }
          
        actionSheet.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: { (action) in
            selectionCallback(nil)
        }))
          
        if actionSheet.popoverPresentationController != nil {
          actionSheet.popoverPresentationController?.sourceView = presentedViewController!.view
          actionSheet.popoverPresentationController?.sourceRect = presentedViewController!.view.bounds
        }
        presentedViewController!.present(actionSheet, animated: true, completion: nil)
      }
    }
}

