var loc = "http://localhost:8084/MavenWebProject/VNSC/csw/";

var app = angular.module('App', []);

    app.filter('format', function() {
    return function(movies,genres) {
      var out = [];
      // Filter logic here, adding matches to the out var.
      return out;
    }
  });
    app.controller('Ctrl', function($scope, $http, $location) {
        $scope.search={identifier:"",format:{geotiff:true, modis:true}, from:5, to:"", month:"", coordinates:{long1:"",lat1:"", long2:"",lat2:""}};
        $scope.print = "";
//      $http.get(loc + "GetRecord/GEOTIFF")
//      .then(function(response) {
//         
//          $scope.data = response.data;
//          console.log($scope.data);
//          $scope.bbox = $scope.data.BoundingBox;
//      });
         
        
            
            
        $scope.submit= function(){
//                $scope.checkFormat = true;
                if($scope.search.format.geotiff == true && $scope.search.format.modis == false){
                    $http.get(loc+"GetRecordByFormat/GEOTIFF").then(function(response){
                        $scope.output = response.data.SummaryRecord;
                        console.log($scope.test($scope.output));
                    });   
                }
//                
                if($scope.search.format.modis == true && $scope.search.format.geotiff == false){
                    $http.get(loc+"GetRecordByFormat/MOD021KM").then(function(response){
                        $scope.output= response.data.SummaryRecord;
                        console.log($scope.test($scope.output));
                    });
                }
                
                if($scope.search.format.geotiff == true && $scope.search.format.modis == true){
                    $http.get(loc+"DescribeRecord").then(function(response){
                        $scope.output= response.data.SummaryRecord;
                        console.log($scope.test($scope.output));
                    });
                }
        }
           
            
        $scope.test=function(a){
            var log;
            angular.forEach(a,function(key,value){
                var a;
                a = key.BoundingBox;
//                console.log(a.eastBoundLongitude);
                return a;
            });
        }
        
        $scope.back = function(){
            $scope.output = "";
        }
        
        
        


    });
    
    
    
    