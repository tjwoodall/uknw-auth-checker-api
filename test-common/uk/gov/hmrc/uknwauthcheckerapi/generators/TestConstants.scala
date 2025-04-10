/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.uknwauthcheckerapi.generators

object TestConstants {
  val authorisationEndpoint:         String = "authorisation"
  val actorName:                     String = "actor"
  val bearerToken:                   String = "Bearer PFZBTElEX1RPS0VOPg=="
  val callAmountWithRetries:         Int    = 4
  val configAuditingKey:             String = "auditing.enabled"
  val configOverrideRetryInterval:   String = "1ms"
  val configMetricsKey:              String = "metrics.enabled"
  val configRetriesKey:              String = "http-verbs.retries.intervals"
  val configWireMockHost:            String = "localhost"
  val configWireMockPort:            Int    = 9999
  val credentialProviderId:          String = "id"
  val credentialProviderType:        String = "StandardApplication"
  val eisAuthorisationsEndpointPath: String = "/cau/validatecustomsauth/v1"
  val errorExpectedException:        String = "expected exception to be thrown"
  val errorUnexpectedResponse:       String = "unexpected response"
  val errorExpectedUpstreamResponse: String = "expected UpstreamErrorResponse when error is received"
  val emptyJson:                     String = "{}"
  val standardApplicationJson = """{ "authProviderId": { "clientId": "123" }}"""
  val emptyString:                     String = ""
  val invalidAuthTypeErrorMessage:     String = "Invalid auth type UKNW"
  val invalidAuthTypeEisErrorMessage:  String = "Invalid authorisation type : UKNW"
  val invalidEorisEisErrorMessage:     String = "Invalid format of EORI(s): 0000000001,0000000003"
  val serviceEndpointAuth:             String = "/auth/authorise"
  val serviceNameAuth:                 String = "auth"
  val serviceNameIntegrationFramework: String = "integration-framework"
}
