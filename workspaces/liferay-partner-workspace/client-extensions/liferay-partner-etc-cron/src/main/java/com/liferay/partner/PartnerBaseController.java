/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR
 * LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.partner;

import com.liferay.petra.string.StringBundler;

import java.net.URI;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import java.util.function.Function;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

/**
 * @author Elvison Victor
 */
public abstract class PartnerBaseController {

	public JSONObject get(Function<UriBuilder, URI> uriFunction) {
		return new JSONObject(
			WebClient.create(
				_lxcDXPServerProtocol + "://" + _lxcDXPMainDomain
			).get(
			).uri(
				uriBuilder -> uriFunction.apply(uriBuilder)
			).accept(
				MediaType.APPLICATION_JSON
			).header(
				HttpHeaders.AUTHORIZATION,
				"Bearer " + _oAuth2AccessToken.getTokenValue()
			).retrieve(
			).bodyToMono(
				String.class
			).block());
	}

	public void put(String bodyValue, String path) {
		WebClient.create(
			_lxcDXPServerProtocol + "://" + _lxcDXPMainDomain
		).put(
		).uri(
			uriBuilder -> uriBuilder.path(
				path
			).build()
		).accept(
			MediaType.APPLICATION_JSON
		).contentType(
			MediaType.APPLICATION_JSON
		).header(
			HttpHeaders.AUTHORIZATION,
			"Bearer " + _oAuth2AccessToken.getTokenValue()
		).bodyValue(
			bodyValue
		).retrieve(
		).bodyToMono(
			Void.class
		).block();
	}

	public void sendNotification(
		long activityId, ZonedDateTime zonedActivityExpirationDate,
		ZonedDateTime zonedDateTime) {

		long days = 0;

		if (zonedActivityExpirationDate.toLocalDate(
			).isEqual(
				zonedDateTime.plusDays(
					15
				).toLocalDate()
			)) {

			days = 15;
		}
		else if (zonedActivityExpirationDate.toLocalDate(
				).isEqual(
					zonedDateTime.plusDays(
						5
					).toLocalDate()
				)) {

			days = 5;
		}
		else if (zonedActivityExpirationDate.toLocalDate(
				).isEqual(
					zonedDateTime.plusDays(
						1
					).toLocalDate()
				)) {

			days = 1;
		}

		put(
			"",
			StringBundler.concat(
				"/o/c/activities/", activityId,
				"/object-actions/notificationDueDate", days,
				"DayTemplateAction"));
	}

	public String toString(ZonedDateTime zonedDateTime) {
		return zonedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

	@Autowired
	private OAuth2AccessToken _oAuth2AccessToken;

}