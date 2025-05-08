var exec = require('cordova/exec');

var ScreenshotListener = {
    start: function (success, error) {
        exec(success, error, 'ScreenshotListener', 'start', []);
    },
};

module.exports = ScreenshotListener;
