const fs = require("fs");
const path = require("path");

const pluginId = "com-outsystems-uaepass";

module.exports = function(context) {
    console.log("Adding App Group!")

    const configPath = path.join(context.opts.projectRoot,"plugins","fetch.json"); 
    const configsString = fs.readFileSync(configPath,"utf-8");
    var configs = JSON.parse(configsString);
    console.log(configs)
    configs = configs[pluginId].variables;

    var pathGradle = path.join(
        context.opts.projectRoot,
        "plugins",
        pluginId,
        "src",
        "android",
        "dependencies.gradle"
        );

    console.log(configs)
    console.log(configs.SCHEMASUCCESS)
    console.log(configs.SCHEMAFAIL)
    console.log(configs.SCHEMA)

    var content = fs.readFileSync(pathGradle,"utf8");
    if(typeof content === "string"){
        content = content.replace(/\$success/g,configs.SCHEMASUCCESS);
        content = content.replace(/\$failure/g,configs.SCHEMAFAIL);
        content = content.replace(/\$HelloCordova/g,configs.SCHEMA);
    }else{
        console.error(pathGradle + "could not be retrieved!");
    }
    
    fs.writeFileSync(pathGradle,content);
    console.log("Changed "+path.basename(pathGradle)+"!");

    console.log("Changed Schemas Group!")
};