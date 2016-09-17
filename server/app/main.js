'use strict';

const Server = require('./server/serve.js'),
    UserManager = require('./users/users.js'),
    DoorControl = require('./door.js'),
    VideoWatch = require('./videos.js'),
    AuditLog = require('./audit.js');

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

    server.apiGet('open', (req) => {
        DoorControl.open();
        AuditLog.log('Open Door', req.authorizedUser, req.ipv4,
            req.useragent.browser + ' ' + req.useragent.version + ' on ' + req.useragent.os);
        return true;
    });

    server.apiGet('close', (req) => {
        DoorControl.close();
        AuditLog.log('Close Door', req.authorizedUser, req.ipv4,
            req.useragent.browser + ' ' + req.useragent.version + ' on ' + req.useragent.os);
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
            AuditLog.log('Delete Video (' + data.delete + ')', socket.request.authorizedUser, socket.request.ipv4,
                socket.request.useragent.browser + ' ' + socket.request.useragent.version + ' on ' + socket.request.useragent.os);
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
        socket.emit('state', {
            state: DoorControl.getState(),
            webcamFeed: config.webcamFeed
        });
    });

    DoorControl.on('change', (state) => {
        server.emit('state', state);
    });

    server.apiGet('auditLog', (req, res) => {
        AuditLog.readLog((data) => {
            res.send({
                success: true,
                data: data
            });
        });
    });

    server.start();
};
