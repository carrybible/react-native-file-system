
# react-native-file-system

## Getting started

`$ npm install react-native-file-system --save`

### Mostly automatic installation

`$ react-native link react-native-file-system`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-file-system` and add `RNFileSystem.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNFileSystem.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNFileSystemPackage;` to the imports at the top of the file
  - Add `new RNFileSystemPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-file-system'
  	project(':react-native-file-system').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-file-system/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-file-system')
  	```

## Usage
```javascript
import RNFileSystem from 'react-native-file-system';

// TODO: What to do with the module?
RNFileSystem;
```
