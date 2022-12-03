//
//  ForgeRockModuleSwift.m
//  Bookeep
//
//  Copyright (c) 2021 ForgeRock. All rights reserved.
//
//  This software may be modified and distributed under the terms
//  of the MIT license. See the LICENSE file for details.
//

#import <Foundation/Foundation.h>
#import "React/RCTBridgeModule.h"

@interface RCT_EXTERN_REMAP_MODULE(ForgeRockModule, ForgeRockModuleSwift, NSObject)
RCT_EXTERN_METHOD(frAuthStart)
RCT_EXTERN_METHOD(performUserLoginWithoutUIWithCallback: (RCTResponseSenderBlock)callback)
RCT_EXTERN_METHOD(nextWithUserCompletion: (NSArray<NSDictionary*> *)array callback:(RCTResponseSenderBlock)callback)
RCT_EXTERN_METHOD(performUserLogout)
RCT_EXTERN_METHOD(loginWithBrowser: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
RCT_EXTERN_METHOD(loginWithIdP: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
RCT_EXTERN_METHOD(registerWebAuthN: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
RCT_EXTERN_METHOD(loginWebAuthN: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
RCT_EXTERN_METHOD(getUserInfo: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
RCT_EXTERN_METHOD(getAccessToken: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
RCT_EXTERN_METHOD(authenticateWithTree: (NSString *)tree callback:(RCTResponseSenderBlock)callback)
RCT_EXTERN_METHOD(getDeviceInformation: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)

@end
