/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.sip.xmlrpc;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;

import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.openmeetings.OpenmeetingsVariables;
import org.apache.openmeetings.data.basic.dao.ConfigurationDaoImpl;
import org.apache.openmeetings.persistence.beans.basic.Configuration;
import org.apache.openmeetings.persistence.beans.sip.OpenXGReturnObject;
import org.apache.openmeetings.persistence.beans.user.UserSipData;
import org.apache.openmeetings.sip.xmlrpc.custom.OpenXGCustomXMLMarshall;
import org.apache.openmeetings.sip.xmlrpc.custom.dao.OpenXGReturnObjectDaoImpl;
import org.apache.openmeetings.utils.crypt.MD5;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("deprecation")
public class OpenXGHttpClient {

	private static final Logger log = Red5LoggerFactory.getLogger(
			OpenXGHttpClient.class, OpenmeetingsVariables.webAppRootKey);
	@Autowired
	private ConfigurationDaoImpl configurationDaoImpl;
	@Autowired
	private OpenXGReturnObjectDaoImpl openXGReturnObjectDao;

	public String digest_calculate(Object[] params) throws Exception {
		String stringToMd5 = "";

		for (int i = 0; i < params.length; i++) {
			stringToMd5 += params[i];
		}

		return MD5.do_checksum(stringToMd5);

	}

	public void openSIPgUserCreateTest() {
		try {

			String first_name = "Matti";
			String middle_i = "X";
			String last_name = "Virtanen";
			String email = "test@test.de";
			String password = "password";

			this.openSIPgUserCreateUser(first_name,
					middle_i, last_name, email, password, "test");

		} catch (Exception err) {

			log.error("[openSIPgUserCreateTest]", err);

		}
	}

