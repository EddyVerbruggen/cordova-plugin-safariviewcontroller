exports.show =  function(url, toolbarColor, showDefaultShareMenuItem, transition) {
    window.location = url.url;
};

exports.isAvailable = function(callback) {
    return callback(true);
};