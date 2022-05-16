var exec = require('cordova/exec');

exports.init = function (success, error,environment,clientID,clientSecret,redirectUrl) {
    exec(success, error, 'uaepass', 'initPlugin', [environment,clientID,clientSecret,redirectUrl]);
};

exports.getWritePermission = function (success, error) {
    exec(success, error, 'uaepass', 'getWritePermission', []);
};

exports.getCode = function (success, error) {
    exec(success, error, 'uaepass', 'getCode', []);
};

exports.getAccessToken = function (success, error,code) {
    exec(success, error, 'uaepass', 'getAccessToken', [code]);
};

exports.getProfile = function (success, error,accessToken) {
    exec(success, error, 'uaepass', 'getProfile', [accessToken]);
};

exports.signDocument = function (success, error,documentURL) {
    exec(success, error, 'uaepass', 'signDocument', [documentURL]);
};

exports.clearData = function (success, error) {
    exec(success, error, 'uaepass', 'clearData', []);
};