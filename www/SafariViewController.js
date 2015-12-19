var exec = require("cordova/exec");
module.exports = {
  isAvailable: function (callback) {
    exec(callback, null, "SafariViewController", "isAvailable", []);
  },
  show: function (options, onSuccess, onError) {
    var opts = options || {};
    if (!opts.hasOwnProperty('animated')) {
      opts.animated = true;
    }
    exec(onSuccess, onError, "SafariViewController", "show", [options]);
  },
  hide: function (onSuccess, onError) {
    exec(onSuccess, onError, "SafariViewController", "hide", []);
  }
};