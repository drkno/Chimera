var config;
try {
  config = require('./config.json');
}
catch(e) {
  config = {};
}

process.on('uncaughtException', (err) => {
    console.log(err);
    process.exit(-1);
});

let normalExitHandler = () => {
    console.log('exit')
    process.exit();
};
process.on('SIGINT', normalExitHandler);
process.on('SIGHUP', normalExitHandler);
process.on('exit', normalExitHandler);

require('./app/main.js').run(config);
