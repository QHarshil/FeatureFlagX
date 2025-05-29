/** @type {import('ts-jest').JestConfigWithTsJest} **/
module.exports = {
  testEnvironment: "node",
  transform: {
    "^.+\\.tsx?$": ["ts-jest", {}],
  },
  testRegex: "(/__tests__/.*|(\\.|/)(test|spec))\\.(tsx?|jsx?)$",
  testPathIgnorePatterns: ["/node_modules/", "/dist/", "\\.js$"],
  moduleFileExtensions: ["ts", "tsx", "js", "jsx", "json", "node"],
};