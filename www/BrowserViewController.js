var exec = require("cordova/exec");
module.exports = {
  isAvailable: function (callback) {
    var errorHandler = function errorHandler(error) {
      // An error has occurred while trying to access the
      // BrowserViewController native implementation, most likely because
      // we are on an unsupported platform.
      callback(false);
    };
    exec(callback, errorHandler, "BrowserViewController", "isAvailable", []);
  },
  show: function (options, onSuccess, onError) {
    options = options || {};
    if (!options.hasOwnProperty('animated')) {
      options.animated = true;
    }
    exec(onSuccess, onError, "BrowserViewController", "show", [options]);
  },
  hide: function (onSuccess, onError) {
    exec(onSuccess, onError, "BrowserViewController", "hide", []);
  },
  getViewHandlerPackages: function (onSuccess, onError) {
    exec(onSuccess, onError, "BrowserViewController", "getViewHandlerPackages", []);
  },
  useCustomTabsImplementation: function (packageName, onSuccess, onError) {
    exec(onSuccess, onError, "BrowserViewController", "useCustomTabsImplementation", [packageName]);
  },
  connectToService: function (onSuccess, onError) {
    exec(onSuccess, onError, "BrowserViewController", "connectToService", []);
  },
  warmUp: function (onSuccess, onError) {
    exec(onSuccess, onError, "BrowserViewController", "warmUp", []);
  },
  mayLaunchUrl: function (url, onSuccess, onError) {
    exec(onSuccess, onError, "BrowserViewController", "mayLaunchUrl", [url]);
  }
};
