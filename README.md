
# react-native-image-store

The goal: stop sending images over the React Native bridge! Instead, store them in the native-side image store and use [rn-fetch-blob](https://github.com/mvayngrib/rn-fetch-blob) to upload directly from native

iOS/Android normalization for https://facebook.github.io/react-native/docs/imagestore.html  

Use this on the JS/native side to store images in native cache

## Getting started

`$ npm install react-native-image-store --save`

### Mostly automatic installation

`$ react-native link react-native-image-store`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-image-store` and add `RNImageStore.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNImageStore.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import io.tradle.reactimagestore.RNImageStorePackage;` to the imports at the top of the file
  - Add `new RNImageStorePackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-image-store'
  	project(':react-native-image-store').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-image-store/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-image-store')
  	```

## Usage

iOS/Android normalization for https://facebook.github.io/react-native/docs/imagestore.html  
