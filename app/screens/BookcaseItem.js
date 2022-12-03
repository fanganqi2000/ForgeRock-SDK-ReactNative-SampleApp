import React, { Component } from 'react';
import {
  StyleSheet,
  TouchableOpacity,
  Text,
  Image,
  View
} from 'react-native';

export default function BookcaseItem(props) {
  const onEditBook = () => {
    let id = props.id;
    props.navigation.navigate('EditBook', { id: id })
  }
  return (
    <TouchableOpacity onPress={onEditBook}>
      <View style={styles.rowContainer} >
        <Image source={{ uri: props.thumbnail }}
          style={styles.thumbnail}
          resizeMode="contain" />
        <View style={styles.rowText}>
          <Text style={styles.title} numberOfLines={2} ellipsizeMode={'tail'}>
            {props.title}
          </Text>
          <Text style={styles.author} numberOfLines={1} ellipsizeMode={'tail'}>
            {props.author}
          </Text>
        </View>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  rowContainer: {
    flexDirection: 'row',
    backgroundColor: '#FFF',
    height: 100,
    padding: 10,
    marginRight: 10,
    marginLeft: 10,
    marginTop: 10,
    borderRadius: 4,
    shadowOffset: { width: 1, height: 1, },
    shadowColor: '#CCC',
    shadowOpacity: 1.0,
    shadowRadius: 1
  },
  title: {
    paddingLeft: 10,
    paddingTop: 5,
    fontSize: 16,
    fontWeight: 'bold',
    color: '#777'
  },
  author: {
    paddingLeft: 10,
    marginTop: 5,
    fontSize: 14,
    color: '#777'
  },
  thumbnail: {
    flex: 1,
    height: undefined,
    width: undefined
  },
  rowText: {
    flex: 4,
    flexDirection: 'column'
  }
});