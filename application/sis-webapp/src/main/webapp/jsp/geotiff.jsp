<!DOCTYPE html>
<html>
    <style>
        table,th,td {
            border : 1px solid black;
            border-collapse: collapse;
        }

        th,td {
            padding: 5px;
        }

        .image{
            height:50px;
            width:50px;
        }
    </style>
    <body onload="loadDoc()">
        <br><br>
        <table id="demo"></table>
        <table id="detail"></table>

        <!--       105.19,109.55,14.7,17.9-->

        <script>



            function loadDoc() {
                var xhttp = new XMLHttpRequest();

                xhttp.onreadystatechange = function () {
                    if (xhttp.readyState === 4 && xhttp.status === 200) {

                        myFunction(xhttp);
                    }
                };
                var format = "<%= request.getParameter("format")%>";
                if (format==='null'){
                format = "";
    }
    var identifier = "<%= request.getParameter("identifier")%>";
     if (identifier==='null'){
                identifier = "";
    }
                var myString = "<%= request.getParameter("divid")%>";
                var mySplitResult;
                mySplitResult = myString.split(",");
                var west = mySplitResult[0];
                if (west === 'null') {
                    west = "";
                }
                var east = mySplitResult[1];
                if (typeof east === 'undefined') {
                    east = "";
                }
                var south = mySplitResult[2];
                if (typeof south === 'undefined') {
                    south = "";
                }
                var north = mySplitResult[3];
                if (typeof north === 'undefined') {
                    north = "";
                }

                xhttp.open("GET", "http://localhost:8080/MavenWebProject/VNSC/csw/2.0.2/GetRecord?format="+format+"&identifier="+identifier+"&west=" + west + "&east=" + east + "&south=" + south + "&north=" + north + "&startDate=<%= request.getParameter("date1")%>&rangeDate=<%= request.getParameter("date2")%>", true);
                xhttp.send();
//                document.write("http://localhost:8080/MavenWebProject/VNSC/csw/Search?format="+format+"&identifier=&west=" +west + "&east=" + east + "&south=" + south + "&north=" + north + "&startDate="+date1+"&rangeDate="+date2);
            }
            function myFunction(xml) {
                var i;
                var xmlDoc = xml.responseXML;

                var table = "<tr><th>STT</th><th>Image</th><th>Name</th><th>Format</th><th>Download</th></tr>";

                var x = xmlDoc.getElementsByTagName("Record");
                //   console.log(x[0].getElementsByTagName("dc:identifier")[0].childNodes[0].nodeValue);
                //   console.log(x[0].getElementsByTagName("dc:format")[0].childNodes[0].nodeValue);
                for (i = 0; i < x.length; i++) {
                    var stt = i + 1;
                    table += "<tr><td>" +
                            stt +
                            "</td><td>" +
                            "<img class='image' src='http://localhost:8080/MavenWebProject/VNSC/wcs/image/" +
                            x[i].getElementsByTagName("identifier")[0].childNodes[0].nodeValue + ".jpg'/>" +
                            "</td><td>" +
                            "<a href='#' onclick='loadMetadata(" + x[i].getElementsByTagName("id")[0].childNodes[0].nodeValue + ")'>" +
                            x[i].getElementsByTagName("title")[0].childNodes[0].nodeValue + "</a>" +
                            "</td><td>" +
                            x[i].getElementsByTagName("format")[0].childNodes[0].nodeValue +
                            "</td><td>" +
                            "<a href = 'http://localhost:8080/MavenWebProject/VNSC/csw/2.0.2/download/" + x[i].getElementsByTagName("name")[0].childNodes[0].nodeValue + "'><button >Download</button></a>" +
                            "</td></tr>";
                }
                //  console.log(table);
                document.getElementById("demo").innerHTML = table;
            }

            function loadMetadata(id) {
                document.getElementById("demo").innerHTML = "";
                var getXML = new XMLHttpRequest();

                getXML.onreadystatechange = function () {
                    if (getXML.readyState === 4 && getXML.status === 200) {
                        console.log(getXML);
                        tableMetadata(getXML);
                    }
                };
                getXML.open("GET", "http://localhost:8080/MavenWebProject/VNSC/csw/2.0.2/GetRecordById?Id=" + id, true);
                getXML.send();
            }

            function tableMetadata(x) {
                var doc = x.responseXML;
                var metadata = doc.getElementsByTagName("Record")[0];
                var bbox = metadata.getElementsByTagName("BoundingBox");

                console.log(bbox[0].getElementsByTagName("eastBoundLongitude")[0].childNodes[0].nodeValue);
                var table = "<tr><th>Field</th><th>Detail</th></tr>";
                table += "<tr><td>ID</td><td>" + metadata.getElementsByTagName("id")[0].childNodes[0].nodeValue + "</td></tr>" +
                        "<tr><td>Name</td><td>" + metadata.getElementsByTagName("name")[0].childNodes[0].nodeValue + "</td></tr>" +
                        "<tr><td>Title</td><td>" + metadata.getElementsByTagName("title")[0].childNodes[0].nodeValue + "</td></tr>" +
                        "<tr><td>Subject</td><td>" + metadata.getElementsByTagName("Subject")[0].childNodes[0].nodeValue + "</td></tr>" +
                        "<tr><td>Date</td><td>" + metadata.getElementsByTagName("modified")[0].childNodes[0].nodeValue + "</td></tr>" +
                        "<tr><td>Type</td><td>" + metadata.getElementsByTagName("type")[0].childNodes[0].nodeValue + "</td></tr>" +
                        "<tr><td>Format</td><td>" + metadata.getElementsByTagName("format")[0].childNodes[0].nodeValue + "</td></tr>" +
                        "<tr><td>Identifier</td><td>" + metadata.getElementsByTagName("identifier")[0].childNodes[0].nodeValue + "</td></tr>" +
                        "<tr><td>Language</td><td>" + metadata.getElementsByTagName("language")[0].childNodes[0].nodeValue + "</td></tr>" +
                        "<tr><td>east</td><td>" + bbox[0].getElementsByTagName("eastBoundLongitude")[0].childNodes[0].nodeValue + "</td></tr>" +
                        "<tr><td>west</td><td>" + bbox[0].getElementsByTagName("westBoundLongitude")[0].childNodes[0].nodeValue + "</td></tr>" +
                        "<tr><td>south</td><td>" + bbox[0].getElementsByTagName("southBoundLatitude")[0].childNodes[0].nodeValue + "</td></tr>" +
                        "<tr><td>north</td><td>" + bbox[0].getElementsByTagName("northBoundLatitude")[0].childNodes[0].nodeValue + "</td></tr>";


                document.getElementById("detail").innerHTML = table;
            }
        </script>

    </body>
</html>
