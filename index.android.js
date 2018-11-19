
import { NativeModules } from 'react-native'
import { wrapAndroidImageStore } from './wrap'

const { RNImageStore } = NativeModules

export default wrapAndroidImageStore(RNImageStore)
