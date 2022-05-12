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

exports.login = function (success, error) {
    exec(success, error, 'uaepass', 'login', []);
};

exports.getProfile = function (success, error) {
    exec(success, error, 'uaepass', 'getProfile', []);
};

exports.clearData = function (success, error) {
    exec(success, error, 'uaepass', 'clearData', []);
};