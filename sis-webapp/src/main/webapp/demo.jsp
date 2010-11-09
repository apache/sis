<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!-- 
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at
 
      http://www.apache.org/licenses/LICENSE-2.0
 
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 -->
<html>
<head>
<title>Apache SIS Spatial Query Demo with Quad Tree Storage and Google Maps
API</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="initial-scale=1.0, user-scalable=no" />
<style type="text/css">
html {
	height: 100%
}

body {
	height: 100%;
	font-family: arial, Sans-serif;
	font-size: 12px;
}

#map_canvas {
	height: 85%;
	width: 50%;
	font-size: 10px;
}

#result {
	font-color: #808080;
	font-size: 10px;
}
</style>
<script type="text/javascript"
	src="http://maps.google.com/maps/api/js?sensor=true"></script>

<script type="text/javascript">
	var map;
	var infoWindow;
	var req;
	
	function initialize() {
		var myLatlng = new google.maps.LatLng(0, 0);
		var myOptions = {
			zoom : 1,
			center : myLatlng,
			mapTypeId : google.maps.MapTypeId.ROADMAP
		}
		map = new google.maps.Map(document.getElementById("map_canvas"),
				myOptions);

		//get the query string
		var queryStr = window.location.search.substring(1);
		var queryStrSplit = queryStr.split("&");
		for (var i = 0; i < queryStrSplit.length; i++)
		{
			var keyValue = queryStrSplit[i].split("=");
			if (keyValue[0] == "type")
			{
				document.getElementById("type").value = keyValue[1];
				switchType();
			}
			else if (keyValue[0] == "llLat")
				document.getElementById("llLat").value = keyValue[1];
			else if (keyValue[0] == "llLon")
				document.getElementById("llLon").value = keyValue[1];
			else if (keyValue[0] == "urLat")
				document.getElementById("urLat").value = keyValue[1];
			else if (keyValue[0] == "urLon")
				document.getElementById("urLon").value = keyValue[1];
			else if (keyValue[0] == "lat")
				document.getElementById("lat").value = keyValue[1];
			else if (keyValue[0] == "lon")
				document.getElementById("lon").value = keyValue[1];
			else if (keyValue[0] == "radius")
				document.getElementById("radius").value = keyValue[1];
		}

		if (queryStr != "") {
			getXMLDoc("/location?" + queryStr);
		}
	}

	function getXMLDoc(url) {
		req = false;

		if (window.XMLHttpRequest) {
			try {
				req = new XMLHttpRequest();
			} catch (e) {
				req = false;
			}
		} else if (window.ActiveXObject) {
			try {
				req = new ActiveXObject("Msxml2.XMLHTTP");
			} catch (e) {
				try {
					req = new ActiveXObject("Microsoft.XMLHTTP");
				} catch (e) {
					req = false;
				}
			}
		}
		if (req) {
			req.onreadystatechange = processXMLDoc;
			req.open("GET", url, true);
			req.send("");
		}
	}

	function processXMLDoc() {
		if (req.readyState == 4) {
			if (req.status == 200) {
				var xmlDoc = req.responseXML;
				var ids = xmlDoc.getElementsByTagName("id");
				var lats = xmlDoc.getElementsByTagName("lat");
				var lons = xmlDoc.getElementsByTagName("lon");
				var regions = xmlDoc.getElementsByTagName("region");
				var time = xmlDoc.getElementsByTagName("time");

				var indexLoadTime = xmlDoc.getElementsByTagName("indexLoadTime");
				if (indexLoadTime != null && indexLoadTime.length == 1) {
					alert(indexLoadTime[0].firstChild.nodeValue);
				}

				document.getElementById("result").innerHTML = ids.length + " results (" + ( parseInt(time[0].firstChild.nodeValue)  / 1000 ) + " seconds)";
				
				for ( var i = 0; i < ids.length; i++) {
					var latLon = new google.maps.LatLng(
							parseFloat(lats[i].firstChild.nodeValue),
							parseFloat(lons[i].firstChild.nodeValue));

					var filename = ids[i].firstChild.nodeValue;
					var marker = createMarker(latLon, filename);
				}

				for (var j = 0; j < regions.length; j++)
				{
					var regionStr = regions[j].firstChild.nodeValue;
					var latLonPairs = regionStr.split(",");
					var regionCoordinates = [];
					for (var k = 0; k < latLonPairs.length; k+=2)
					{
						var lon = latLonPairs[k+1];
						if (lon == 180.0)
							lon = 179.99;
						var point = new google.maps.LatLng(latLonPairs[k], lon);
						regionCoordinates.push(point);
					}
					var regionPath = new google.maps.Polygon({
						    paths: regionCoordinates,
						    strokeColor: "#FF0000",
						    strokeOpacity: 0.8,
						    strokeWeight: 2,
						    fillColor: "#FF0000",
						    fillOpacity: 0.0
						  });

					regionPath.setMap(map);
					
				}
			} else {
				alert("Error retrieving results from server: " + req.statusText);
			}
		}
	}
	var globalMarker;
	function getHTMLDescription(filename, marker) {
		req = false;

		if (window.XMLHttpRequest) {
			try {
				req = new XMLHttpRequest();
			} catch (e) {
				req = false;
			}
		} else if (window.ActiveXObject) {
			try {
				req = new ActiveXObject("Msxml2.XMLHTTP");
			} catch (e) {
				try {
					req = new ActiveXObject("Microsoft.XMLHTTP");
				} catch (e) {
					req = false;
				}
			}
		}
		if (req) {
			globalMarker = marker;
			req.onreadystatechange = displayMarker;
			req.open("POST", "/location", true);
			req.setRequestHeader("Content-type","application/x-www-form-urlencoded");
			req.send("filename=" + filename);
		}
	}
	
	function createMarker(latLon, filename) {
		var marker = new google.maps.Marker( {
			position : latLon,
			map : map
		});
		google.maps.event.addListener(marker, "click", function(event)
		{
			getHTMLDescription(filename, marker);
		});
		return marker;
	}

	function displayMarker()
	{
		if (req.readyState == 4) {
			if (req.status == 200) {
				var text = req.responseText;
				if (infoWindow != null)
					infoWindow.close();
				infoWindow = new google.maps.InfoWindow( {
					content : text,
					maxWidth : 300
				});
				infoWindow.open(map, globalMarker);
			}
		}
	}
	
	function switchType()
	{

		if (document.getElementById("type").selectedIndex == 0)
		{
			document.getElementById("bbox").style.visibility = "visible";
			document.getElementById("bbox").style.display = "block";
			document.getElementById("pointradius").style.visibility = "hidden";
			document.getElementById("pointradius").style.display = "none";
		}
		else
		{
			document.getElementById("bbox").style.visibility = "hidden";
			document.getElementById("bbox").style.display = "none";
			document.getElementById("pointradius").style.visibility = "visible";
			document.getElementById("pointradius").style.display = "block";
		}
	}

	function validate() {
		var regex = /-?[0-9]+.?[0-9]*/;
		if (document.getElementById("type").selectedIndex == 0) {
			if (document.getElementById("llLat").value.match(regex)
					&& document.getElementById("llLon").value.match(regex)
					&& document.getElementById("urLat").value.match(regex)
					&& document.getElementById("urLon").value.match(regex)) {
				return true;
			} else {
				alert("Enter valid lat/lon values in corresponding fields.");
				return false;
			}
		} else if (document.getElementById("type").selectedIndex == 1) {
			if (document.getElementById("lat").value.match(regex)
					&& document.getElementById("lon").value.match(regex)
					&& document.getElementById("radius").value.match(regex)) {
				return true;
			} else {
				alert("Enter valid lat/lon and radius values in corresponding fields.");
				return false;
			}
		}
		
	}