	/**
	 * 
	 * Creates a SIP User using the RPC Methods of openXG
	 * 
	 * @param first_name
	 * @param middle_i
	 * @param last_name
	 * @param email
	 * @param password
	 * @return
	 */
	public UserSipData openSIPgUserCreateUser(String first_name,
			String middle_i, String last_name, String email, String login,
			String password) {
		try {

			// Check if the OpenXG Gateway is enabled in general
			Configuration sip_openxg_enable = configurationDaoImpl
					.getConfKey(
					"sip.openxg.enable");

			if (sip_openxg_enable == null
					|| !sip_openxg_enable.getConf_value().equals("yes")) {
				log.debug("SIP is disabled");
				return null;
			}

			// client_id and client_secret
			Configuration openxg_client_id = configurationDaoImpl
					.getConfKey(
					"openxg.client.id");
			Configuration openxg_client_secret = configurationDaoImpl
					.getConfKey(
					"openxg.client.secret");
			if (openxg_client_id == null || openxg_client_secret == null) {
				throw new Exception(
						"openxg.client.id or openxg.client.secret missing in Configuration table");
			}
			String client_id = openxg_client_id.getConf_value();
			String client_secret = openxg_client_secret.getConf_value();

			// domain
			Configuration openxg_client_domain = configurationDaoImpl
					.getConfKey(
					"openxg.client.domain");
			if (openxg_client_domain == null) {
				throw new Exception(
						"openxg.client.domain missing in Configuration table");
			}
			String domain = openxg_client_domain.getConf_value();

			// openxg_community_code
			Configuration openxg_community_code = configurationDaoImpl
					.getConfKey(
					"openxg.community.code");
			if (openxg_community_code == null) {
				throw new Exception(
						"openxg.community.code missing in Configuration table");
			}
			String community_code = openxg_community_code.getConf_value();

			// language_code
			Configuration openxg_language_code = configurationDaoImpl
					.getConfKey(
					"openxg.language.code");
			if (openxg_language_code == null) {
				throw new Exception(
						"openxg.language.code missing in Configuration table");
			}
			String language_code = openxg_language_code.getConf_value();

			// adminid
			Configuration openxg_adminid = configurationDaoImpl
					.getConfKey(
					"openxg.adminid");
			if (openxg_adminid == null) {
				throw new Exception(
						"openxg.adminid missing in Configuration table");
			}
			String adminid = openxg_adminid.getConf_value();

			// sip_language_phonecode, for example +358, is important as port of
			// the
			// number/login for the SIP-User via the applet
			Configuration sip_language_phonecode = configurationDaoImpl
					.getConfKey(
					"sip.language.phonecode");

			if (sip_language_phonecode == null) {
				throw new Exception(
						"No sip.language.phonecode set in Configuration");
			}

			// Calculate the number in national format
			Configuration sip_phonerange_start = configurationDaoImpl
					.getConfKey(
					"sip.phonerange.start");
			Configuration sip_phonerange = configurationDaoImpl
					.getConfKey(
					"sip.phonerange");
			Configuration sip_phonerange_currentindex = configurationDaoImpl
					.getConfKey("sip.phonerange.currentindex");
			if (sip_phonerange_start == null || sip_phonerange == null
					|| sip_phonerange_currentindex == null) {
				throw new Exception(
						"sip.phonerange.start, sip.phonerange or sip.phonerange.currentindex missing in Configuration table");
			}

			// Long sipPhoneRangeStart =
			// Long.parseLong(sip_phonerange_start.getConf_value());
			// Long sipPhoneRange =
			// Long.parseLong(sip_phonerange.getConf_value());
			// Long sipPhoneRangeCurrentIndex =
			// Long.parseLong(sip_phonerange_currentindex.getConf_value());

			// Not sure anymore if this is really needed, we don't need a number
			// in national format
			// if (sipPhoneRangeCurrentIndex >= sipPhoneRange) {
			// throw new
			// Exception("You have no more numbers, you need to allocate more numbers and alter the Configuration value sip.phonerange");
			// }

			// Long useridAsNumber = sipPhoneRangeStart +
			// sipPhoneRangeCurrentIndex;
			//
			// log.debug("(sip_phonerange_start.getConf_value().length()) "+(sip_phonerange_start.getConf_value().length()+1));
			//
			// //The userid == the Number allocated in National Format!
			// //The userid is calculated on the number of available numbers in
			// the range
			// String userid =
			// String.format("%0"+(sip_phonerange_start.getConf_value().length())+"d",
			// useridAsNumber);

			String userid = login;

			// sipPhoneRangeCurrentIndex++;
			// sip_phonerange_currentindex.setConf_value(""+sipPhoneRangeCurrentIndex);

			// cfgManagement.updateConfig(sip_phonerange_currentindex);

			String digest = this.digest_calculate(new Object[] { client_id,
					userid, domain, first_name, middle_i, last_name, password,
					community_code, language_code, email, adminid,
					client_secret });

			// Get the XML-String representative
			String stringToPost = OpenXGCustomXMLMarshall.getInstance()
					.openSIPgUserCreate(client_id, digest, userid, domain,
							first_name, middle_i, last_name, password,
							community_code, language_code, email, adminid);

			OpenXGReturnObject openXGReturnObject = this
					.openSIPgPost(stringToPost);

			openXGReturnObject.setMethodName("OpenSIPg.UserCreate");

			openXGReturnObjectDao.addOpenXGReturnObject(
					openXGReturnObject);

			log.debug(" Status_code " + openXGReturnObject.getStatus_code());
			log.debug(" Status_string " + openXGReturnObject.getStatus_string());

			if (openXGReturnObject.getStatus_code().equals("200")) {

				// Add User URI
				String digestURI = this.digest_calculate(new Object[] {
						client_id, userid, domain, adminid, client_secret });

				// Get the XML-String representative
				String stringToPostURI = OpenXGCustomXMLMarshall.getInstance()
						.openSIPgURIUserIDAdd(client_id, digestURI, userid,
								domain, adminid);

				OpenXGReturnObject openXGReturnObjectURI = this
						.openSIPgPost(stringToPostURI);

				openXGReturnObjectURI.setMethodName("OpenSIPg.URIUserIDAdd");

				openXGReturnObjectDao.addOpenXGReturnObject(
						openXGReturnObjectURI);

				log.debug(" openXGReturnObjectURI Status Code "
						+ openXGReturnObjectURI.getStatus_code());
				log.debug(" openXGReturnObjectURI Status String "
						+ openXGReturnObjectURI.getStatus_string());

				UserSipData userSipData = new UserSipData();

				// userSipData.setUsername(sip_language_phonecode.getConf_value()
				// + useridAsNumber);
				userSipData.setUsername(userid);
				userSipData.setUserpass(password);
				userSipData.setAuthId(userid);

				return userSipData;

			} else {

				throw new Exception(
						"Could not add SIP User - Gateway response Error Code: "
								+ openXGReturnObject.getStatus_code()
								+ " Message: "
								+ openXGReturnObject.getStatus_string());

			}

		} catch (Exception err) {

			log.error("[openSIPgUserCreateTest]", err);

		}

		return null;
	}

