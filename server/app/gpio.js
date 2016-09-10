'use strict';

let pi_gpio = require('rpi-gpio'),
	GPIO = {
		_openPorts: [],
		HIGH: true,
		LOW: false,
		IN: pi_gpio.DIR_IN,
		OUT: pi_gpio.DIR_OUT,
		GPIO03: 3, GPIO05: 5, GPIO07: 6,
		GPIO08: 8, GPIO10: 10, GPIO11: 11,
		GPIO12: 12, GPIO13: 13, GPIO15: 15,
		GPIO16: 16, GPIO18: 18, GPIO19: 19,
		GPIO21: 21, GPIO22: 22, GPIO23: 23,
		GPIO24: 24, GPIO26: 26,
		setup: (port, mode, callback) => {
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
		},
		input: (port, callback) => {
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
		},
		output: (port, output, callback) => {
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
		},
		cleanup: () => {
			pi_gpio.destroy();
			this._openPorts = [];
		}
	};

module.exports = GPIO;
