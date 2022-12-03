import React, { Component } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView
} from 'react-native';

export default function Explore(props) {
  return (
    <ScrollView>
      <View>
        <Text style={styles.title}>
          Email: {global.userInfo.email}
        </Text>
        <Text style={styles.title}>
          First Name: {global.userInfo.given_name}
        </Text>
        <Text style={styles.title}>
          Last Name: {global.userInfo.family_name}
        </Text>
        <Text style={styles.title}>
          Device: {global.DeviceInfo}
        </Text>
      </View>
    </ScrollView>
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
    fontSize: 18,
    textAlign: 'left',
    margin: 8,
  }
});