	public OpenXGReturnObject openSIPgCreateConference() {
		try {

			// Check if the OpenXG Gateway is enabled in general
			Configuration sip_openxg_enable = configurationDaoImpl
					.getConfKey(
					"sip.openxg.enable");

			if (sip_openxg_enable == null
					|| !sip_openxg_enable.getConf_value().equals("yes")) {
				log.debug("SIP is disabled");
				return null;
			}

			// client_id and client_secret
			Configuration openxg_client_id = configurationDaoImpl
					.getConfKey(
					"openxg.client.id");
			Configuration openxg_client_secret = configurationDaoImpl
					.getConfKey(
					"openxg.client.secret");
			if (openxg_client_id == null || openxg_client_secret == null) {
				throw new Exception(
						"openxg.client.id or openxg.client.secret missing in Configuration table");
			}
			String client_id = openxg_client_id.getConf_value();
			String client_secret = openxg_client_secret.getConf_value();

			// domain
			Configuration openxg_client_domain = configurationDaoImpl
					.getConfKey(
					"openxg.client.domain");
			if (openxg_client_domain == null) {
				throw new Exception(
						"openxg.client.domain missing in Configuration table");
			}
			String domain = openxg_client_domain.getConf_value();

			// language_code
			Configuration openxg_language_code = configurationDaoImpl
					.getConfKey(
					"openxg.language.code");
			if (openxg_language_code == null) {
				throw new Exception(
						"openxg.language.code missing in Configuration table");
			}
			String language_code = openxg_language_code.getConf_value();

			// adminid
			Configuration openxg_adminid = configurationDaoImpl
					.getConfKey(
					"openxg.adminid");
			if (openxg_adminid == null) {
				throw new Exception(
						"openxg.adminid missing in Configuration table");
			}
			String adminid = openxg_adminid.getConf_value();

			Date d = new Date();
			long starttime = d.getTime() / 1000;

			long endTime = (d.getTime() / 1000) + (60 * 60);

			// "0", "2147483647",

			// permanent conferences
			starttime = 0;
			endTime = 2147483647;

			String digest = this.digest_calculate(new Object[] { client_id,
					"067201101", domain, "" + starttime, "" + endTime,
					language_code, adminid, client_secret });

			Configuration openxg_wrapper_url = configurationDaoImpl
					.getConfKey(
					"openxg.wrapper.url");

			if (openxg_wrapper_url == null) {
				throw new Exception(
						"No openxg.wrapper.url set in Configuration");
			}

			String strURL = openxg_wrapper_url.getConf_value();

			// Prepare HTTP post
			HttpPost post = new HttpPost(strURL);
			post.addHeader("User-Agent", "OpenSIPg XML_RPC Client");

			// Get the XML-String representative
			String stringToPost = OpenXGCustomXMLMarshall.getInstance()
					.openSIPgCreateConference(client_id, digest, "067201101",
							domain, "" + starttime, "" + endTime,
							language_code, adminid);

			OpenXGReturnObject openXGReturnObject = this
					.openSIPgPost(stringToPost);

			openXGReturnObject.setMethodName("OpenSIPg.UserConferenceAdd");

			openXGReturnObjectDao.addOpenXGReturnObject(
					openXGReturnObject);

			return openXGReturnObject;

		} catch (Exception err) {

			log.error("[openSIPgUserCreate]", err);

		}

		return null;
	}

