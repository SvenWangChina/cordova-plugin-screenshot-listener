module.exports = {
    start: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, 'ScreenshotListener', 'start', []);
    },
};
