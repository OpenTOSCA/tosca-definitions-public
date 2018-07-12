<%@ page import="org.opentosca.ConnectionDetails" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no"/>
    <link rel="stylesheet"
          href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-alpha.6/css/bootstrap.min.css"/>
    <style>
        body {
            min-height: 75rem;
            padding-top: 4.5rem;
        }
    </style>
</head>
<body>
<nav class="navbar navbar-inverse bg-inverse fixed-top">
    <a class="navbar-brand" href="#">Shop</a>
</nav>
<div class="container">
    <c:choose>
        <c:when test="<%=ConnectionDetails.getInstance().getConnectionDetails().isEmpty()%>">
            <div class="jumbotron">
                <p class="lead">Currently there are no connections.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="jumbotron">
                <p class="lead">Currently following connections can be used:</p>
            </div>
            <table class="table">
                <thead>
                <tr>
                    <th>#</th>
                    <th>Value</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="item"
                           items="<%=ConnectionDetails.getInstance().getConnectionDetails()%>"
                           varStatus="loop">
                    <tr>
                        <td><c:out value="${loop.count}"/></td>
                        <td><c:out value="${item.toString()}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>
</body>
</html>
