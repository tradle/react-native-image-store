
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

export const withUnwrappedOpts = obj => {
  const wrapped = {}
  Object.keys(methodToOpt).map(method => {
    wrapped[method] = unwrapOpt(obj, method, methodToOpt[method])
  })

  return wrapped
}
