<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
        <link rel="stylesheet" href="../css/geotiff.css"/>
       <link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css">
        <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
        <script src="http://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"></script>
    </head>
    
    <body onload="loadDoc()">
        <center>
            <h1>Result</h1><br><br>
            <img style="width: 50px; height: 50px" id="loading" class="image" src="../images/loading.gif"/>
            <div id="no-result" style="display:none;">
                <h2 style="color: #fa2730">No result find</h2>
                <button onclick="goBack()">Search Again</button>
                
            </div>
        
            <!--<img id="myImg" src="../images/bg.jpg" alt="Trolltunga, Norway" width="300" height="200">-->

        <!-- The Modal -->
        <div id="myModal" class="modal">

          <!-- The Close Button -->
          <span class="close-img" onclick="document.getElementById('myModal').style.display='none'">&times;</span>

          <!-- Modal Content (The Image) -->
          <img class="modal-content" id="img01">

          <!-- Modal Caption (Image Text) -->
          <div id="caption"></div>
        </div>
        <table class="table table-striped" style="margin-left: 20px; margin-right: 20px;width:80%" id="demo"></table>
        </center>
        <!--       105.19,109.55,14.7,17.9-->
        
        <script>
            function goBack() {
                window.history.back();
            }
            
            function loadDoc() {
                var xhttp = new XMLHttpRequest();
                
                xhttp.onreadystatechange = function () {
                    if (xhttp.readyState === 4 && xhttp.status === 200) {
                        $('#loading').hide();
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

                xhttp.open("GET", "http://localhost:8084/sis/VNSC/csw/2.0.2/filter?service=CSW&version=2.0.2&request=GetRecords&resultType=results&typeName=gmd:Metadata&constraintLanguage=filter&format="+format+"&identifier="+identifier+"&west=" + west + "&east=" + east + "&south=" + south + "&north=" + north + "&startDate=<%= request.getParameter("date1")%>&rangeDate=<%= request.getParameter("date2")%>&elementSetName=full", true);
                xhttp.send();
//                document.write("http://localhost:8084/MavenWebProject/VNSC/csw/Search?format="+format+"&identifier=&west=" +west + "&east=" + east + "&south=" + south + "&north=" + north + "&startDate="+date1+"&rangeDate="+date2);
            }
            function myFunction(xml) {
                var i;
                var xmlDoc = xml.responseXML;
                
                var table = "<tr><th>STT</th><th>Image</th><th>Name</th><th>Format</th><th>Download</th></tr>";

                var x = xmlDoc.getElementsByTagName("Record");
                console.log(x.length);
                if(x.length == 0) {
                    $('#no-result').show();
                } else {
                //   console.log(x[0].getElementsByTagName("dc:identifier")[0].childNodes[0].nodeValue);
                //   console.log(x[0].getElementsByTagName("dc:format")[0].childNodes[0].nodeValue);
                    for (i = 0; i < x.length; i++) {
                        var stt = i + 1;
                        var identifier = x[i].getElementsByTagName("identifier")[0].childNodes[0].nodeValue;
                        table += "<tr><td>" +
                                stt +
                                "</td><td>" +
                                "<img id='"+ identifier + "' class='myImg' onclick='openImg(this)' alt='" + identifier + "' src='http://localhost:8084/sis/VNSC/wcs/image/" +
                        
                                identifier + ".jpg'/>" +
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
                   $("#demo").html(table);
                }
            }
            
            // Get the modal
            var modal = document.getElementById('myModal');

            // Get the image and insert it inside the modal - use its "alt" text as a caption
            var modalImg = document.getElementById("img01");
            var captionText = document.getElementById("caption");
            
//            img.onclick = function(){
//                modal.style.display = "block";
//                modalImg.src = this.src;
//                modalImg.alt = this.alt;
//                captionText.innerHTML = this.alt;
//            }
            
            function openImg(img) {
                console.log(img.src);
                modal.style.display = "block";
                modalImg.src = img.src;
                modalImg.alt = img.alt;
                captionText.innerHTML = img.alt;
            }

            // Get the <span> element that closes the modal
            var span = document.getElementsByClassName("close-img")[0];

            // When the user clicks on <span> (x), close the modal
            span.onclick = function() { 
              modal.style.display = "none";
            }

        </script>

    </body>
</html>
