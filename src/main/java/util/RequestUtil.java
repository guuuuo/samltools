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

package util;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.zip.DeflaterOutputStream;

/**
 * This utility class is used across the various servlets that make up the
 * SAML-based Single Sign-On Reference Tool. It includes various helper methods
 * that are used for the SAML transactions.
 * 
 */
public class RequestUtil {
  /**
   * Generates an encoded and compressed String from the specified XML-formatted
   * String. The String is encoded in the following order:
   * <p>
   * 1. URL encode <br>
   * 2. Base64 encode <br>
   * 3. Deflate <br>
   * 
   * @param xmlString XML-formatted String that is to be encoded
   * @return String containing the encoded contents of the specified XML String
   */
  public static String encodeMessage(String xmlString) throws IOException,
      UnsupportedEncodingException {
    // first DEFLATE compress the document (saml-bindings-2.0,
    // section 3.4.4.1)
    byte[] xmlBytes = xmlString.getBytes("UTF-8");
    ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
    DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(
      byteOutputStream);
    deflaterOutputStream.write(xmlBytes, 0, xmlBytes.length);
    deflaterOutputStream.close();

    // next, base64 encode it
    Base64 base64Encoder = new Base64();
    byte[] base64EncodedByteArray = base64Encoder.encode(byteOutputStream
      .toByteArray());
    String base64EncodedMessage = new String(base64EncodedByteArray);

    // finally, URL encode it
    String urlEncodedMessage = URLEncoder.encode(base64EncodedMessage, "UTF-8");

    return urlEncodedMessage;
  }

  /**
   * Returns HTML encoded version of the specified String s.
   * 
   * @param s String to be HTML encoded
   * @return HTML encoded String
   */
  public static String htmlEncode(String s) {
    StringBuffer encodedString = new StringBuffer("");
    char[] chars = s.toCharArray();
    for (char c : chars) {
      if (c == '<') {
        encodedString.append("&lt;");
      } else if (c == '>') {
        encodedString.append("&gt;");
      } else if (c == '\'') {
        encodedString.append("&apos;");
      } else if (c == '"') {
        encodedString.append("&quot;");
      } else if (c == '&') {
        encodedString.append("&amp;");
      } else {
        encodedString.append(c);
      }
    }
    return encodedString.toString();
  }

}
