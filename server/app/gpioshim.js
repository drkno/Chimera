let log = (msg) => {
	console.log('GPIO-SHIM|' + msg);
};

log('Running immitation rather than actual GPIO server.');

exports.DIR_IN = "IN";
exports.DIR_OUT = "OUT";

exports.setup = (port, mode, callback) => {
	log(`Setup.`);
	if (callback) {
		callback();
	}
};

exports.read = (port, callback) => {
	log(`Reading from GPIO${port}, "${Math.floor(Math.random() * 2)}".`);
	callback(value);
};

exports.write = (port, output, callback) => {
	log(`Writing "${output}" to GPIO${port}.`);
	if (callback) {
		callback();
	}
};

exports.destroy = () => {
	log('Destroyed GPIO');
};
