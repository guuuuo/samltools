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

<%@ page import="util.RequestUtil" %>
<%@ page import="java.lang.String" %>
<%@ page import="java.net.URLEncoder" %>

<html>
<head>
  <meta http-equiv="content-type" content="text/html; charset=UTF-8">
  <link href="global/style.css" type="text/css" rel="stylesheet"/>
  <title>SAML-based Single Sign-On Service for Google Apps for Your Domain - Test Tool</title>
</head>
<script language="JavaScript">
  function submit_now(s,r) {
    document.acsForm.SAMLResponse.value=s;
    document.acsForm.RelayState.value=r;
    document.acsForm.submit();
  }
</script>
<body>
  <h1>PARTNER - Identity Provider</h1>
  <%
    String samlRequest = request.getParameter("SAMLRequest");
    String domainName = "psosamldemo.net";
    String username = request.getParameter("username");
    if (username == null) {
      username = "demouser";
    }

    //If SAML parameters still null, then authnRequest has not yet been
    //received by the Identity Provider, so user should not be logged in.
    if (samlRequest == null) {
  %>
 
      <p><div style="padding:0px 8px;border:solid 1px #000;background:#ddd">
      <p><center><img src="global/warning.gif"></center>
      <p><b>Note: The user cannot be authenticated, and a SAML response cannot be 
      sent, until a SAML request is received from the service provider. </b></p>
      </div>
  <%
    } else {	
		
      String error = (String) request.getAttribute("error");
      if (error != null) {
      %>
        <p><font color="red"><b><%= error %></b></font><p>
      <%
      }
      String issueInstant = (String) request.getAttribute("issueInstant");
      String providerName = (String) request.getAttribute("providerName");
      String acsURL = (String) request.getAttribute("acsURL");
      String relayState = (String) request.getAttribute("relayStateURL");
      %>
      <form name="IdentityProviderForm" action="ProcessResponseServlet" method="post">
        <input type="hidden" name="SAMLRequest" value="<%=samlRequest%>"/>
        <input type="hidden" name="RelayState" value="<%=RequestUtil.htmlEncode(relayState)%>"/>
        <input type="hidden" name="returnPage" value="identity_provider.jsp">
        <input type="hidden" name="samlAction" value="Generate SAML Response">

        <p><div style="padding:6px 0px;border-top:solid 1px #3366cc;border-bottom:solid 1px #3366cc">
          <b>Step 4: Partner Handles SAML Request, Authenticates User</b>
        </div>
        <p>The following values have been parsed from the SAML request:</p>
        <p>
        <ul>
          <li><b>Issue Instant</b> - <%=issueInstant%></li>
          <li><b>Provider Name</b> - <%=providerName%></li>
          <li><b>ACS URL</b> - <%=acsURL%></li>
        </ul>
        <p>
          <b>Note:</b> These are all values that you will receive from the 
          service provider in a SAML transaction.
        </p>
        <p><b>User Login Details</b></p> 
        <p>During this step, you also authenticate the user. The reference code 
        is designed to log a user into the account <b>demouser@psosamldemo.net
        </b>. However, the reference implementation does not actually 
        authenticate the user; it assumes that the authentication is successful. 
        You will need to modify the reference code to call your internal 
        mechanism for authenticating users.</p>

        <!-- Stage II: Display a Username and Password Field -->
        <!-- Remove the comments around the following four lines in Stage II -->
        <!--
          <blockquote>
            <p>Username: <input type="text" name="username" value=""/></p>
            <p>Password: <input type="password" name="password" value=""/></p>
            <p>
              <b>Note:</b> The submit buttons in step 5 submit the username and
              password to the ProcessResponseServlet.
            </p>
          </blockquote>
	    -->

        <!-- Stage II: Hide default username values -->
        <!-- Comment out the following three lines in Stage II -->
        <blockquote>
          <p>Username: <%=username%>@<%=domainName%>
          <p>Password: ******
        </blockquote>

        <p><div style="padding:6px 0px;border-top:solid 1px #3366cc;border-bottom:solid 1px #3366cc">
          <b>Step 5: Partner Generates SAML Response</b>
        </div>
        <p>In this step, you can click the <b>Generate SAML Response</b> button, 
        prompting the reference implementation to generate a SAML response 
        indicating that the user (demouser@psosamldemo.net) is authorized to 
        reach the Gmail service. When you click the <b>Generate SAML 
        Response</b> button, you will execute the ProcessResponseServlet's 
        <b>doPost</b> method.</p>
        <p>
        <center>
	      <input type="submit" name="samlButton" value="Generate SAML Response">
	    </center>
        <p><br>
      </form>
      <% 
      String samlResponse = (String) request.getAttribute("samlResponse");
      if (samlResponse != null) {
        if (username != null) {
      %>
          <%-- This is a hidden form that POSTs the SAML response to the ACS.--%>
          <form name="acsForm" action="<%=acsURL%>" method="post" target="_blank">
            <div style="display: none">
            <textarea rows=10 cols=80 name="SAMLResponse"><%=samlResponse%> </textarea>
            <textarea rows=10 cols=80 name="RelayState"><%=RequestUtil.htmlEncode(relayState)%></textarea>
            </div>
          </form>
      <%
	    } else {
      %> 
          <p><span style="font-weight:bold;color:red">
            You must enter a valid username and password to log in.
          </span></p>
      <%
	    }
      %>
        <span id="samlResponseDisplay" style="display:inline">
        <b> Generated and Signed SAML Response </b>
        <p><div class="codediv"><%=RequestUtil.htmlEncode(samlResponse)%></div>
        <p>The SAML response contains the following variables:</p>
        <p>
        <ul>
          <li>
	        <p>
	          <b>RESPONSE_ID</b> - A 160-bit string containing a set of 
	          randomly generated characters. The code calls the 
	          <b>Util.createID()</b> method to generate this value.
	        </p>
	      </li>
          <li>
            <p>
              <b>ISSUE_INSTANT</b> - A timestamp indicating the date and time 
              that the SAML response was generated. The code calls the 
              <b>Util.getDateAndTime()</b> method to generate this value.
            </p>
          </li>
          <li>
            <p>
              <b>ASSERTION_ID</b> - A 160-bit string containing a set of 
              randomly generated characters. The code calls the 
              <b>Util.createID()</b> method to generate this value.
            </p>
          </li>
          <li>
            <p>
              <b>USERNAME_STRING</b> - The username for the authenticated user. 
              Modify the <b>ProcessResponseServlet.login()</b> method to return 
              the correct value.
            </p>
          </li>
          <li>
            <p>
              <b>NOT_BEFORE</b> - A timestamp identifying the date and time 
              before which the SAML response is deemed invalid. The code sets 
              this value to the <b>IssueInstant</b> value from the SAML request.
            </p>
          </li>
          <li>
            <p>
              <b>NOT_ON_OR_AFTER</b> - A timestamp identifying the date and time
              after which the SAML response is deemed invalid.
            </p>
          </li>
          <li>
            <p>
              <b>AUTHN_INSTANT</b> - A timestamp indicating the date and time 
              that you authenticated the user.
            </p>
          </li>
        </ul>
        <p>
        <center>
          <input type="button" 
                 value="Submit SAML Response" 
                 onclick="javascript:document.acsForm.submit()">
        </center>
        </span>
    <%
      }
    }
  %>
</body>
</html>
