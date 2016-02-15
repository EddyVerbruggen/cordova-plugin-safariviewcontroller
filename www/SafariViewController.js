var exec = require("cordova/exec");
module.exports = {
  isAvailable: function (callback) {
    var errorHandler = function errorHandler(error) {
      // An error has occurred while trying to access the
      // SafariViewController native implementation, most likely because
      // we are on an unsupported platform.
      callback(false);
    };
    exec(callback, errorHandler, "SafariViewController", "isAvailable", []);
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
