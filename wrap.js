
const unwrapOpt = (obj, method, opt) => val => {
  val = typeof val === 'object' ? val[opt] : val
  return obj[method](val)
}

const methodToOpt = {
  hasImageForTag: 'imageTag',
  getBase64ForTag: 'imageTag',
  removeImageForTag: 'imageTag',
  addImageFromBase64: 'base64',
}

const normalizeProp = {
  addImageFromBase64: base64 => base64.replace(/^data:.*?base64,/, ''),
}

const createTransformer = transform => obj => {
  const wrapped = {}
  Object.keys(obj).map(method => {
    const original = obj[method].bind(obj)
    wrapped[method] = transform(obj, method, original) || original
  })

  return wrapped
}

export const withUnwrappedOpts = createTransformer((obj, method, value) => {
  if (method in methodToOpt) {
    return unwrapOpt(obj, method, methodToOpt[method])
  }
})

export const withNormalizedProp = createTransformer((obj, method, fn) => {
  if (method in normalizeProp) {
    return prop => fn(normalizeProp[method](prop))
  }
})

export const wrapImageStore = imageStore => withUnwrappedOpts(withNormalizedProp(imageStore))
