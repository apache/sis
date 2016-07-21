<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Welcome to VNSC web project</title>
        <link  rel="stylesheet" href="css/myweb.css" type="text/css">
         <meta name="viewport" content="width=devide-width,initial-scale=1.0">
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" integrity="sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7" crossorigin="anonymous">
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap-theme.min.css" integrity="sha384-fLW2N01lMqjakBkx3l/M9EahuwpSfeNvV63J5ezn3uZzapT0u7EYsXMjQV+0En5r" crossorigin="anonymous">
        <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js" integrity="sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS" crossorigin="anonymous"></script>
         <script src="javascript/main.js" /></script>
        <meta name="viewport" content="width=devide-width,initial-scale=1.0"
    </head>

    <body background="image/bg.jpg" class="mainBody" >
        <header class="mainheader">
        <div>
            <div class="links-top row"> 
                <span class="col-md-9"></span>
                <span class="col-md-1 link-box"><a class="link-text" href="#">Home</a></span>
                <span class="col-md-1 link-box"><a class="link-text" href="#">Q&A</a></span>
                <span class="col-md-1 link-box"><a class="link-text" href="#">About</a></span>
            </div>
            <div class="row background-title">
                <div class="col-md-4"><img style="width:80%" src="image/image.png"></div>
                <div></div>
                <div class="col-md-7 title"><marquee direction="right"> Trung Tâm Vệ Tinh Quốc Gia</marquee></div>
            </div>
            <div class="links-bottom"></div>
        </div>
        </header>

    <div class="mainContent" >
        <div  class="content">
            <article class="left-content">
                <div>
                    <div  class="post-info"><label>Search Result</label></div>
                    <form class="search-form" action="jsp/geotiff.jsp" method="POST" >
                        <div class="row content-search">
                            <div class="col-md-3"><label>Name</label></div>
                            <div class="col-md-9"><input class="form-control" type="text" name="identifier"/></div>
                        </div>
                        <div class="row content-search">
                            <div class="col-md-3"><label>Format</label></div>
                            <div class="col-md-3"><label class="checkbox-inline"><input type="checkbox" value="GEOTIFF" name="format">Geotiff</label></div>
                            <div class="col-md-3"><label class="checkbox-inline"><input type="checkbox" value="MOD" name="format">Modis</label></div>              
                        </div>
                        
                    <label style="font-size:20px;">Date</label>
                    <div class="row content-search">
                        <div class="col-md-3"><label>From</label></div>
                        <div class="col-md-9"><input class="form-control" type="date" name="date1"/></div>

                    </div>
                    <div class="row content-search">
                        <div class="col-md-3"><label>To</label></div>
                        <div class="col-md-9"><input class="form-control" type="date" name="date2"/></div>
                    
                    </div>
                        
                    <label style="font-size:20px;">Coordinate</label>
                    <div class="row content-search">
                        <div class="col-md-3"><label>BoundingBox</label></div>
                        <div class="col-md-9"><input class="form-control" id="divId" name="divid" ></div>
                    </div>
                        <div class="Button">
                            <input type="submit" />
                        </div>
                    </form>
                    
                </header>
            </article>
        </div>
    </div>

    <aside class="top-sidebar">
        <article>
            <div id="map"></div>
        </article>
    </aside>
    <footer class="mainfooter">
        <p>CopyRight &copy; VNSC.com</p>
    </footer>

     
   
    <script async defer
        src="https://maps.googleapis.com/maps/api/js?key=AIzaSyBP1jtGKFo80FQZ9BC1nOD5c0c0Mhk4Ivo&callback=initMap">
    </script>
    

</body>
</html>