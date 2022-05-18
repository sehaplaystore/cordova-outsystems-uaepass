const fs = require("fs");
const path = require("path");

const pluginId = "com-outsystems-uaepass";

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
    console.log("Adding App Group!")

    const configPath = path.join(context.opts.projectRoot,"plugins","ios.json");
    const configsString = fs.readFileSync(configPath,"utf-8");
    var configs = JSON.parse(configsString);
    configs = configs.installed_plugins[pluginId];

    const ConfigParser = require('cordova-common').ConfigParser;
    const config = new ConfigParser("config.xml");
    const appName = config.name();

    var pathSwift = path.join(
        context.opts.projectRoot,
        "platforms",
        "ios",
        appName,
        "Plugins",
        pluginId,
        "UAEPass.swift"
    );

    var content = fs.readFileSync(pathSwift,"utf8");
    if(typeof content === "string"){
        content = content.replace(/\$success/g,configs.SCHEMASUCCESS);
        content = content.replace(/\$failure/g,configs.SCHEMAFAIL);
    }else{
        console.error(pathSwift + "could not be retrieved!");
    }
    
    fs.writeFileSync(pathSwift,content);
    console.log("Changed "+path.basename(pathSwift)+"!");

    var pathPlist = path.join(
        context.opts.projectRoot,
        "platforms",
        "ios",
        appName,
        appName+"-Info.plist"
    );
    
    replaceFile(configs.IOSSCHEMA,/\$HelloCordova/g,pathPlist);

    console.log("Changed Schemas!")
};