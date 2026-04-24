module.exports = {
  root: true,
  env: {
    browser: true,
    es2021: true,
    node: true
  },
  extends: ['eslint:recommended'],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module'
  },
  ignorePatterns: ['dist/', 'dist-portable/', 'node_modules/'],
  overrides: [
    {
      files: ['scripts/*.js', 'scripts/*.mjs'],
      env: {
        node: true
      },
      rules: {
        'no-console': 'off'
      }
    }
  ],
  rules: {
    'no-console': 'warn'
  }
}
