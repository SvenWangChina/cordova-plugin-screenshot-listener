var exec = require('cordova/exec');

var ScreenshotDetector = {
    startWatch: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "ScreenshotDetector", "startWatch", []);
    },
    stopWatch: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "ScreenshotDetector", "stopWatch", []);
    }
};

module.exports = ScreenshotDetector;