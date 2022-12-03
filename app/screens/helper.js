import { NativeModules } from 'react-native';
const { ForgeRockModule } = NativeModules;
import { StackActions } from '@react-navigation/native';

export async function proceedWithAuthenticated(props, responseArray, callbacks) {
    try {
        global.accessToken = responseArray.accessToken;
        const userInfoResponse = await ForgeRockModule.getUserInfo();
        global.userInfo = userInfoResponse;
        console.log(userInfoResponse);
        const deviceInfoResponse = await ForgeRockModule.getDeviceInformation();
        console.log(deviceInfoResponse);
        global.DeviceInfo = JSON.stringify(deviceInfoResponse);
        if (responseArray.error == "User is already authenticated" || responseArray.agentType == "browser") {
            if (global.accessToken === undefined) {
                const accessTokenResponse = await ForgeRockModule.getAccessToken();
                console.log(accessTokenResponse);
                global.accessToken = accessTokenResponse.accessToken;
                console.log("xxxxxxxxxxxxxxxxxxxxxx");
                console.log("Access Token: " + global.accessToken);
                console.log("xxxxxxxxxxxxxxxxxxxxxx");
            }
            props.navigation.navigate('MyTabs', callbacks);
        } else {
            props.navigation.dispatch(StackActions.replace('MyTabs', {params: {}}));
        }

    } catch (e) {
        //catch errors
        console.log("xxxxxxxxxxxxxxxxxxxxxx");
        console.log(e);
        console.log("xxxxxxxxxxxxxxxxxxxxxx");
    }
}