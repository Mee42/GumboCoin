if (typeof kotlin === 'undefined') {
  throw new Error("Error loading module 'website'. Its dependency 'kotlin' was not found. Please, check whether 'kotlin' is loaded prior to 'website'.");
}
var website = function (_, Kotlin) {
  'use strict';
  var Kind_CLASS = Kotlin.Kind.CLASS;
  var throwCCE = Kotlin.throwCCE;
  var toString = Kotlin.toString;
  var println = Kotlin.kotlin.io.println_s8jyv4$;
  var Unit = Kotlin.kotlin.Unit;
  function RSocketClient() {
  }
  RSocketClient.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'RSocketClient',
    interfaces: []
  };
  function el(id) {
    var tmp$;
    return Kotlin.isType(tmp$ = document.getElementById(id), Element) ? tmp$ : throwCCE();
  }
  function main$lambda(closure$i) {
    return function (it) {
      var tmp$;
      el('text_1').textContent = '' + toString((tmp$ = closure$i.v, closure$i.v = tmp$ + 1 | 0, tmp$));
      println('Hello');
      return Unit;
    };
  }
  function main() {
    var i = {v: 0};
    el('button_1').onclick = main$lambda(i);
  }
  _.RSocketClient = RSocketClient;
  _.el_evuobd$ = el;
  _.main = main;
  main();
  Kotlin.defineModule('website', _);
  return _;
}(typeof website === 'undefined' ? {} : website, kotlin);
