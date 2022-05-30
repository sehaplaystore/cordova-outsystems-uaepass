
var fs = require('fs');
var path = require('path');

function replacerLaunchMode(match, p1, p2, p3, offset, string){
    var newLaunchMode = "launchMode=\"singleInstance\"";
    return [p1,newLaunchMode,p3].join("");
}

module.exports = function (context) {

    console.log("--------------------------------------Start changing Manifest!-----------------------------------------");
    var deferral = require('q').defer();

    var projectRoot = context.opts.cordova.project ? context.opts.cordova.project.root : context.opts.projectRoot;
    var manifestPath = path.join(projectRoot,"platforms","android","app","src","main","AndroidManifest.xml");
    var manifest = fs.readFileSync(manifestPath, "utf8");

    var regexLaunchMode = /(<\?xml[.|\s|\S]*<activity[.|\s|\S]*)(launchMode="singleTop")([\s|\S]*MainActivity[\s|\S]*manifest>)/gm;
    manifest = manifest.replace(regexLaunchMode,replacerLaunchMode);
    
    fs.writeFileSync(manifestPath, manifest);
    console.log("---------------------------------------Finished changing Manifest!------------------------------------");
    deferral.resolve();

    return deferral.promise;
}