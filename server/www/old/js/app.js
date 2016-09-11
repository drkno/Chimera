let Wudhagh = angular.module('Wudhagh', [
    'ngRoute',
    'ui.bootstrap',
    'ngTouch',
    'chart.js',
    'btford.socket-io',
    'wudhaghControllers'
]),

routes = [
    {
        path: '/shoppinglist',
        templateUrl: 'pages/shoppinglist.html',
        controller: 'ShoppingListController',
        name: 'Shopping List'
    },
    {
        path: '/shoppinghistory',
        templateUrl: 'pages/shoppinghistory.html',
        controller: 'ShoppingHistoryController',
        name: 'Shopping History'
    }
],

defaultRoute = 0;

Wudhagh.config(['$routeProvider', '$locationProvider', ($routeProvider, $locationProvider) => {

    let def = $routeProvider;
    for (let i = 0; i < routes.length; i++) {
        def = def.when(routes[i].path, routes[i]);
    }
    def.otherwise({
        redirectTo: routes[defaultRoute].path
    });

//    $locationProvider.html5Mode(true);
}]);

let wudhaghControllers = angular.module('wudhaghControllers', ['ui.bootstrap', 'ngTouch', 'chart.js', 'btford.socket-io']);

wudhaghControllers.factory('socket', (socketFactory) => {
    let wudhaghIoSocket = io.connect('/', { path: '/wudhagh-ws-events' }),
        wudhaghSocket = socketFactory({
            ioSocket: wudhaghIoSocket
        });
    return wudhaghSocket;
});

Wudhagh.run(['$route', ($route) => {
    $route.reload();
}]);
