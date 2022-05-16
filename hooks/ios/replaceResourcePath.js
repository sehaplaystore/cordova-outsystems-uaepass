const fs = require("fs");
const path = require("path");

const pluginId = "cordova-outsystems-uaepass";

function replaceFile(toreplace,regex,filepath){
    var content = fs.readFileSync(filepath,"utf8");
    if(!content.includes(toreplace)){
        if(typeof content === "string"){
            content = content.replace(regex,toreplace);
        }else{
            console.error(filepath + "could not be retrieved!");
        }
    }
    
    fs.writeFileSync(filepath,content);
    console.log("Changed "+path.basename(filepath)+"!");
}

module.exports = function(context) {
    console.log("Changing Copy Resources Script!")

    const ConfigParser = require('cordova-common').ConfigParser;
    const config = new ConfigParser("config.xml");
    const appName = config.name();
    
    var pathPodResources = path.join(
        context.opts.projectRoot,
        "platforms",
        "ios",
        "Pods",
        "Target Support Files",
        "Pods-"+appName,
        "Pods-"+appName+"-resources.sh"
    );

    replaceFile("${PODS_ROOT}/../www/UAEPassClient/UAEPassClient/ViewControllers/UAEPassWebViewController.xib",/\${BUILT_PRODUCTS_DIR.*UAEPassWebViewController\.nib/g,pathPodResources);

    console.log("Fixed Copy Resources Script!")
};