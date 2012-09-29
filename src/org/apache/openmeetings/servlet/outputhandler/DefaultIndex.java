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
package org.apache.openmeetings.servlet.outputhandler;

import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.openmeetings.OpenmeetingsVariables;
import org.apache.openmeetings.data.basic.dao.ConfigurationDaoImpl;
import org.apache.openmeetings.persistence.beans.basic.Configuration;
import org.apache.openmeetings.remote.red5.ScopeApplicationAdapter;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.VelocityViewServlet;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class DefaultIndex extends VelocityViewServlet {
	private static final long serialVersionUID = 3043617619650666432L;
	private static final Logger log = Red5LoggerFactory.getLogger(
			DefaultIndex.class, OpenmeetingsVariables.webAppRootKey);

	private ConfigurationDaoImpl getConfigurationDaoImpl() {
		try {
			if (!ScopeApplicationAdapter.initComplete) {
				return null;
			}
			ApplicationContext context = WebApplicationContextUtils
					.getWebApplicationContext(getServletContext());
			return (ConfigurationDaoImpl) context
					.getBean("configurationDaoImpl");
		} catch (Exception err) {
			log.error("[getConfigurationmanagement]", err);
		}
		return null;
	}

	@Override
	public Template handleRequest(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, Context ctx) {

		try {

			if (getConfigurationDaoImpl() == null) {
				return getVelocityView().getVelocityEngine().getTemplate(
						"booting.vm");
			}

			String template = "sip_template.vm";

			// Enable SIP Template or not
			Configuration SIP_ENABLE = getConfigurationDaoImpl().getConfKey(
					"sip.enable");

			// SIP_REALM
			ctx.put("APP_NAME", getConfigurationDaoImpl().getAppName());

			if (SIP_ENABLE == null || !SIP_ENABLE.getConf_value().equals("yes")) {

				template = "usual_template.vm";

			} else {

				// Set all the Params for the Applet Configuration

				// SIP_REALM
				Configuration SIP_REALM = getConfigurationDaoImpl()
						.getConfKey("sip.realm");
				if (SIP_REALM == null) {
					ctx.put("SIP_REALM", "");
				} else {
					ctx.put("SIP_REALM", SIP_REALM.getConf_value());
				}

				// SIP_PORT
				Configuration SIP_PORT = getConfigurationDaoImpl()
						.getConfKey("sip.port");
				if (SIP_PORT == null) {
					ctx.put("SIP_PORT", "");
				} else {
					ctx.put("SIP_PORT", SIP_PORT.getConf_value());
				}

				// SIP_PROXYNAME
				Configuration SIP_PROXYNAME = getConfigurationDaoImpl()
						.getConfKey("sip.proxyname");
				if (SIP_PROXYNAME == null) {
					ctx.put("SIP_PROXYNAME", "");
				} else {
					ctx.put("SIP_PROXYNAME", SIP_PROXYNAME.getConf_value());
				}

				// SIP_TUNNEL
				Configuration SIP_TUNNEL = getConfigurationDaoImpl()
						.getConfKey("sip.tunnel");
				if (SIP_TUNNEL == null) {
					ctx.put("SIP_TUNNEL", "");
				} else {
					ctx.put("SIP_TUNNEL", SIP_TUNNEL.getConf_value());
				}

				// SIP_CODEBASE
				Configuration SIP_CODEBASE = getConfigurationDaoImpl()
						.getConfKey("sip.codebase");
				if (SIP_CODEBASE == null) {
					ctx.put("SIP_CODEBASE", "");
				} else {
					ctx.put("SIP_CODEBASE", SIP_CODEBASE.getConf_value());
				}

				// SIP_FORCETUNNEL
				Configuration SIP_FORCETUNNEL = getConfigurationDaoImpl()
						.getConfKey("sip.forcetunnel");
				if (SIP_FORCETUNNEL == null) {
					ctx.put("SIP_FORCETUNNEL", "");
				} else {
					ctx.put("SIP_FORCETUNNEL", SIP_FORCETUNNEL.getConf_value());
				}
			}

			// Parse the Param for the SWF URL
			String swf = httpServletRequest.getParameter("swf");
			if (swf == null) {
				ctx.put("SWF_URL", "main.as3.swf10.swf");
			} else {
				ctx.put("SWF_URL", swf);
			}

			String SWF_PARAMS = "";
			String SWF_FLASHVARS = "";

			// Load params from URL and set into wrapper code
			if (httpServletRequest.getParameterMap() != null) {
				for (@SuppressWarnings("unchecked")
				Iterator<String> iter = httpServletRequest.getParameterMap()
						.keySet().iterator(); iter.hasNext();) {
					String paramKey = iter.next();
					SWF_FLASHVARS += paramKey
							+ "="
							+ httpServletRequest.getParameterMap()
									.get(paramKey) + "&amp;";
					SWF_PARAMS += paramKey
							+ "="
							+ httpServletRequest.getParameterMap()
									.get(paramKey) + "&amp;";
				}
			}

			HashMap<String, String> defaultValuesMap = new HashMap<String, String>();

			defaultValuesMap.put("lzt", "swf");
			defaultValuesMap.put("lzproxied", "solo");
			defaultValuesMap.put("lzr", "swf8");
			defaultValuesMap.put("bgcolor", "%23ffffff");
			defaultValuesMap.put("width", "100%25");
			defaultValuesMap.put("height", "100%25");
			// defaultValuesMap.put("__lzurl","main.lzx%3Flzt%3Dswf%26lzproxied%3Dsolo%26lzr%3Dswf8");
			defaultValuesMap.put("__lzminimumversion", "8");
			defaultValuesMap.put("id", "lzapp");

			for (Iterator<String> iter = defaultValuesMap.keySet().iterator(); iter
					.hasNext();) {
				String paramKey = iter.next();
				SWF_PARAMS += paramKey + "=" + defaultValuesMap.get(paramKey);
				SWF_FLASHVARS += paramKey + "="
						+ defaultValuesMap.get(paramKey);
				if (iter.hasNext()) {
					SWF_PARAMS += "&";
					SWF_FLASHVARS += "&amp;";
				}
			}

			ctx.put("SWF_PARAMS", SWF_PARAMS);
			ctx.put("SWF_FLASHVARS", SWF_FLASHVARS);

			return getVelocityView().getVelocityEngine().getTemplate(template);

		} catch (Exception er) {
			System.out.println("Error downloading: " + er);
			er.printStackTrace();
			log.error("[Calendar :: service]", er);
		}
		return null;
	}
}