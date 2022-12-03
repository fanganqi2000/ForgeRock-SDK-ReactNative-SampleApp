import React, { useState, useEffect } from 'react';
import { Text, View, BackHandler, Alert, LogBox } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { enableScreens } from 'react-native-screens';
import { Icon } from 'react-native-elements';
import Bookcase from './screens/bookcase.js';
import Explore from './screens/explore.js';
import AddBook from './screens/addBook.js';
import Lists from './screens/lists.js';
import Profile from './screens/profile.js';
import EditBook from './screens/editBook.js';
import Homesceen from './screens/Homescreen.js';
import Login from './screens/login.js';

LogBox.ignoreAllLogs();

const Tab = createBottomTabNavigator();
const Stack = createStackNavigator();
 
enableScreens();

function MyTabs() {
  return (
    <Tab.Navigator>
      <Tab.Screen
        name="Bookcase"
        component={Bookcase}
        navigation={Stack}
        options={{
          tabBarLabel: 'Bookcase',
          tabBarIcon: ({ color, size }) => (
            <Icon name="open-book" type="entypo" size={28} color={color} />
          ),
        }} />
      <Tab.Screen
        name="Explore"
        component={Explore}
        options={{
          tabBarLabel: 'Explore',
          tabBarIcon: ({ color, size }) => (
            <Icon name="ios-albums-outline" type="ionicon" size={28} color={color} />
          ),
        }} />
      <Tab.Screen
        name="AddBook"
        component={AddBook}
        options={{
          tabBarLabel: 'AddBook',
          tabBarIcon: ({ color, size }) => (
            <Icon name="ios-add-circle-outline" type="ionicon" size={28} color={color} />
          ),
        }} />
      <Tab.Screen
        name="Lists"
        component={Lists}
        options={{
          tabBarLabel: 'Lists',
          tabBarIcon: ({ color, size }) => (
            <Icon name="list" type="entypo" size={28} color={color} />
          ),
        }} />
      <Tab.Screen
        name="Profile"
        component={Profile}
        options={{
          tabBarLabel: 'Profile',
          tabBarIcon: ({ color, size }) => (
            <Icon name="ios-attach-outline" type="ionicon" size={28} color={color} />
          ),
        }} />
    </Tab.Navigator>
  );
}
 
export default function App({ navigation }) {  
  return (
 
    <NavigationContainer>
      <Stack.Navigator>
        <Stack.Screen name="Homescreen" component={Homesceen} />
        <Stack.Screen name="Login" component={Login} />
        <Stack.Screen name="MyTabs" component={MyTabs} />
        <Stack.Screen name="Bookcase" component={Bookcase} />
        <Stack.Screen name="EditBook" component={EditBook} />
      </Stack.Navigator>
    </NavigationContainer>
 
  );
}