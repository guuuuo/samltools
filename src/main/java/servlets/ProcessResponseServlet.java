/*
 * Copyright (C) 2006 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package servlets;

import org.apache.commons.codec.binary.Base64;
import org.jdom.Document;

import util.SamlException;
import util.Util;
import util.XmlDigitalSigner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet, part of the SAML-based Single Sign-On Reference Tool, takes in
 * a SAML AuthnRequest and verifies the user login credentials. Upon succesful
 * user login, it generates and signs the corresponding SAML Response, which is
 * then redirected to the specified Assertion Consumer Service.
 * 
 */
public class ProcessResponseServlet extends HttpServlet {

  private final String samlResponseTemplateFile = "SamlResponseTemplate.xml";
  private static final String domainName = "psosamldemo.net";

  /*
   * The login method should either return a null string, if the user is not
   * successfully authenticated, or the user's username if the user is
   * successfully authenticated.
   */
  private String login(String username, String password) {
    // Stage II: Update this method to call your authentication mechanism.
    // Return username for successful authentication. Return null string
    // for failed authentication.
    return "demouser";
  }

  /*
   * Retrieves the AuthnRequest from the encoded and compressed String extracted
   * from the URL. The AuthnRequest XML is retrieved in the following order: <p>
   * 1. URL decode <br> 2. Base64 decode <br> 3. Inflate <br> Returns the String
   * format of the AuthnRequest XML.
   */
  private String decodeAuthnRequestXML(String encodedRequestXmlString)
      throws SamlException {
    try {
      // URL decode
      // No need to URL decode: auto decoded by request.getParameter() method

      // Base64 decode
      Base64 base64Decoder = new Base64();
      byte[] xmlBytes = encodedRequestXmlString.getBytes("UTF-8");
      byte[] base64DecodedByteArray = base64Decoder.decode(xmlBytes);

      //Uncompress the AuthnRequest data
      //First attempt to unzip the byte array according to DEFLATE (rfc 1951)
      try {

        Inflater inflater = new Inflater(true);
        inflater.setInput(base64DecodedByteArray);
        // since we are decompressing, it's impossible to know how much space we
        // might need; hopefully this number is suitably big
        byte[] xmlMessageBytes = new byte[5000];
        int resultLength = inflater.inflate(xmlMessageBytes);

        if (!inflater.finished()) {
          throw new RuntimeException("didn't allocate enough space to hold "
            + "decompressed data");
        }

        inflater.end();      
        return new String(xmlMessageBytes, 0, resultLength, "UTF-8");
                   
      } catch (DataFormatException e) {

        // if DEFLATE fails, then attempt to unzip the byte array according to
        // zlib (rfc 1950)      
        ByteArrayInputStream bais = new ByteArrayInputStream(
          base64DecodedByteArray);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InflaterInputStream iis = new InflaterInputStream(bais);
        byte[] buf = new byte[1024];
        int count = iis.read(buf);
        while (count != -1) {
          baos.write(buf, 0, count);
          count = iis.read(buf);
        }
        iis.close();
        return new String(baos.toByteArray());      
      }      
      
    } catch (UnsupportedEncodingException e) {
      throw new SamlException("Error decoding AuthnRequest: " +
            "Check decoding scheme - " + e.getMessage());
    } catch (IOException e) {
      throw new SamlException("Error decoding AuthnRequest: " +
            "Check decoding scheme - " + e.getMessage());
    }
  }

  /*
   * Creates a DOM document from the specified AuthnRequest xmlString and
   * extracts the value under the "AssertionConsumerServiceURL" attribute
   */
  private String[] getRequestAttributes(String xmlString) throws SamlException {
      Document doc = Util.createJdomDoc(xmlString);
      if (doc != null) {
        String[] samlRequestAttributes = new String[4];
        samlRequestAttributes[0] = doc.getRootElement().getAttributeValue(
          "IssueInstant");
        samlRequestAttributes[1] = doc.getRootElement().getAttributeValue(
          "ProviderName");
        samlRequestAttributes[2] = doc.getRootElement().getAttributeValue(
          "AssertionConsumerServiceURL");
        samlRequestAttributes[3] = doc.getRootElement().getAttributeValue(
          "ID");
        return samlRequestAttributes;
      } else {
        throw new SamlException("Error parsing AuthnRequest XML: Null document");
      }
  }

