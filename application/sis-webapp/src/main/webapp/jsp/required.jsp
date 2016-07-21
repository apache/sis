<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>Welcome!</title>
</head>
<style>
    table {
        border-collapse: collapse;
    }

    table, th, td {
        border: 1px solid black;
    }
</style>
<body>
  <h1>Welcome </h1>
  <table>
      <thead>
          <td>Field</td>
          <td>Detail</td>
      </thead>
      <tr>
          <td>ID</td>
          <td>${it.getId()}</td>
      </tr>
      <tr>
          <td></td>
          <td>${it.getId()}</td>
      </tr>
      <tr>
          <td>Identifier</td>
          <td>${it.getIdentifier()}</td>
      </tr>
      <tr>
          <td>Title</td>
          <td>${it.getTitle()}</td>
      </tr>
      <tr>
          <td>Type</td>
          <td>${it.getType()}</td>
      </tr>
      <tr>
          <td>Format</td>
          <td>${it.getFormat()}</td>
      </tr>
      
      <tr>
          <td>Modified</td>
          <td>${it.getModified()}</td>
      </tr>
</body>
</html>