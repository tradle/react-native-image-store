
import { ImageStore } from 'react-native'
import { wrapIOSImageStore } from './wrap'

const promisifyFromTwoCallbacks = fn => (...args) => new Promise((resolve, reject) => fn(...args, resolve, reject))
const wrapper = wrapIOSImageStore({
  hasImageForTag: imageTag => new Promise(resolve => {
    // normalize boolean
    return ImageStore.hasImageForTag(imageTag, result => resolve(!!result))
  }),
  getBase64ForTag: promisifyFromTwoCallbacks(ImageStore.getBase64ForTag.bind(ImageStore)),
  addImageFromBase64: promisifyFromTwoCallbacks(ImageStore.addImageFromBase64.bind(ImageStore)),
  removeImageForTag: async uri => ImageStore.removeImageForTag(uri),
})

export default wrapper
