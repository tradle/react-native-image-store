
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

const normalizeOpts = {
  addImageFromBase64: ({ base64, ...rest }) => ({
    base64: base64.replace(/^data:.*?base64,/, ''),
    ...rest,
  }),
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

export const withNormalizedOpts = createTransformer((obj, method, fn) => {
  if (method in normalizeOpts) {
    return opts => fn(normalizeOpts[method](opts))
  }
})

export const wrapIOSImageStore = imageStore => withNormalizedOpts(withUnwrappedOpts(imageStore))
export const wrapAndroidImageStore = imageStore => withNormalizedOpts(imageStore)
