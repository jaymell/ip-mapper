timer = function(label) {
  return function() {
    var begin;
    var end; 

    return {
      begin: function() {
        if (begin === undefined) {
          begin = new Date();
          console.log(label + " begin: ", begin);
          return begin;
        }
      },
      end: function(n) {
        if (end === undefined) {
          end = new Date();
          console.log(label + " end: ", end);
          console.log(label + " elapsed (ms): ", end-begin)
          if (n) {
            console.log(label + " elapsed per-unit (ms): ", (end-begin)/n);
          }
          return end;
        }
      }
    }
  }();
};
