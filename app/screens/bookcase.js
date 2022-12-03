import React, { Component, useEffect } from 'react';
import {
  SafeAreaView,
  StatusBar,
  StyleSheet,
  FlatList,
  View,
  BackHandler,
  Alert,
  LogBox
} from 'react-native';


LogBox.ignoreAllLogs();

import BookcaseItem from './BookcaseItem.js';
export default function Boookcase(props) {
  
  useEffect(() => {
    const backAction = () => {
      Alert.alert("Hold on!", "Are you sure you want to go back?", [
        {
          text: "Cancel",
          onPress: () => null,
          style: "cancel"
        },
        { text: "YES", onPress: () => props.navigation.navigate('Homescreen') }
      ]);
      return true;
    };

    const backHandler = BackHandler.addEventListener(
      "hardwareBackPress",
      backAction
    );

    return () => backHandler.remove();
  }, []);

  const books = [
    {
      id: 1,
      title: 'Harry Potter and the Goblet of Fire',
      author: 'J. K. Rowling',
      thumbnail: 'https://covers.openlibrary.org/w/id/7984916-M.jpg'
    },
    {
      id: 2,
      title: 'The Hobbit',
      author: 'J. R. R. Tolkien',
      thumbnail: 'https://covers.openlibrary.org/w/id/6979861-M.jpg'
    },
    {
      id: 3,
      title: '1984',
      author: 'George Orwell',
      thumbnail: 'https://covers.openlibrary.org/w/id/7222246-M.jpg'
    }
  ];

  renderItem = ({ item }) => (
    <BookcaseItem
      id={item.id}
      title={item.title}
      author={item.author}
      thumbnail={item.thumbnail}
      navigation={props.navigation}
    />
  );

  keyExtractor = (item, index) => item.id.toString();

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.container}>
        <StatusBar
          barStyle="light-content"
        />
        <FlatList
          data={books}
          keyExtractor={keyExtractor}
          renderItem={renderItem}
        />
      </View>

    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5FCFF',
  }
});