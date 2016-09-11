'use strict';

let GPIO = require('./gpio.js'),
	_doorTime = 10000,
	_doorCurrent = false,
	_toggleTimeout = null,
	_toggleState = false,
	_toggleEnd = () => {
		_toggleTimeout = null;
		_doorCurrent = !_doorCurrent;
		_toggleState = false;
	},
	_toggleStart = () => {
		if (_toggleState) {
			throw new Error('Cannot perform operation at this time.');
		}
		_toggleState = true;
		GPIO.output(GPIO.GPIO21, GPIO.LOW, () => {
			setTimeout(() => {
				GPIO.output(GPIO.GPIO21, GPIO.HIGH, () => {
					_toggleTimeout = setTimeout(_toggleEnd, _doorTime);
				});
			}, 500);
		});
	},
	DoorControl = {
	    setup: () => {
	        GPIO.setup(GPIO.GPIO21, GPIO.OUT, () => {
	            GPIO.output(GPIO.GPIO21, GPIO.HIGH);
	        });
	    },
	    stop: () => {
			if (_toggleTimeout) {
				clearTimeout(_toggleTimeout);
			}
	        GPIO.cleanup();
	    },
		open: () => {
			if (_doorCurrent) {
				throw new Error('The door is already open.');
			}
			_toggleStart();
		},
		close: () => {
			if (!_doorCurrent) {
				throw new Error('The door is already closed.');
			}
			_toggleStart();
		},
		getState: () => {
			return _doorCurrent ? 'Open' : 'Closed';
		}
	},
	_exitHandler = () => {
		DoorControl.stop();
		process.exit();
	};

process.on('exit', _exitHandler);
process.on('SIGINT', _exitHandler);
//process.on('uncaughtException', _exitHandler);

DoorControl.setup();

module.exports = DoorControl;
