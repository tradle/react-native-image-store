
import { NativeModules } from 'react-native'
import { wrapImageStore } from './wrap'

const { RNImageStore } = NativeModules

export default wrapImageStore(RNImageStore)
