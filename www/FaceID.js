var exec = require('cordova/exec');

var PLUGIN_NAME = 'FaceID';

function FaceID() { }


FaceID.prototype.enrolar = function (successCallback, errorCallback, key) {

      if (errorCallback == null) {
            errorCallback = function () {
            };
      }

      if (typeof errorCallback != "function") {
            console.log("El parametro de Error debe ser una función");
            return;
      }

      if (typeof successCallback != "function") {
            console.log("El parametro de Exito debe ser una función");
            return;
      }

      exec(successCallback, errorCallback, PLUGIN_NAME, 'enrolamiento', [{"cedula": key}]);
}

FaceID.prototype.verificar = function (successCallback, errorCallback, key) {

      if (errorCallback == null) {
            errorCallback = function () {
            };
      }

      if (typeof errorCallback != "function") {
            console.log("El parametro de Error debe ser una función");
            return;
      }

      if (typeof successCallback != "function") {
            console.log("El parametro de Exito debe ser una función");
            return;
      }

      exec(successCallback, errorCallback, PLUGIN_NAME, 'verificacion', [{"cedula": key}]);
}

var FaceID = new FaceID();
module.exports = FaceID;
