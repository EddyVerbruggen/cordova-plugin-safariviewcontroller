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
    options = options || {};
    if (!options.hasOwnProperty('animated')) {
      options.animated = true;
    }
    exec(onSuccess, onError, "SafariViewController", "show", [options]);
  },
  hide: function (onSuccess, onError) {
    exec(onSuccess, onError, "SafariViewController", "hide", []);
  },
  connectToService: function (onSuccess, onError) {
    exec(onSuccess, onError, "SafariViewController", "connectToService", []);
  },
  warmUp: function (onSuccess, onError) {
    exec(onSuccess, onError, "SafariViewController", "warmUp", []);
  },
  mayLaunchUrl: function (url, onSuccess, onError) {
    exec(onSuccess, onError, "SafariViewController", "mayLaunchUrl", [url]);
  }
};
