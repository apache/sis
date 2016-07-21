var loc = "http://localhost:8084/MavenWebProject/VNSC/csw/";

var app = angular.module('App', []);
    app.controller('Ctrl', function($scope, $location, $http) {
        var parameter = $location.path() ;
        console.log($location);
        $http.get(loc+"GetRecordByID" + parameter ).then(function(response){
           $scope.data = response.data.SummaryRecord; 
        });
        
    });
    
    
    
    