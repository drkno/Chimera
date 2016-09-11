wudhaghControllers.controller('ShoppingHistoryController', ['$scope', '$http',
    function ($scope, $http) {

        $scope.shopsChart = {
            labels: [],
            series: [],
            data: []
        };

        $scope.itemsPie = {
            labels: [],
            data: [],
            legend: false,
            toggleLegend: (s) => {
                if (s || s === false) {
                    this.legend = s;
                }
                else {
                    this.legend = !this.legend;
                }
                let outcome = this.legend? 'block' : 'none';
                $('chart-legend').css('display', outcome);
            }
        };

        $scope.items = [];

        $scope.getItems = () => {
            $http({
                method: 'GET',
                url: '/api/suggestions'
            })
            .then((response) => {
                $scope.itemsData = response.data;
                    
                // Construct Shopping Trips Chart
                $scope.shopsChart.series = ['Items per Trip'];
                $scope.shopsChart.data = [response.data.shops];
                let labels = [];
                for (let i = 1; i <= response.data.shops.length; i++) {
					if (i === response.data.shops.length) {
						labels.push('Current');
					}
					else {
						labels.push('Shop ' + i);
					}
                }
                $scope.shopsChart.labels = labels;
                    
                // Construct Shopping Items Pie
                for (let i = 0; i < response.data.items.length; i++) {
                    $scope.itemsPie.labels.push(response.data.items[i][0]);
                    $scope.itemsPie.data.push(response.data.items[i][1]);
                }
                $scope.itemsPie.toggleLegend(false);
                
                $scope.items = response.data.items;
            },
            () => {});
        };

        $scope.getItems();
    }
]);