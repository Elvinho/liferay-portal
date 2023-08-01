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

package com.liferay.evp;

import java.time.Duration;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.util.retry.Retry;

/**
 * @author Raymond Augé
 * @author Gregory Amerson
 * @author Brian Wing Shun Chan
 */
@RequestMapping("/object/action/2")
@RestController
public class ObjectAction2RestController extends BaseRestController {

	String token;
	String organizationName;
	long organizationId;
	String statusKeyOrganization;

	@PostMapping
	public ResponseEntity<String> post(
			@AuthenticationPrincipal Jwt jwt, @RequestBody String json) {

		JSONObject organization = new JSONObject(json);

		JSONObject originalObjectEntryDTOEVPOrganization = organization
				.getJSONObject("originalObjectEntryDTOEVPOrganization");

		organizationId = originalObjectEntryDTOEVPOrganization.getLong("id");

		token = jwt.getTokenValue();

		getOrganization(organizationId, token);

		return new ResponseEntity<>(json, HttpStatus.OK);
	}

	private static final Log _log = LogFactory.getLog(
			ObjectAction2RestController.class);

	private void _put(String bodyValue, String path) {
		WebClient.create(
				_lxcDXPServerProtocol + "://" + _lxcDXPMainDomain)
				.put().uri(
						uriBuilder -> uriBuilder.path(
								path).build())
				.accept(
						MediaType.APPLICATION_JSON)
				.contentType(
						MediaType.APPLICATION_JSON)
				.header(
						"Authorization", "Bearer " + token)
				.bodyValue(
						bodyValue)
				.retrieve().bodyToMono(
						Void.class)
				.subscribe();
	}

	private JSONObject getOrganization(long organizationId, String token) {

		return new JSONObject(
				Objects.requireNonNull(WebClient.create(
						_lxcDXPServerProtocol + "://" + _lxcDXPMainDomain)
						.get()
						.uri("/o/c/evporganizations/" + organizationId)
						.header(
								"Authorization", "Bearer " + token)
						.retrieve().bodyToMono(
								String.class)
						.retryWhen(
								Retry.backoff(
										3, Duration.ofSeconds(1))
										.doAfterRetry(retrySignal -> _log.info("Retrying request")))
						.doOnNext(
								output -> {
									if (_log.isInfoEnabled()) {
										_log.info("getOrg: " + output);
									}

									JSONObject organization = new JSONObject(output);

									JSONObject organizationStatus = organization.getJSONObject("organizationStatus");
									String organizationName = organization.getString("organizationName");
									statusKeyOrganization = organizationStatus.getString("key");
									_get(organizationName, token);

								})
						.subscribe()));
	}

	private JSONObject _get(String organizationName, String token) {

		return new JSONObject(
				Objects.requireNonNull(WebClient.create(
						_lxcDXPServerProtocol + "://" + _lxcDXPMainDomain)
						.get()
						.uri("/o/c/evprequests?filter=r_organization_c_evpOrganizationId eq '" + organizationId + "'")
						.header(
								"Authorization", "Bearer " + token)
						.retrieve().bodyToMono(
								String.class)
						.retryWhen(
								Retry.backoff(
										3, Duration.ofSeconds(1))
										.doAfterRetry(retrySignal -> _log.info("Retrying request")))
						.doOnNext(
								output -> {

									JSONObject requests = new JSONObject(output);

									if (statusKeyOrganization.equals("verified")) {
										if (requests.getInt("totalCount") > 0) {
											JSONArray items = requests.getJSONArray("items");

											for (int i = 0; i < items.length(); i++) {

												JSONObject itemJSONObject = items.getJSONObject(i);

												JSONObject requestType = itemJSONObject.getJSONObject("requestType");

												JSONObject requestsStatusJSONObject = itemJSONObject
														.getJSONObject("requestStatus");

												if (requestType.getString("key").equals("grant")) {
													requestsStatusJSONObject.put(
															"key", "awaitingApprovalOnEvp").put(
																	"name", "Awaiting Approval On EVP");
												}

												if (requestType.getString("key").equals("service")) {

													requestsStatusJSONObject.put(
															"key", "awaitingApprovalOnManager").put(
																	"name", "Awaiting Approval on Manager");
												}
											}
											_put(items.toString(), "/o/c/evprequests/batch");
										}
									}

									if (statusKeyOrganization.equals("rejected")) {
										if (requests.getInt("totalCount") > 0) {
											JSONArray items = requests.getJSONArray("items");

											for (int i = 0; i < items.length(); i++) {

												JSONObject itemJSONObject = items.getJSONObject(i);

												JSONObject requestsStatusJSONObject = itemJSONObject
														.getJSONObject("requestStatus");

												requestsStatusJSONObject.put(
														"key", "rejected").put(
																"name", "Rejected");

											}

											_put(items.toString(), "/o/c/evprequests/batch");
										}
									}
								})
						.subscribe()));
	}

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

}