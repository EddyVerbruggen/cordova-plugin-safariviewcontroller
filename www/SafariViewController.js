var exec = require("cordova/exec");
var channel = require("cordova/channel");

var channels = {
  exit: channel.create("exit")
};

function eventHandler(event) {
  if (event && (event.type in channels)) {
    channels[event.type].fire(event);
  }
}

module.exports = {
  isAvailable: function (onSuccess, onError) {
    exec(onSuccess, onError, "SafariViewController", "isAvailable", []);
  },
  show: function (options, onSuccess, onError) {
    // This callback remains open for event handling; call onSuccess only once
    // then await events.
    exec(function(event) {
      if (onSuccess) {
        onSuccess.apply(null, arguments);
        onSuccess = null;
      }
      eventHandler(event);
    }, onError, "SafariViewController", "show", [options]);
  },
  hide: function (onSuccess, onError) {
    exec(onSuccess, onError, "SafariViewController", "hide", []);
  },
  addEventListener: function (eventname,f) {
    if (eventname in channels) {
      channels[eventname].subscribe(f);
    }
  },
  removeEventListener: function(eventname, f) {
    if (eventname in channels) {
      channels[eventname].unsubscribe(f);
    }
  }
};