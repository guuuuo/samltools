<!--
Copyright (C) 2006 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
-->

<%@page import="java.lang.String"%>
<%@page import="java.net.URLEncoder"%>

<html>
<head>
  <meta http-equiv="content-type" content="text/html; charset=UTF-8">
  <link href="global/style.css" type="text/css" rel="stylesheet"/>
  <title>SAML-based Single Sign-On Service for Google Apps for Your Domain - Test Tool</title>
</head>
<body>
  <img alt="Google" src="global/logo.gif" border="0" height="59" width="143"/>
  <span class="t">
  SAML-based Single Sign-On for Google Apps for Your Domain - Test Tool
  </span>
  <p><div style="padding:6px;border:solid 1px #00723d;background:#ddf8cc"><b>Note:</b> Please refer to the reference implementation documentation for recommended guidelines on installing this reference implementation and adapting its code to create your own SAML-based SSO service. Several sections of this page refer to that documentation in an effort to help you build your SSO application.</div></p>
  <h1>Pre-Transaction Details</h1>
  <p>This window contains information about actions that occur before the SAML-based SSO authentication process begins. The application assumes a user with the user account <b>demouser@psosamldemo.net</b> is trying to log into a Google-hosted version of the Gmail application.</p>

  <p>When you install this application, it will send SAML authentication requests to the <b>ProcessResponseServlet</b>, which is included in the sample code package. It will also use the DSA public and private keys provided with the sample code package to digitally sign SAML responses.</p>
  <div style="padding:0px 20px">
  <p><div style="padding:6px 0px;border-top:solid 1px #3366cc;border-bottom:solid 1px #3366cc"><b>Step 1: User tries to reach hosted application</b></div></p>
  <p>The SAML authentication process begins when a user, who has not already been authenticated, tries to reach the URL for a hosted Google service. In this application, a user is trying to reach the URL <b>http://mail.google.com/a/psosamldemo.net</b>.</p>
  </div>

  <%
    String error = (String)request.getAttribute("keyError");
    if (error != null) {
  %>
      <font color="red"><b><%= error %>. File not updated.</b></font><p>
  <%
    }
  %>		
  <table width="100%" height="100%">
    <tr>
      <td>
        <iframe name="service_provider" src="./service_provider.jsp"
          width="100%" height="100%" frameborder="1"></iframe>
      </td>
      <td>
        <iframe name="identity_provider" src="./ProcessResponseServlet"
          width="100%" height="100%" frameborder="1"></iframe>
      </td>
    </tr>
  </table>
</body>
</html>
