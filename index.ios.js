
import { ImageStore } from 'react-native'

const promisify = fn => (...args) => new Promise((resolve, reject) => fn(...args, resolve, reject))

const promisified = Object
  .keys(RNImageStore)
  .reduce((original, key) => promisify(original[key].bind(original)))

export default promisified
