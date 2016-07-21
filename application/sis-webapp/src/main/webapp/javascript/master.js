/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


var loc = "http://localhost:8084/MavenWebProject/VNSC/csw/";

var app = angular.module('App', []);
    app.controller('myCtrl', function($scope, $http) {
        $scope.test="aaaaaaa";
        $scope.ssss="aaaaaaa";
        
      $http.get(loc + "GetRecord/GEOTIFF")
      .then(function(response) {
         
          $scope.data = response.data;
          console.log($scope.data);
          $scope.bbox = $scope.data.BoundingBox;
      });      
    });
    
    