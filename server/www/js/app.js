let AppName = 'Chimera';

let Chimera = angular.module('chimeraApp', [
    'ngRoute',
    'ui.bootstrap',
    'btford.socket-io',
    'chimeraControllers'
]),

routes = [
    {
        path: '/door',
        templateUrl: 'pages/door.html',
        controller: 'DoorController',
        name: 'Garage Door'
    }
],

defaultRoute = 0;

Chimera.config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {

    var def = $routeProvider;
    for (var i = 0; i < routes.length; i++) {
        def = def.when(routes[i].path, routes[i]);
    }
    def.otherwise({
        redirectTo: routes[defaultRoute].path
    });

//    $locationProvider.html5Mode(true);
}]);

let chimeraControllers = angular.module('chimeraControllers', ['ui.bootstrap', 'btford.socket-io']);

chimeraControllers.factory('socket', function (socketFactory) {
    var ioSocket = io.connect('/', { path: '/chimera-ws-events' });
    var socket = socketFactory({
        ioSocket: ioSocket
    });
    return socket;
});

// http://stackoverflow.com/questions/23659395/can-i-use-angular-variables-as-the-source-of-an-audio-tag
chimeraControllers.directive('audios', function($sce) {
    return {
        restrict: 'A',
        scope: { code:'=' },
        replace: true,
        template: '<audio ng-src="{{url}}" controls></audio>',
        link: function (scope) {
            scope.$watch('code', function (newVal, oldVal) {
                if (newVal !== undefined) {
                    scope.url = $sce.trustAsResourceUrl(newVal);
                }
            });
        }
    };
});

Chimera.run(['$route', ($route) => {
    $route.reload();
}]);
