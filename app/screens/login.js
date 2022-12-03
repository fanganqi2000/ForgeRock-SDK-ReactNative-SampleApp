import React, { useState, Component } from 'react';
import { Text, View, TextInput, StyleSheet, TouchableHighlight, NativeModules, LogBox } from 'react-native';
import t from 'tcomb-form-native';
import { proceedWithAuthenticated } from './helper';
import useTimeBlockedCallback from '../hooks/useTimeBlockedCallback'
import { SocialIcon } from 'react-native-elements';

const { ForgeRockModule } = NativeModules;

LogBox.ignoreAllLogs();

const Form = t.form.Form;
export default function Login(props) {
    let showNext = true;
    let showFaceBook = false;
    let showGoogle = false;
    let showApple = false;
    let providerMap = {};
    const [count, setCount] = useState(0);
    const [showText, setShowText] = useState(false);
    let callbacks = props.route.params;
    let structObj = {};
    let passwordPrompt = "";
    const values = {};
    callbacks.forEach(async element => {
        console.log("xxxxxxxxxxxxxxxxxxxxxx");
        console.log("Callback Type: " + element.type);
        console.log("xxxxxxxxxxxxxxxxxxxxxx");
        console.log(element);
        if (element.type == "BooleanAttributeInputCallback") {
            structObj[element.prompt] = t.Boolean;
            values[element.prompt] = element.checked;
        } else if (element.type == "SelectIdPCallback") {
            element.providers.forEach((providerStr, index) => {
                if (providerStr.toUpperCase().indexOf("FACEBOOK") > -1) {
                    showFaceBook = true;
                    providerMap["FACEBOOK"] = providerStr;
                } else if (providerStr.toUpperCase().indexOf("GOOGLE") > -1) {
                    showGoogle = true;
                    providerMap["GOOGLE"] = providerStr;
                } else if (providerStr.toUpperCase().indexOf("APPLE") > -1) {
                    showApple = true;
                    providerMap["APPLE"] = providerStr;
                }
            });
        } else if (element.type == "WebAuthnRegistrationCallback") {
            console.log("======" + element.type);
            showNext = false;
            if (!showText) { 
                const loginResponseArray = await ForgeRockModule.registerWebAuthN();
                if (loginResponseArray.error) {
                    //Login failed. Go to the Homescreen
                    console.log("xxxxxxxxxxxxxxxxxxxxxx");
                    console.log("Error: " + loginResponseArray.error);
                    console.log("xxxxxxxxxxxxxxxxxxxxxx");
                    var error = loginResponseArray.error;
                    try { 
                        //For Android
                        error = JSON.parse(loginResponseArray.error);
                        props.navigation.navigate('Homescreen', { errorMsg: error.code + " :: " + error.message });
                    } catch (e) {
                        props.navigation.navigate('Homescreen', { errorMsg: error });
                    }
                } else {
                    console.log("====== proceed to show confirmation page");
                    setShowText(true);
                }
            }
            console.log(showText);
            console.log("-----------++++++++");
        } else if (element.type == "WebAuthnAuthenticationCallback") {
            console.log("+++++++" + element.type);
            showNext = false;
            const loginResponseArray = await ForgeRockModule.loginWebAuthN();
            console.log(loginResponseArray);
            console.log("+++++++ proceed to next ...");
            nextStep([]);
        } else if (element.choices) {
            let enumObj = {};
            element.choices.forEach((choice, index) => enumObj[index] = choice);
            structObj[element.prompt] = t.enums(enumObj);
        } else if (element.prompt) {
            structObj[element.prompt] = t.String;
            if (element.type == "PasswordCallback") {
                passwordPrompt = element.prompt;
            }
        } else if (element.type == "DeviceProfileCallback") {
            showNext = false;
            nextStep([]);
        } else if (element.type == "IdPCallback") {
            console.log("======" + element.type);
            showNext = false;
            const loginResponseArray = await ForgeRockModule.loginWithIdP();
            console.log(loginResponseArray);
            console.log("====== proceed to next ...");
            nextStep([]);
        }
    });
    form = t.struct(structObj);

    const options = {fields: {}};
    
    if (passwordPrompt != "") {
        options["fields"][passwordPrompt] = {secureTextEntry: true};
    }

    const onPressFaceBook = useTimeBlockedCallback((provider) => {
        console.log("xxxxxFaceBookxxxxxxx");
        const requestArray = [{identifier: "SelectIdPCallback", text: provider}];
        nextStep(requestArray);
    });

    const onPressGoogle = useTimeBlockedCallback(() => {
        console.log("xxxxxGooglexxxxxxx");
    });

    const onPressApple = useTimeBlockedCallback(() => {
        console.log("xxxxxApplexxxxxxx");
    });

    const onPress = useTimeBlockedCallback(() => {
        const value = this._form.getValue(); // use that ref to get the form value
        console.log("xxxxxForm Valuexxxxxxx");
        console.log(value);
        console.log("xxxxxxxxxxxxxxxxxxxxxx");
        if (value) {
            const requestArray = Object.values(callbacks).map((item, index) => ({ identifier: item.type + index, text: value[item.prompt] }));
            console.log("xxxxxRequest Arrayxxxx");
            console.log(requestArray);
            console.log("xxxxxxxxxxxxxxxxxxxxxx");
            nextStep(requestArray);
        }
    });

    const onCancel = useTimeBlockedCallback(() => {
        props.navigation.navigate('Homescreen');
    });

    function nextStep(requestArray) {
        ForgeRockModule.nextWithUserCompletion(requestArray, async (responseArray) => {
            console.log("xxxxxxxxxxxxxxxxxxxxxx");
            console.log("accessToken: " + responseArray.accessToken);
            console.log("xxxxxxxxxxxxxxxxxxxxxx");
            if (responseArray.accessToken) {
                proceedWithAuthenticated(props, responseArray, callbacks);
            }
            else if (responseArray.error) {
                //Login failed. Go to the Homescreen
                console.log("xxxxxxxxxxxxxxxxxxxxxx");
                console.log("Error: " + responseArray.error);
                console.log("xxxxxxxxxxxxxxxxxxxxxx");
                var error = responseArray.error;
                try { 
                    //For Android
                    error = JSON.parse(responseArray.error);
                    props.navigation.navigate('Homescreen', { errorMsg: error.code + " :: " + error.message });
                } catch (e) {
                    props.navigation.navigate('Homescreen', { errorMsg: error });
                }
            }
            else {
                console.log("xxxxResponse Arrayxxxx");
                console.log(responseArray);
                console.log("xxxxxxxxxxxxxxxxxxxxxx");
                var newCallbacks = [];
                if (responseArray.indexArray) { //from iOS in unordered dictionary
                    console.log("xxxxxxIndex Arrayxxxxx");
                    console.log(responseArray.indexArray);
                    console.log("xxxxxxxxxxxxxxxxxxxxxx");
                    responseArray.indexArray.indexArr.forEach(element => {
                        newCallbacks.push(responseArray[element]);    
                    });
                } else {
                    newCallbacks = Object.values(responseArray).map(item => ({ prompt: item.prompt, type: item.type, choices: item.choices, providers: item.providers, name: item.name, checked: item.checked }));
                }
                props.route.params = newCallbacks
                setCount(count + 1);
            }
        });
    };

    return (
        <View style={styles.container}>
            {showFaceBook ? (
                <SocialIcon
                            button
                            title="Sign In Facebook"
                            type="facebook"
                            onPress={onPressFaceBook.bind(this, providerMap["FACEBOOK"])}
                          />) : null}
            {showGoogle ? (
                <SocialIcon
                            button
                            title="Sign In Google"
                            type="google"
                            onPress={onPressGoogle}
                        />) : null}
            {showApple ? (
                <SocialIcon
                            button
                            title="Sign In Apple"
                            type="apple"
                            onPress={onPressApple}
                        />) : null}
            <Form
                ref={c => this._form = c}
                type={form}
                options={options}
                value={values}
            />
            {showText ? (
                <View>
                <Text style={{color: 'green', margin: 10,}}>WebAuthN Registration</Text>
                    <View style={{flexDirection:"row"}}>
                        <View style={{flex:1}}>
                            <TouchableHighlight style={styles.buttonC} onPress={onCancel} underlayColor='#f59def'>
                                <Text style={styles.buttonText}>Cancel</Text>
                            </TouchableHighlight>
                        </View>
                        <Text>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</Text>
                        <View style={{flex:1}}>
                            <TouchableHighlight style={styles.button} onPress={onPress} underlayColor='#99d9f4'>
                                <Text style={styles.buttonText}>Next</Text>
                            </TouchableHighlight>
                        </View>
                    </View>
                </View>
            ) : null}
            {showNext ? (
                <TouchableHighlight style={styles.button} onPress={onPress} underlayColor='#99d9f4'>
                    <Text style={styles.buttonText}>Next</Text>
                </TouchableHighlight>
            ) : null}
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        justifyContent: 'center',
        marginTop: 50,
        padding: 20,
        backgroundColor: '#ffffff',
    },
    buttonText: {
        fontSize: 18,
        color: 'white',
        alignSelf: 'center'
    },
    button: {
        height: 36,
        backgroundColor: '#48BBEC',
        borderColor: '#48BBEC',
        borderWidth: 1,
        borderRadius: 8,
        marginBottom: 10,
        alignSelf: 'stretch',
        justifyContent: 'center'
    },
    buttonC: {
        height: 36,
        backgroundColor: '#e330d7',
        borderColor: '#e330d7',
        borderWidth: 1,
        borderRadius: 8,
        marginBottom: 10,
        alignSelf: 'stretch',
        justifyContent: 'center'
    }
});