/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.evp;

import com.liferay.petra.string.StringBundler;

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

/**
 * @author Elvison Victor
 */
@RequestMapping("/object/action/2")
@RestController
public class ObjectAction2RestController extends BaseRestController {

	@PostMapping
	public ResponseEntity<String> post(
		@AuthenticationPrincipal Jwt jwt, @RequestBody String json) {

		JSONObject evpOrganizationJSONObject = new JSONObject(json);

		JSONObject originalObjectEntryDTOEVPOrganizationJSONObject =
			evpOrganizationJSONObject.getJSONObject(
				"originalObjectEntryDTOEVPOrganization");

		long evpOrganizationId =
			originalObjectEntryDTOEVPOrganizationJSONObject.getLong("id");

		get(
			output -> {
				JSONObject evpOrganizationsJSONObject = new JSONObject(output);

				JSONObject evpOrganizationStatusJSONObject =
					evpOrganizationsJSONObject.getJSONObject(
						"organizationStatus");

				String evpOrganizationStatusKey =
					evpOrganizationStatusJSONObject.getString("key");

				get(
					response -> {
						JSONObject evpRequestsJSONObject = new JSONObject(
							response);

						if (evpRequestsJSONObject.getInt("totalCount") < 0) {
							return;
						}

						JSONArray itemsJSONArray =
							evpRequestsJSONObject.getJSONArray("items");

						for (int i = 0; i < itemsJSONArray.length(); i++) {
							JSONObject itemJSONObject =
								itemsJSONArray.getJSONObject(i);

							JSONObject evpRequestsStatusJSONObject =
								itemJSONObject.getJSONObject("requestStatus");

							if (evpOrganizationStatusKey.equals("verified")) {
								JSONObject evpRequestTypeJSONObject =
									itemJSONObject.getJSONObject("requestType");

								if (evpRequestTypeJSONObject.getString(
										"key"
									).equals(
										"grant"
									)) {

									evpRequestsStatusJSONObject.put(
										"key", "awaitingApprovalOnEvp"
									).put(
										"name", "Awaiting Approval On EVP"
									);
								}
								else {
									evpRequestsStatusJSONObject.put(
										"key", "awaitingApprovalOnManager"
									).put(
										"name", "Awaiting Approval on Manager"
									);
								}
							}
							else if (evpOrganizationStatusKey.equals(
										"rejected")) {

								evpRequestsStatusJSONObject.put(
									"key", "rejected"
								).put(
									"name", "Rejected"
								);
							}
						}

						_put(
							itemsJSONArray.toString(), "/o/c/evprequests/batch",
							jwt);
					},
					jwt,
					StringBundler.concat(
						"/o/c/evprequests?filter=",
						"r_organization_c_evpOrganizationId eq '",
						evpOrganizationId, "'"));
			},
			jwt, "/o/c/evporganizations/" + String.valueOf(evpOrganizationId));

		return new ResponseEntity<>(json, HttpStatus.OK);
	}

	private void _put(String bodyValue, String path, Jwt jwt) {
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
			"Authorization", "Bearer " + jwt.getTokenValue()
		).bodyValue(
			bodyValue
		).retrieve(
		).bodyToMono(
			Void.class
		).subscribe();
	}

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

}