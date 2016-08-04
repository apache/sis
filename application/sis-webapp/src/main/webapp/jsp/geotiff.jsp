<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
       <link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css">
        <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
        <script src="http://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"></script>
    </head>
    <style>
/*        table,th,td {
            border : 1px solid black;
            border-collapse: collapse;
        }*/

/*        th,td {
            padding: 5px;
        }*/

        .image{
            height:50px;
            width:50px;
        }
    </style>
    <body onload="loadDoc()">
        <center>
            <h1>Result</h1><br><br>
            <img id="loading" class="image" src="../images/loading.gif"/>
        </center>
        <table class="table table-striped" style="margin-left: 20px; margin-right: 20px;" id="demo"></table>

        <!--       105.19,109.55,14.7,17.9-->

        <script>


            document.getElementById('loading').style.display = "";
            function loadDoc() {
                var xhttp = new XMLHttpRequest();
                
                xhttp.onreadystatechange = function () {
                    if (xhttp.readyState === 4 && xhttp.status === 200) {
                        document.getElementById('loading').style.display = "none";
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

                xhttp.open("GET", "http://localhost:8084/sis/VNSC/csw/2.0.2/filter?service=CSW&version=2.0.2&request=GetRecords&constraintLanguage=filter&format="+format+"&identifier="+identifier+"&west=" + west + "&east=" + east + "&south=" + south + "&north=" + north + "&startDate=<%= request.getParameter("date1")%>&rangeDate=<%= request.getParameter("date2")%>", true);
                xhttp.send();
//                document.write("http://localhost:8084/MavenWebProject/VNSC/csw/Search?format="+format+"&identifier=&west=" +west + "&east=" + east + "&south=" + south + "&north=" + north + "&startDate="+date1+"&rangeDate="+date2);
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
                            "<img class='image' src='http://localhost:8084/sis/VNSC/wcs/image/" +
                            x[i].getElementsByTagName("identifier")[0].childNodes[0].nodeValue + ".jpg'/>" +
                            "</td><td>" +
                            "<a href='detail.jsp?identifier=" +x[i].getElementsByTagName("identifier")[0].childNodes[0].nodeValue+"&format="+ x[i].getElementsByTagName("format")[0].childNodes[0].nodeValue+"'>"+
                            x[i].getElementsByTagName("title")[0].childNodes[0].nodeValue + "</a>" +
                            "</td><td>" +
                            x[i].getElementsByTagName("format")[0].childNodes[0].nodeValue +
                            "</td><td>" +
                            "<a href = 'http://localhost:8084/sis/VNSC/csw/2.0.2/download/" + 
                            x[i].getElementsByTagName("title")[0].childNodes[0].nodeValue + 
                            "'><button >Download</button></a>" +
                            "</td></tr>";
                }
                //  console.log(table);
                document.getElementById("demo").innerHTML = table;
            }

        </script>

    </body>
</html>
