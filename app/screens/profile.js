import React, { useContext, Component } from 'react';
import {
  StyleSheet,
  Text,
  View,
  NativeModules,
  TouchableHighlight,
  ScrollView
} from 'react-native';

const { ForgeRockModule } = NativeModules;

export default function Profile(props) {
  return (
    <View style={styles.container}>
      <ScrollView>
        <Text style={styles.title}>
          {global.accessToken}
        </Text>
      </ScrollView>
      <TouchableHighlight style={styles.button} onPress={() => {
        ForgeRockModule.performUserLogout();
        props.navigation.navigate('Homescreen');
      }} underlayColor='#99d9f4'>
        <Text style={styles.buttonText}>Logout</Text>
      </TouchableHighlight>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  title: {
    fontSize: 10,
    textAlign: 'center',
    margin: 12,
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
  buttonText: {
    fontSize: 18,
    color: 'white',
    alignSelf: 'center'
  }
});