</script>
</head>
<body onload="initialize()">
<form method="get"
	onsubmit="return validate()">

<p><label><b>Query By&nbsp;</b></label> <select id="type"
	onchange="switchType()" name="type">
	<option value="bbox">Bounding Box</option>
	<option value="pointradius">Point-Radius</option>
</select></p>
<span id="bbox">
<table cellpadding="5" cellspacing="5">
	<tr>
		<td><b>Lower Left</b></td>
		<td>Latitude&nbsp;<input id="llLat" type="text" size="8"
			name="llLat" /></td>

		<td>Longitude&nbsp;<input id="llLon" type="text" size="8"
			name="llLon" /></td>
	</tr>
	<tr>
		<td><b>Upper Right</b></td>
		<td>Latitude&nbsp;<input id="urLat" type="text" size="8"
			name="urLat" /></td>
		<td>Longitude&nbsp;<input id="urLon" type="text" size="8"
			name="urLon" /></td>
	</tr>
</table>
</span> <span id="pointradius" style="display: none; visibility: hidden;">
<table cellpadding="5" cellspacing="5]">
	<tr>
		<td>Latitude&nbsp;<input id="lat" type="text" size="8" name="lat" /></td>

		<td>Longitude&nbsp;<input id="lon" type="text" size="8"
			name="lon" /></td>
		<td>Radius (km)&nbsp;<input id="radius" type="text" size="8"
			name="radius" /></td>
	</tr>
</table>
</span>
<p><input type="submit" value="Query" /></p>
</form>
<p id="result"></p>
<div id="map_canvas"></div>
</body>
</html>