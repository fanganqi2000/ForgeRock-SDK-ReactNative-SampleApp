// Homescreen.js
import React, { Component, useEffect, useState } from 'react';
import { Button, View, Text, NativeModules, unstable_enableLogBox } from 'react-native';
import { proceedWithAuthenticated } from './helper';
import useTimeBlockedCallback from '../hooks/useTimeBlockedCallback'
import DialogInput from 'react-native-dialog-input';

const { ForgeRockModule } = NativeModules;

export default function Homescreen(props) {
    const [ dialogVisible, setDialogVisible ] = useState(false);

    // Similar to componentDidMount and componentDidUpdate:
    useEffect(() => {
        if (!props.route.params) {
            console.log("xxxxxxxxxxxxxxxxxxxxxx");
            console.log("Sart ForgeRock Auth");
            console.log("xxxxxxxxxxxxxxxxxxxxxx");
            ForgeRockModule.frAuthStart();
        }
        if (props.route.params && props.route.params.errorMsg) {
            ForgeRockModule.performUserLogout();
            alert(props.route.params.errorMsg);
            props.route.params.errorMsg = "";
        }
    });

    const onPressFRSession = () => {
        setDialogVisible(true);
    };

    const onPressBrowser = useTimeBlockedCallback(async () => {
        const loginResponseArray = await ForgeRockModule.loginWithBrowser();
        console.log("xxxxxxxxxxxxxxxxxxxxxx");
        console.log("Access Token: " + loginResponseArray.accessToken);
        console.log("Error: " + loginResponseArray.error);
        console.log("xxxxxxxxxxxxxxxxxxxxxx");
        loginResponseArray.agentType = "browser";
        if (loginResponseArray.accessToken || loginResponseArray.error == "User is already authenticated") {
            proceedWithAuthenticated(props, loginResponseArray, []);
        }
    });

    const onPress = useTimeBlockedCallback(() => {
        ForgeRockModule.performUserLoginWithoutUIWithCallback(async (responseArray) => {
            console.log("xxxxResponse Arrayxxxx");
            console.log(responseArray);
            console.log("xxxxxxxxxxxxxxxxxxxxxx");
            if (responseArray.error == "User is already authenticated") {
                const userInfoResponse = await ForgeRockModule.getUserInfo();
                responseArray.accessToken = userInfoResponse.accessToken;
                console.log("xxxxResponse Array with tokenxxxx");
                console.log(userInfoResponse);
                console.log("xxxxxxxxxxxxxxxxxxxxxx");
                proceedWithAuthenticated(props, responseArray, []);
            } else {
                var callbacks = [];
                if (responseArray.indexArray) { //from iOS in unordered dictionary
                    console.log("xxxxxxIndex Arrayxxxxx");
                    console.log(responseArray.indexArray);
                    console.log("xxxxxxxxxxxxxxxxxxxxxx");
                    responseArray.indexArray.indexArr.forEach(element => {
                        callbacks.push(responseArray[element]);    
                    });
                } else {
                    callbacks = Object.values(responseArray).map(item => ({ prompt: item.prompt, type: item.type, choices: item.choices, providers: item.providers, name: item.name, checked: item.checked }));
                    console.log("xxxxxxxx--callbacks---xxxxxxxx");
                    console.log(callbacks);
                }
                props.navigation.navigate('Login', callbacks);
            }
        });
    });

    const showDialog = (isShown) => {
        setDialogVisible(false);
      };

    const sendInput = useTimeBlockedCallback((treeName) => {
        ForgeRockModule.authenticateWithTree(treeName, (responseArray) => {
            console.log("xxxxResponse Arrayxxxx");
            console.log(responseArray);
            console.log("xxxxxxxxxxxxxxxxxxxxxx");
            if (responseArray.accessToken) {
                responseArray.error = "User is already authenticated";
                proceedWithAuthenticated(props, responseArray, []);               
            } else if (responseArray.error) {
                setDialogVisible(false);
                var error = JSON.parse(responseArray.error);
                alert(error.code + " :: " + error.message);          
            } else {
                var callbacks = [];
                if (responseArray.indexArray) { //from iOS in unordered dictionary
                    console.log("xxxxxxIndex Arrayxxxxx");
                    console.log(responseArray.indexArray);
                    console.log("xxxxxxxxxxxxxxxxxxxxxx");
                    responseArray.indexArray.indexArr.forEach(element => {
                        callbacks.push(responseArray[element]);    
                    });
                } else {
                    callbacks = Object.values(responseArray).map(item => ({ prompt: item.prompt, type: item.type, choices: item.choices, providers: item.providers, name: item.name, checked: item.checked }));
                }
                props.navigation.navigate('Login', callbacks);
            }
            setDialogVisible(false);
        });
    });

    return (
        <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
            <Text>Welcome to Bookeep{"\n"}</Text>
            <Button
                title="Login"
                onPress={onPress}
            />
            <Text>{"\n"}</Text>
            <Button
                title="Login With Browser"
                onPress={onPressBrowser}
            />
            <Text>{"\n"}</Text>
            <Button
                title="Login With FRSession - Tree Login"
                onPress={onPressFRSession}
            />
            <DialogInput isDialogVisible={dialogVisible}
                title={"AM Intelligent Access Tree"}
                message={"Please enter tree name"}
                hintInput ={""}
                submitInput={ (inputText) => {sendInput(inputText)} }
                closeDialog={ () => {showDialog(false)}}>
            </DialogInput>
        </View>
    );
}