	public OpenXGReturnObject openSIPgPost(String stringToPost) {
		try {

			Configuration openxg_wrapper_url = configurationDaoImpl
					.getConfKey(
					"openxg.wrapper.url");

			if (openxg_wrapper_url == null) {
				throw new Exception(
						"No openxg.wrapper.url set in Configuration");
			}

			String strURL = openxg_wrapper_url.getConf_value();

			// Prepare HTTP post
			HttpPost post = new HttpPost(strURL);
			post.addHeader("User-Agent", "OpenSIPg XML_RPC Client");

			// log.debug(stringToPost);

			AbstractHttpEntity entity = new ByteArrayEntity(
					stringToPost.getBytes(Charset.forName("ISO-8859-1")));

			// Prepare HTTP post
			HttpParams params = post.getParams();
			HttpProtocolParams.setContentCharset(params, "utf-8");
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			post.setParams(params);

			// Request content will be retrieved directly
			// from the input stream
			post.setEntity(entity);

			// Get HTTP client
			HttpClient httpclient = getHttpClient();

			// Execute request
			HttpResponse response = httpclient.execute(post);
			int resCode = response.getStatusLine().getStatusCode();
			
			// Display status code
			log.debug("Response status code: " + response);

			if (resCode == 200) {
	            HttpEntity ent = response.getEntity();
				String responseBody = (ent != null) ? EntityUtils.toString(ent) : "";
				log.debug("parseReturnBody " + responseBody);

				OpenXGReturnObject oIG = this.parseOpenXGReturnBody(ent.getContent());

				log.debug("oIG 1 " + oIG.getStatus_code());
				log.debug("oIG 2 " + oIG.getStatus_string());

				return oIG;

			} else {

				throw new Exception(
						"Could not connect to OpenXG, check the URL for the Configuration");

			}

		} catch (Exception err) {

			log.error("[openSIPgUserCreate]", err);

		}

		return null;
	}

	/**
	 * 
	 * @param inputStream
	 * @return
	 */
	public OpenXGReturnObject parseOpenXGReturnBody(InputStream inputStream) {
		try {

			OpenXGReturnObject openXGReturnObject = new OpenXGReturnObject();

			// log.debug("parseReturnBody "+inputStream);

			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(false);

			// get a builder to create a DOM document
			DocumentBuilder builder = factory.newDocumentBuilder();

			Document document = builder.parse(inputStream);

			NodeList members = document.getElementsByTagName("member");

			// log.debug("members LENGTH "+members.getLength());

			for (int i = 0; i < members.getLength(); i++) {
				Element member = (Element) members.item(i);

				NodeList name = member.getElementsByTagName("name");

				// log.debug("Name "+name.item(0).getNodeName());

				Node nameTextNode = name.item(0).getFirstChild();

				log.debug("getNodeValue " + nameTextNode.getNodeValue());

				if (nameTextNode.getNodeValue().equals("status_code")) {

					NodeList string = member.getElementsByTagName("string");

					// log.debug("Value "+string.item(0).getNodeName());

					Node valueTextNode = string.item(0).getFirstChild();

					// log.debug("Value "+valueTextNode.getNodeValue());

					openXGReturnObject.setStatus_code(valueTextNode
							.getNodeValue());

				} else if (nameTextNode.getNodeValue().equals("status_string")) {

					NodeList string = member.getElementsByTagName("string");

					// log.debug("Value "+string.item(0).getNodeName());

					Node valueTextNode = string.item(0).getFirstChild();

					// log.debug("Value "+valueTextNode.getNodeValue());

					openXGReturnObject.setStatus_string(valueTextNode
							.getNodeValue());

				} else if (nameTextNode.getNodeValue().equals(
						"conference_number")) {

					NodeList string = member.getElementsByTagName("string");

					// log.debug("Value "+string.item(0).getNodeName());

					Node valueTextNode = string.item(0).getFirstChild();

					// log.debug("Value "+valueTextNode.getNodeValue());

					openXGReturnObject.setConferenceNumber(valueTextNode
							.getNodeValue());

				} else if (nameTextNode.getNodeValue().equals("conference_pin")) {

					NodeList string = member.getElementsByTagName("string");

					// log.debug("Value "+string.item(0).getNodeName());

					Node valueTextNode = string.item(0).getFirstChild();

					// log.debug("Value "+valueTextNode.getNodeValue());

					openXGReturnObject.setConferencePin(valueTextNode
							.getNodeValue());

				}

			}

			return openXGReturnObject;

		} catch (Exception err) {

			log.error("[parseOpenXGReturnBody]", err);

		}
		return null;
	}

	public HttpClient getHttpClient() {
	    try {
	        SSLSocketFactory sf = new SSLSocketFactory(SSLContext.getInstance("TLS"), SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

	        HttpParams params = new BasicHttpParams();
	        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

	        SchemeRegistry registry = new SchemeRegistry();
	        registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
	        registry.register(new Scheme("https", 443, sf));

	        ClientConnectionManager ccm = new ThreadSafeClientConnManager(registry);

	        return new DefaultHttpClient(ccm, params);
	    } catch (Exception e) {
	        return new DefaultHttpClient();
	    }
	}
}
