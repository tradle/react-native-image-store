
import { NativeModules } from 'react-native'

const { RNImageStore } = NativeModules
const wrapper = { ...RNImageStore }
// support call with single-option: obj[method](optionValue)
// or options obj:                  obj[method](options)
const supportPropOrOptions = (obj, method, propName) => options => typeof options === 'string'
  ? obj[method]({ [propName]: options })
  : obj[method](options)

wrapper.addImageFromBase64 = supportPropOrOptions(RNImageStore, 'addImageFromBase64', 'base64')
wrapper.addImageFromPath = supportPropOrOptions(RNImageStore, 'addImageFromPath', 'path')

export default wrapper