  /*
   * Generates a SAML response XML by replacing the specified username on the
   * SAML response template file. Returns the String format of the XML file.
   */
  private String createSamlResponse(String authenticatedUser, String notBefore,
      String notOnOrAfter, String requestId, String acsURL)
      throws SamlException {
    String filepath = getServletContext().getRealPath(
      "templates/" + samlResponseTemplateFile);
    String samlResponse = Util.readFileContents(filepath);
    samlResponse = samlResponse.replace("<USERNAME_STRING>", authenticatedUser);
    samlResponse = samlResponse.replace("<RESPONSE_ID>", Util.createID());
    samlResponse = samlResponse.replace("<ISSUE_INSTANT>", Util
      .getDateAndTime());
    samlResponse = samlResponse.replace("<AUTHN_INSTANT>", Util
      .getDateAndTime());
    samlResponse = samlResponse.replace("<NOT_BEFORE>", notBefore);
    samlResponse = samlResponse.replace("<NOT_ON_OR_AFTER>", notOnOrAfter);
    samlResponse = samlResponse.replace("<ASSERTION_ID>", Util.createID());
    samlResponse = samlResponse.replace("<REQUEST_ID>", requestId);
    samlResponse = samlResponse.replace("<ACS_URL>", acsURL);
    return samlResponse;

  }

  /*
   * Signs the SAML response XML with the specified private key, and embeds with
   * public key. Uses helper class XmlDigitalSigner to digitally sign the XML.
   */
  private String signResponse(String response, DSAPublicKey publicKey,
      DSAPrivateKey privateKey) throws SamlException {
      return (XmlDigitalSigner.signXML(response, publicKey, privateKey));
  }

  /*
   * Checks if the specified samlDate is formatted as per the SAML 2.0
   * specifications, namely YYYY-MM-DDTHH:MM:SSZ.
   */
  private boolean validSamlDateFormat(String samlDate) {
    if (samlDate == null) {
      return false;
    }
    int indexT = samlDate.indexOf("T");
    int indexZ = samlDate.indexOf("Z");
    if (indexT != 10 || indexZ != 19) {
      return false;
    }
    String dateString = samlDate.substring(0, indexT);
    String timeString = samlDate.substring(indexT + 1, indexZ);
    SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    ParsePosition pos = new ParsePosition(0);
    Date parsedDate = dayFormat.parse(dateString, pos);
    pos = new ParsePosition(0);
    Date parsedTime = timeFormat.parse(timeString, pos);
    if (parsedDate == null || parsedTime == null) {
      return false;
    }
    return true;
  }

  /**
   * The doGet method handles HTTP GET requests sent to the
   * ProcessResponseServlet. This method's sole purpose is to interact with the
   * user interface that allows you to walk through the steps of the reference
   * implementation. In a production environment, Google's would send SAML
   * requests using HTTP redirect.
   * 
   * This method receives an HTTP GET request and then forwards that request on
   * to the identity_provider.jsp file, which is included in Google's SAML
   * reference package. If this method receives a SAML request Read in SAML
   * AuthnRequest parameters from request and generate signed SAML response to
   * post to the Assertion Consumer Service.
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String SAMLRequest = request.getParameter("SAMLRequest");
    String relayStateURL = request.getParameter("RelayState");
    if (SAMLRequest != null) {
      try {
        String requestXmlString = decodeAuthnRequestXML(SAMLRequest);
        String[] samlRequestAttributes = getRequestAttributes(requestXmlString);
        String issueInstant = samlRequestAttributes[0];
        String providerName = samlRequestAttributes[1];
        String acsURL = samlRequestAttributes[2];
        String requestId = samlRequestAttributes[3];
        request.setAttribute("issueInstant", issueInstant);
        request.setAttribute("providerName", providerName);
        request.setAttribute("acsURL", acsURL);
        request.setAttribute("requestId", requestId);
        request.setAttribute("relayStateURL", relayStateURL);
      } catch (SamlException e) {
        request.setAttribute("error", e.getMessage());
      }
    }

    String returnPage = "./identity_provider.jsp";
    request.getRequestDispatcher(returnPage).include(request, response);
  }

  /**
   * The doPost method handles HTTP POST requests sent to the
   * ProcessResponseServlet. It then works to generate a SAML response with the
   * received parameter and then post the response to the Assertion Consumer
   * Service.
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String samlAction = request.getParameter("samlAction");
    String SAMLRequest = request.getParameter("SAMLRequest");
    String returnPage = request.getParameter("returnPage");
    String username = request.getParameter("username");
    String password = request.getParameter("password");
    String relayStateURL = request.getParameter("RelayState");

    boolean continueLogin = true;

    if (SAMLRequest == null || SAMLRequest.equals("null")) {
      continueLogin = false;
      request.setAttribute("error", "ERROR: Unspecified SAML parameters.");
    } else if (samlAction == null) {
      continueLogin = false;
      request.setAttribute("error", "ERROR: Invalid SAML action.");
    } else if (returnPage != null) {
      try {
        // Parse the SAML request and extract the ACS URL and provider name
        // Extract the Assertion Consumer Service URL from AuthnRequest
        String requestXmlString = decodeAuthnRequestXML(SAMLRequest);
        String[] samlRequestAttributes = getRequestAttributes(requestXmlString);
        String issueInstant = samlRequestAttributes[0];
        String providerName = samlRequestAttributes[1];
        String acsURL = samlRequestAttributes[2];
        String requestId = samlRequestAttributes[3];

        /*
         * Stage II: Whereas Stage I uses a hardcoded username
         * (demouser@psosamldemo.net), in Stage II you need to modify the code
         * to call your user authentication application.
         */

