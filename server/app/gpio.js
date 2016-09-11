'use strict';

let pi_gpio = require('rpi-gpio');

class GPIO {
	constructor() {
		this._openPorts = [];
		this.HIGH = true;
		this.LOW = false;
		this.IN = pi_gpio.DIR_IN;
		this.OUT = pi_gpio.DIR_OUT;
		this.GPIO03 = 3;
		this.GPIO05 = 5;
		this.GPIO07 = 6;
		this.GPIO08 = 8;
		this.GPIO10 = 10;
		this.GPIO11 = 11;
		this.GPIO12 = 12;
		this.GPIO13 = 13;
		this.GPIO15 = 15;
		this.GPIO16 = 16;
		this.GPIO18 = 18;
		this.GPIO19 = 19;
		this.GPIO21 = 21;
		this.GPIO22 = 22;
		this.GPIO23 = 23;
		this.GPIO24 = 24;
		this.GPIO26 = 26;
	}

	setup (port, mode, callback) {
		if (!this['GPIO' + port]) {
			throw new Error('Invalid GPIO port.');
		}
		pi_gpio.setup(port, mode, (err) => {
			if (err) {
				throw new Error(err);
			}
			let filtered = this._openPorts.filter((v) => v.port === port),
				result = filtered[0];
			if (result) {
				result.mode = mode;
			}
			else {
				result = {
					port: port,
					mode: mode
				};
				this._openPorts.push(result);
			}
			if (callback) {
				callback(result);
			}
		});
	}

	input (port, callback) {
		let filtered = this._openPorts.filter((v) => v.port === port && v.mode === this.IN);
		if (filtered.length !== 1) {
			throw new Error('A GPIO port must be setup before it can be output to.');
		}
		pi_gpio.read(port, (err, value) => {
			if (err) {
				throw new Error(err);
			}
			callback(value);
		});
	}

	output (port, output, callback) {
		let filtered = this._openPorts.filter((v) => v.port === port && v.mode === this.OUT);
		if (filtered.length !== 1) {
			throw new Error('A GPIO port must be setup before it can be output to.');
		}
		pi_gpio.write(port, output, (err) => {
			if (err) {
				throw new Error(err);
			}
			if (callback) {
				callback();
			}
		});
	}

	cleanup () {
		pi_gpio.destroy();
		this._openPorts = [];
	}
};

module.exports = new GPIO();
