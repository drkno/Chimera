wudhaghControllers.controller('ShoppingListController', ['$scope', '$http', '$filter', 'socket',
    function ($scope, $http, $filter, socket) {

        //#region data context

        $scope.items = [];
        $scope.numDays = 0;
        $scope.contextItem = null;
        $scope.suggestions = null;
        let currName = null;

        $scope.setContext = (item) => {
            currName = item.name;
            $scope.contextItem = angular.copy(item);
            $scope.contextItem.insert = false;
        };
        
        let resetSelectionContext = () => {
            $scope.contextItem = {
                name: "",
                quantity: 1,
                insert: true,
                unit: '#',
                purchased: false
            };
            $scope.suggestions = null;
            currName = null;
        };
        
        let getDays = () => {
            var currMin = new Date();
            for (var i = 0; i < $scope.items.length; i++) {
                if (new Date($scope.items[i].added) < currMin) {
                    currMin = new Date($scope.items[i].added);
                }
            }
            return Math.round(Math.abs(((new Date()).getTime() - currMin.getTime()) / (86400000)));
        };
        
        let setOrder = () => {
            $scope.items.sort((a, b) => {
                if (a.name > b.name) return 1;
                if (a.name === b.name) return 0;
                return -1;
            });
            $scope.numDays = getDays();
        };

        //#endregion data context

        //#region current context

        socket.on('current', (data) => {
            $scope.items = data.items;
            resetSelectionContext();
            setOrder();
        });
        socket.emit('refresh');

        //#endregion

        //#region new

        let newList = () => {
            $scope.items = [];
            resetSelectionContext();
            setOrder();
        };

        socket.on('new', () => {
            newList();
        });

        $scope.newList = () => {
            newList();
            socket.emit('new');
        };

        //#endregion new

        //#region add

        let addItem = (data) => {
            $scope.items.push(data);
            setOrder();
        };

        socket.on('add', (data) => {
            addItem(data);
        });

        let performAdd = (item) => {
            delete item.insert;
            addItem(item);
            socket.emit('add', item);
            resetSelectionContext();
        };

        //#endregion add

        //#region remove

        let removeItem = (item) => {
            for (let i = 0; i < $scope.items.length; i++) {
                if ($scope.items[i].name === item.name) {
                    $scope.items.splice(i, 1);
                    break;
                }
            }
        };

        socket.on('remove', (data) => {
            removeItem(data);
        });

        $scope.deleteItem = () => {
            removeItem($scope.contextItem);
            socket.emit('remove', $scope.contextItem);
            resetSelectionContext();
        };

        //#endregion remove

        //#region update

        let updateItem = (oldName, item) => {
            if (oldName !== item.name) {
                removeItem({ name: oldName });
                addItem(item);
            }
            else {
                for (let i = 0; i < $scope.items.length; i++) {
                    if ($scope.items[i].name === item.name) {
                        $scope.items[i] = item;
                        break;
                    }
                }
            }
        };

        socket.on('update', (data) => {
            updateItem(data.oldName, data.item);
        });

        let performUpdate = (oldName, item) => {
            delete item.insert;
            updateItem(oldName, item);
            socket.emit('update', { oldName: oldName, item: item });
            resetSelectionContext();
        };

        //#endregion update

        //#region swipe

        let swipeUIToggle = (data) => {
            for (let i = 0; i < $scope.items.length; i++) {
                if ($scope.items[i].name === data.name) {
                    $scope.items[i].purchased = data.purchased;
                    break;
                }
            }
        };

        socket.on('swipe', (data) => {
            swipeUIToggle(data);
        });

        $scope.swipeLeft = (item) => {
            item.purchased = true;
            socket.emit('swipe', { name: item.name, purchased: true });
        };

        $scope.swipeRight = (item) => {
            item.purchased = false;
            socket.emit('swipe', { name: item.name, purchased: false });
        };

        //#endregion swipe

        //#region suggestions

        const suggestionsLimit = 8;

        $scope.getSuggestions = (val) => {
            if ($scope.suggestions === null) {
                return $http({
                    method: 'GET',
                    url: '/api/suggestions'
                })
                .then((response) => {
                    $scope.suggestions = response.data.items.map((item) => {
                        return item[0];
                    });
                    return $filter('limitTo')($filter('filter')($scope.suggestions, val), suggestionsLimit);
                },
                () => {
                    return [];
                });
            }
            else {
                return $filter('limitTo')($filter('filter')($scope.suggestions, val), suggestionsLimit);
            }
        };

        //#endregion suggestions

        //#region misc UI functions

        $scope.dtToString = (d) => {
            let dt = new Date(d),
                day = dt.getDate(),
                month = dt.getMonth() + 1,
                year = dt.getFullYear(),
                hours = dt.getHours(),
                min = dt.getMinutes();

            return day + '/' + pad(month, 2) + '/' + year + ' ' + hours + ':' + pad(min, 2);
        };

        $scope.getUnit = (item) => {
            if (item.unit && item.unit !== '#') {
                let unit = item.unit;
                if (unit.length >= 3 && item.quantity > 1) {
                    switch (unit) {
                        case 'Loaf': unit = 'Loaves'; break;
                        default: unit += 's'; break;
                    }
                }
                return unit;
            }
            return '';
        };

        $scope.countItems = () => {
            let counter = 0;
            for (let i = 0; i < $scope.items.length; i++) {
                if ($scope.items[i].unit && $scope.items[i].unit === '#') {
                    counter += $scope.items[i].quantity;
                }
                else {
                    counter += 1;
                }
            }
            return counter;
        };

        $scope.approximateBags = () => {
            let count = $scope.items.length;
            return Math.ceil(count / 5);
        };

        let pad = (n, width, z) => {
            z = z || '0';
            n = n + '';
            return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
        }

        $scope.print = () => {
            let printContents = document.getElementById('itemsTable').outerHTML,
                popupWin = window.open('', '_blank', 'width=300,height=300'),

                // yes, I agree. Get me a better cross browser solution and I will happily use it
                html = '<!doctype html><html><head><title>Shopping List</title><link rel="stylesheet" type="text/css" href="style.css" />' +
                    '<link rel="stylesheet" type="text/css" href="../bower_components/bootstrap/dist/css/bootstrap.min.css" />' +
                    '<style>.print-remove{display:none;}.table-nonfluid{width:auto !important;}body{font-size:10px}</style></head><body>'
                    + printContents + '<script>setTimeout(function(){window.print();},500);window.onfocus=function(){' +
                    'setTimeout(function(){window.close();},500);}</script></body></html>';

            popupWin.document.open();
            popupWin.document.write(html);
            popupWin.document.close();
        };

        $scope.saveItem = () => {
            if (!$scope.contextItem.name || $scope.contextItem.name.trim().length === 0 || !$scope.contextItem.quantity) {
                alert('Invalid input.');
                resetSelectionContext();
                return;
            }

            $scope.contextItem.name = $scope.contextItem.name.replace(/\w\S*/g, (txt) => {
                return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
            }).trim();

            var filtered = $scope.items.filter((obj) => {
                return obj.name === $scope.contextItem.name;
            });

            if (filtered.length > 0) {
                let cpy = angular.copy(filtered[0]);
                if ($scope.contextItem.insert) {
                    cpy.quantity += $scope.contextItem.quantity;
                }
                else {
                    cpy.quantity = $scope.contextItem.quantity;
                }
                $scope.contextItem = cpy;
                currName = cpy.name;
            }

            if (currName !== null) {
                performUpdate(currName, $scope.contextItem);
                return;
            }

            $scope.contextItem.added = new Date().toISOString();
            performAdd($scope.contextItem);
        };

        $scope.resetContext = () => {
            resetSelectionContext();
        };

    }]);