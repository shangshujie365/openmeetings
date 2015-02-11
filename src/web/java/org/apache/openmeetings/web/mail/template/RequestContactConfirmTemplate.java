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
package org.apache.openmeetings.web.mail.template;

import org.apache.openmeetings.db.entity.user.User;
import org.apache.openmeetings.db.entity.user.UserContact;
import org.apache.openmeetings.web.app.WebSession;
import org.apache.wicket.markup.html.basic.Label;

public class RequestContactConfirmTemplate extends AbstractTemplatePanel {
	private static final long serialVersionUID = 1L;

	public RequestContactConfirmTemplate(User user, UserContact contact) {
		super(WebSession.getLanguage());
		add(new Label("firstName", user.getFirstname()));
		add(new Label("lastName", user.getLastname()));
		add(new Label("addedFirstName", contact.getContact().getFirstname()));
		add(new Label("addedLastName", contact.getContact().getLastname()));
	}
	
	public static String getEmail(User user, UserContact contact) {
		return renderPanel(new RequestContactConfirmTemplate(user, contact)).toString();
	}
}