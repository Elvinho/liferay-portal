/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.sample;

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.json.JSONObject;

/**
 * @author Raymond Augé
 * @author Gregory Amerson
 * @author Brian Wing Shun Chan
 */
@RequestMapping("/object/action/1")
@RestController
public class ObjectAction1RestController extends BaseRestController {

	@PostMapping
	public ResponseEntity<String> post(
			@AuthenticationPrincipal Jwt jwt, @RequestBody String json) {

		JSONObject jsonObject = new JSONObject(json);

		JSONObject objectEntryDTOEVPRequest = jsonObject.getJSONObject("objectEntryDTOEVPRequest");

		JSONObject properties = objectEntryDTOEVPRequest.getJSONObject("properties");

		long organizationId = properties.getLong("r_organization_c_evpOrganizationId");

		System.out.println("jwt.getTokenValue()" + jwt.getTokenValue());

		String token = jwt.getTokenValue();

		JSONObject responseJSONObject = _get(
				organizationId, token);

		System.out.println("responseJSONObject - " + responseJSONObject);

		return new ResponseEntity<>(json, HttpStatus.OK);
	}

	private static final Log _log = LogFactory.getLog(
			ObjectAction1RestController.class);

	private JSONObject _get(long organizationId, String token) {
		System.out.println("token - " + token);

		return new JSONObject(
				Objects.requireNonNull(WebClient.create(
						_lxcDXPServerProtocol + "://" + _lxcDXPMainDomain)
						.get()
						.uri("/o/c/evporganizations/" + organizationId)
						.header(
								"Authorization", "Bearer " + token)
						.retrieve().bodyToMono(
								String.class)
						.block()));
	}

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

}