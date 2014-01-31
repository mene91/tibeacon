Usage:
------

var iBeacon = require('miga.tibeacon');
iBeacon.initBeacon({
    success : onSuccess
});

function onSuccess(e){
  Ti.API.info(JSON.stringify(e));
}

iBeacon.startScanning();
iBeacon.stopScanning();


