const index = require('../index.js');
const assert = require('assert');

describe('cleanup', () => {
  it('should return expected result', () => {
     const testVal = [
      { "x-forwarded-for": "8.8.8.8", remoteAddress: "8.8.8.8" },
      { remoteAddress: "8.8.8.8" },
      { remoteAddress: "thisismyipaddress" }, // invalid
      { remoteAddress: "192.168.1.1" }, // invalid
      { "xfowarded": "8.8.8.8", "remotestuff": "8.8.8.8" }, // invalid
      { "x-forwarded-for": "8.8.8.8" }
    ];

    const expected = [
      { "x-forwarded-for": "8.8.8.8", remoteAddress: "8.8.8.8" },
      { remoteAddress: "8.8.8.8" },
      { "x-forwarded-for": "8.8.8.8" }
    ];

    assert.deepEqual(expected, index.cleanup(testVal));

  });
});
