function * genf0() {
  var x = yield 1;
  return x;
}

function * genf1() {
  //yield 1;
  try {
    //yield 2;
  } finally {
    yield 3;
  }
  yield 4;
}

var g= genf1();
print(g.next().value);
print(g.next().value);
print(g.next().value);
