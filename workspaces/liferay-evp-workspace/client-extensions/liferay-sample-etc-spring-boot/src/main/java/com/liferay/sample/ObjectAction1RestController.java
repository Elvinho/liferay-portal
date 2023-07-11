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

import java.net.URI;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

/**
 * @author Raymond Augé
 * @author Gregory Amerson
 * @author Brian Wing Shun Chan
 */
@RequestMapping("/object/action/1")
@RestController
public class ObjectAction1RestController extends BaseRestController {
	@Autowired
	ObjectEntryManager1RestController objectEntryManager1RestController;

	@PostMapping
	public ResponseEntity<String> post(
			@AuthenticationPrincipal Jwt jwt, @RequestBody String json) {

		JSONObject jsonObject = new JSONObject(json);

		JSONObject objectEntryDTOEVPRequest = jsonObject.getJSONObject("objectEntryDTOEVPRequest");

		JSONObject properties = objectEntryDTOEVPRequest.getJSONObject("properties");

		long organizationId = properties.getLong("r_organization_c_evpOrganizationId");
		System.out.println("organizationId" + organizationId);

		System.out.println("jwt.getTokenValue()" + jwt.getTokenValue());

		JSONObject responseJSONObject = _get(

				uriBuilder -> uriBuilder.path(
						"/o/c/evporganizations/" + organizationId)
						.build(),
				jwt);

		System.out.println("responseJSONObject" + responseJSONObject);

		return new ResponseEntity<>(json, HttpStatus.OK);
	}

	@GetMapping("/getObjectById")
	public ObjectEntryManager1RestController getObjectById(@AuthenticationPrincipal Jwt jwt, @RequestBody String json) {
		return objectEntryManager1RestController;
	}

	private static final Log _log = LogFactory.getLog(
			ObjectAction1RestController.class);

	private JSONObject _get(Function<UriBuilder, URI> uriFunction, Jwt jwt) {
		return new JSONObject(
				WebClient.create(
						_lxcDXPServerProtocol + "://" + _lxcDXPMainDomain).get()
						.uri(
								uriBuilder -> uriFunction.apply(uriBuilder))
						.accept(
								MediaType.APPLICATION_JSON)
						.header(
								"Authorization", "Bearer " + jwt.getTokenValue())
						.retrieve().bodyToMono(
								String.class)
						.block());
	}

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

}