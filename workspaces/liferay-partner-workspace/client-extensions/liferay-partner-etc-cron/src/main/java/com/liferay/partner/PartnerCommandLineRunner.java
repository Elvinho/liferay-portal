/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.partner;

import com.liferay.petra.string.StringBundler;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author Jair Medeiros
 */
@Component
public class PartnerCommandLineRunner
		extends BasePartnerController implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {
		ZonedDateTime zonedDateTime = ZonedDateTime.now();

		JSONObject responseJSONObject = get(
				uriBuilder -> uriBuilder.path(
						"/o/c/activities").queryParam(
								"filter",
								"activityStatus eq 'approved' and startDate le " +
										toString(zonedDateTime))
						.queryParam(
								"page", "1")
						.queryParam(
								"pageSize", "-1")
						.build());

		if (responseJSONObject.getInt("totalCount") > 0) {
			JSONArray itemsJSONArray = responseJSONObject.getJSONArray("items");

			for (int i = 0; i < itemsJSONArray.length(); i++) {
				JSONObject itemJSONObject = itemsJSONArray.getJSONObject(i);

				JSONObject activityStatusJSONObject = itemJSONObject.getJSONObject("activityStatus");

				activityStatusJSONObject.put(
						"key", "active").put(
								"name", "Active");
			}

			put(itemsJSONArray.toString(), "/o/c/activities/batch");
		}

		responseJSONObject = get(
				uriBuilder -> uriBuilder.path(
						"/o/c/activities").queryParam(
								"filter",
								"activityStatus eq 'active' and endDate lt " +
										toString(zonedDateTime.minusDays(30)))
						.queryParam(
								"page", "1")
						.queryParam(
								"pageSize", "-1")
						.build());

		if (responseJSONObject.getInt("totalCount") > 0) {
			JSONArray itemsJSONArray = responseJSONObject.getJSONArray("items");

			for (int i = 0; i < itemsJSONArray.length(); i++) {
				JSONObject itemJSONObject = itemsJSONArray.getJSONObject(i);

				JSONObject activityStatusJSONObject = itemJSONObject.getJSONObject("activityStatus");

				activityStatusJSONObject.put(
						"key", "expired").put(
								"name", "Expired");
			}

			put(itemsJSONArray.toString(), "/o/c/activities/batch");
		}

		responseJSONObject = get(
				uriBuilder -> uriBuilder.path(
						"/o/c/activities").queryParam(
								"filter",
								StringBundler.concat(
										"submitted eq true and activityStatus eq 'active' and ",
										"endDate le ", toString(zonedDateTime.minusDays(15)),
										" and mdfReqToActs/mdfRequestStatus eq 'approved'"))
						.queryParam(
								"nestedFields", "actToMDFClmActs")
						.queryParam(
								"page", "1")
						.queryParam(
								"pageSize", "-1")
						.build());

		if (responseJSONObject.getInt("totalCount") > 0) {
			JSONArray itemsJSONArray = responseJSONObject.getJSONArray("items");

			for (int i = 0; i < itemsJSONArray.length(); i++) {
				JSONObject itemJSONObject = itemsJSONArray.getJSONObject(i);

				long activityId = itemJSONObject.getLong("id");

				ZonedDateTime zonedActivityEndDate = ZonedDateTime.parse(
						itemJSONObject.getString("endDate"));

				ZonedDateTime zonedActivityExpirationDate = zonedActivityEndDate.plusDays(30);

				long days = zonedActivityEndDate.until(zonedDateTime, ChronoUnit.DAYS);
				System.out.println("days ->" + days);

				JSONArray mdfClaimActivitiesJSONArray = itemJSONObject.getJSONArray("actToMDFClmActs");

				if (mdfClaimActivitiesJSONArray.length() == 0) {
					sendNotification(
							activityId, zonedActivityExpirationDate.toLocalDate(), zonedDateTime.toLocalDate(), days);
				} else {
					JSONArray claimedMdfClaimActivityJSONArray = new JSONArray();

					for (int j = 0; j < mdfClaimActivitiesJSONArray.length(); j++) {

						JSONObject mdfClaimActivityJSONObject = mdfClaimActivitiesJSONArray.getJSONObject(j);

						Boolean selectedActivity = mdfClaimActivityJSONObject.getBoolean("selected");

						if (selectedActivity) {
							long mdfClaimId = mdfClaimActivityJSONObject.getLong(
									"r_mdfClmToMDFClmActs_c_mdfClaimId");

							responseJSONObject = get(
									uriBuilder -> uriBuilder.path(
											"/o/c/mdfclaims/" + mdfClaimId).build());

							JSONObject mdfClaimStatusJSONObject = responseJSONObject.getJSONObject(
									"mdfClaimStatus");

							String mdfClaimStatusKey = mdfClaimStatusJSONObject.getString("key");

							if (!mdfClaimStatusKey.equals("draft") &&
									!mdfClaimStatusKey.equals(
											"moreInfoRequested")
									&&
									!mdfClaimStatusKey.equals("cancel") &&
									!mdfClaimStatusKey.equals("rejected")) {

								claimedMdfClaimActivityJSONArray.put(
										mdfClaimActivityJSONObject);

								break;
							}
						}
					}

					if (claimedMdfClaimActivityJSONArray.length() == 0) {
						sendNotification(
								activityId, zonedActivityExpirationDate.toLocalDate(),
								zonedDateTime.toLocalDate(), days);
					}
				}
			}
		}
	}

}