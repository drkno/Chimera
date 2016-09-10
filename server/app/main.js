'use strict';

let Server = require('./server/serve.js'),
    UserManager = require('./users/users.js'),
    DoorControl = require('./door.js');

let setupServer = (config) => {
    let userManager = new UserManager(config.usersFile, config.passwordAlgorithm),
        eventsRoot = config.eventsRoot ? config.eventsRoot : '/chimera-ws-events';
    return new Server(config.port, config.htmlRoot, eventsRoot, userManager, config.authentication);
};

exports.run = (config) => {
    let server = setupServer(config);

    server.get('open', () => {
        DoorControl.open();
        return true;
    });

    server.get('close', () => {
        DoorControl.close();
        return true;
    });

	server.start();
};
