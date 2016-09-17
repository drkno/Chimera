'use strict';

let GPIO = require('./gpio.js'),
	EventEmitter = require('events'),
	_eventEmitter = new EventEmitter(),
	_doorTime = 16000,
	_doorCurrent = 0,
	_toggleTimeout = null,
	_toggleState = false,
	_toggleEnd = () => {
		_toggleTimeout = null;
		_doorCurrent = (_doorCurrent + 1) % 4;
		_toggleState = false;
		_eventEmitter.emit('change', DoorControl.getState());
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
			if (_doorCurrent !== 0) {
				throw new Error('The door is already open.');
			}
			_doorCurrent = 1;
			_eventEmitter.emit('change', DoorControl.getState());
			_toggleStart();
		},
		close: () => {
			if (_doorCurrent !== 2) {
				throw new Error('The door is already closed.');
			}
			_doorCurrent = 3;
			_eventEmitter.emit('change', DoorControl.getState());
			_toggleStart();
		},
		getState: () => {
			switch(_doorCurrent) {
				case 0: return 'Closed';
				case 1: return 'Opening';
				case 2: return 'Open';
				case 3: return 'Closing';
				default: throw new Error('Invalid internal state of door position.');
			}
		},
		on: (event, callback) => {
			 _eventEmitter.on(event, callback);
		}
	},
	_exitHandler = () => {
		DoorControl.stop();
		process.exit();
	};

process.on('exit', _exitHandler);
process.on('SIGINT', _exitHandler);
process.on('uncaughtException', _exitHandler);

DoorControl.setup();

module.exports = DoorControl;
