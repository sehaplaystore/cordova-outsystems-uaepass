const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

const podfilePath = path.join('platforms', 'ios', 'Podfile');
const podDeclaration = "\npod 'UAEPassClient', :path => 'www/UAEPassClient/UAEPassClient.podspec'\n";

fs.readFile(podfilePath, 'utf8', function (err, data) {
    if (err) {
        return console.log(`Error reading Podfile: ${err.message}`);
    }

    const lastIndex = data.lastIndexOf('end');
    if (lastIndex === -1) {
        throw new Error("🚨 Could not find the position to insert the pod declaration.");
    }

    const newData = [data.slice(0, lastIndex), podDeclaration, data.slice(lastIndex)].join('');

    fs.writeFile(podfilePath, newData, 'utf8', function (err) {
        if (err) throw new Error(`Error writing Podfile: ${err.message}`);

        console.log("✅ Podfile updated successfully. Running 'pod install'...");

        exec('cd platforms/ios && pod install', (err, stdout, stderr) => {
            if (err) {
                return console.log(`🚨 Error executing 'pod install': ${err.message}`);
            }
            console.log("stdout: " + stdout);
            console.log("✅ Pod installation completed.");
        });
    });
});
