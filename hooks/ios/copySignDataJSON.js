
var path = require("path");
var fs = require("fs");

function copyFromSourceToDestPath(defer, sourcePath, destPath) {
    fs.createReadStream(sourcePath).pipe(fs.createWriteStream(destPath))
    .on("close", function (err) {
      defer.resolve();
    })
    .on("error", function (err) {
      console.log(err);
      defer.reject();
    });
  }

  module.exports = function(context) {
    var defer = require("q").defer();

    var pathSignData = path.join(
        context.opts.projectRoot,
        "platforms",
        "ios",
        "signData.json"
    )

    var pathAssetSignData = path.join(
        context.opts.plugin.dir,
        "signData.json"
    )
    if(!fs.existsSync(pathSignData)){

        defer.resolve();
        return defer.promise;
    }

    copyFromSourceToDestPath(defer,pathAssetSignData,pathSignData)
    defer.resolve()

    return defer.promise;
}