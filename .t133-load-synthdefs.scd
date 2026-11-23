(
var sourcePath = "myband.sc";
var source = File.readAllString(sourcePath);
var loadMarker = source.find("\n~myband.load;\n~myband.test;");

if (loadMarker.isNil) {
    Error("No se encontro el punto de carga en myband.sc").throw;
};

source.copyRange(0, loadMarker).interpret;
~myband[\defs].value;
Server.default.sync;
"myband: classicPiano, stringBass, organ70s loaded".postln;
0.exit;
)