        username = login(username, password);

        // The following lines of code set variables used in the UI.
        request.setAttribute("issueInstant", issueInstant);
        request.setAttribute("providerName", providerName);
        request.setAttribute("acsURL", acsURL);
        request.setAttribute("requestId", requestId);
        request.setAttribute("domainName", domainName);
        request.setAttribute("username", username);
        request.setAttribute("relayStateURL", relayStateURL);

        if (username == null) {
          request.setAttribute("error", "Login Failed: Invalid user.");
        } else {
          // Acquire public and private DSA keys

          /*
           * Stage III: Update the DSA filenames to identify the locations of
           * the DSA/RSA keys that digitally sign SAML responses for your
           * domain. The keys included in the reference implementation sign SAML
           * responses for the psosamldemo.net domain.
           */
          String publicKeyFilePath = getServletContext().getRealPath(
            "./keys/DSAPublicKey01.key");
          String privateKeyFilePath = getServletContext().getRealPath(
            "./keys/DSAPrivateKey01.key");

          DSAPublicKey publicKey = (DSAPublicKey) Util.getPublicKey(
            publicKeyFilePath, "DSA");
          DSAPrivateKey privateKey = (DSAPrivateKey) Util.getPrivateKey(
            privateKeyFilePath, "DSA");

          // Check for valid parameter values for SAML response

          // First, verify that the NotBefore and NotOnOrAfter values are valid
          String notBefore = Util.getNotBeforeDateAndTime();
          String notOnOrAfter = Util.getNotOnOrAfterDateAndTime();
          request.setAttribute("notBefore", notBefore);
          request.setAttribute("notOnOrAfter", notOnOrAfter);

          if (!validSamlDateFormat(issueInstant)) {
            continueLogin = false;
            request.setAttribute("error",
              "ERROR: Invalid NotBefore date specified - " + notBefore);
          } else if (!validSamlDateFormat(notOnOrAfter)) {
            continueLogin = false;
            request.setAttribute("notOnOrAfter", notOnOrAfter);
            request.setAttribute("error",
              "ERROR: Invalid NotOnOrAfter date specified - " + notOnOrAfter);
          } 

          // Sign XML containing user name with specified keys
          if (continueLogin) {
            // Generate SAML response contaning specified user name
            String responseXmlString = createSamlResponse(username, notBefore,
                notOnOrAfter, requestId, acsURL);

            // Sign the SAML response XML
            String signedSamlResponse = signResponse(responseXmlString,
              publicKey, privateKey);
            request.setAttribute("samlResponse", signedSamlResponse);
          }
        }
      } catch (SamlException e) {
        request.setAttribute("error", e.getMessage());
      }
    }
    // Forward SAML response to ACS
    request.getRequestDispatcher(returnPage).include(request, response);
  }
}
