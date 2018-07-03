const index = require('../index.js');
const assert = require('assert');
const _ = require('lodash');

describe('exists', () => {
  it('should return expected results', () => {
    assert.deepEqual(index.exists({ one: 1, two: 2 }, "one"), true);
    assert.deepEqual(index.exists({ one: 1, two: 2 }, "three"), false);
    assert.deepEqual(index.exists({ one: 1, two: 2, three: undefined }, "three"), false);
  });
});

describe('existsArr', () => {
  it('should return expected results', () => {
    assert.deepEqual(index.existsArr({ one: 1, two: 2 }, ["one", "two"] ), [ "one", "two"]);
    assert.deepEqual(index.existsArr({ one: 1, two: undefined }, ["one", "two"] ), [ "one" ]);
    assert.deepEqual(index.existsArr({ one: 1, two: undefined }, ["one", "two", "three"] ), [ "one" ]);
  });
});

describe('cleanup', () => {
  it('should return expected result', () => {
    const testVal = [
      { "x-forwarded-for": "8.8.8.8", remoteAddress: "8.8.8.8", date: { $date: 0 } },
      { remoteAddress: "8.8.8.8", date: { $date: 0 } },
      { remoteAddress: "thisismyipaddress", date: { $date: 0 } }, // invalid
      { remoteAddress: "192.168.1.1", date: { $date: 0 } }, // invalid
      { "x-forwarded-for": "8.8.8.8", date: { $date: 0 } },
      { one: 1, two: 2, three: 3 } // invalid
    ];
    const expected = [
      { "x-forwarded-for": "8.8.8.8", remoteAddress: "8.8.8.8", ip: "8.8.8.8", date: new Date(0) },
      { remoteAddress: "8.8.8.8", ip: "8.8.8.8", date: new Date(0) },
      { "x-forwarded-for": "8.8.8.8", ip: "8.8.8.8", date: new Date(0) }
    ];
    const res = index.cleanup(testVal);
    assert.deepEqual(res, expected);
  });
});

describe('cleanIp', () => {
  it('should return expected output', () => {
    const testInput = [
      "8.8.8.8,4.4.4.4",
      "::ffff:193.151.80.66"
    ];
    const expected = [
      "8.8.8.8",
      "193.151.80.66"
    ];
    _.zip(testInput, expected).map(it =>
      assert.equal(index.cleanIp(it[0]), it[1])
      );
  });
});


