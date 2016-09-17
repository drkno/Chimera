'use strict';

let Server = require('./server/serve.js'),
    UserManager = require('./users/users.js'),
    DoorControl = require('./door.js'),
    VideoWatch = require('./videos.js');

let setupServer = (config) => {
    let userManager = new UserManager(config.usersFile, config.passwordAlgorithm),
        eventsRoot = config.eventsRoot ? config.eventsRoot : '/chimera-ws-events';

    if (!config.htmlRoot) {
        config.htmlRoot = {
            '/bower_components/': './../../bower_components/',
            '/videos/': config.videosDirectory,
            '': './../../www/'
        };
    }

    return new Server(config.port, config.htmlRoot, eventsRoot, userManager, config.authentication);
};

exports.run = (config) => {
    let videos = new VideoWatch(config.videosDirectory),
        server = setupServer(config);

    server.apiGet('open', () => {
        DoorControl.open();
        return true;
    });

    server.apiGet('close', () => {
        DoorControl.close();
        return true;
    });

    server.apiGet('door', (req, res) => {
        res.send({
            complete: true,
            state: DoorControl.getState()
        });
    });

    server.on('videos', (socket, data) => {
        if (data && data.delete) {
            videos.delete(data.delete);
        }
        else {
            socket.emit('videos', videos.getFiles());
        }
    });

    videos.on('change', (files) => {
        server.emit('videos', files);
    });

    server.on('state', (socket) => {
        socket.emit('state', DoorControl.getState());
    });

    DoorControl.on('change', (state) => {
        server.emit('state', state);
    });

    server.start();
};
