const fs = require('fs'),
	path = require('path'),
	EventEmitter = require('events');

class VideoWatch extends EventEmitter {
	constructor(directory) {
		super();
		this.directory = directory;
		process.on('exit', this.deconstructor);
		process.on('SIGINT', this.deconstructor);
		process.on('uncaughtException', this.deconstructor);
		let self = this,
			sequence = true;
		this.watch = fs.watch(directory, (eventType, filename) => {
			sequence = !sequence;
			if (sequence) {
				self.__reload();
			}
		});
		this.__reload();
	}

	__reload() {
		this.files = fs.readdirSync(this.directory);
		this.emit('change', this.files);
	}

	getFiles() {
		return this.files;
	}

	delete(file) {
		fs.unlinkSync(path.join(this.directory, file));
		this.__reload();
	}

	deconstructor() {
		this.watch.close();
	}
}

module.exports = VideoWatch;
