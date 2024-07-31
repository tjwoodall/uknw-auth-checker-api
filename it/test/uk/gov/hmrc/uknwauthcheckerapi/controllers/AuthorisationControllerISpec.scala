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

package uk.gov.hmrc.uknwauthcheckerapi.controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

import play.api.http.MimeTypes
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.uknwauthcheckerapi.BaseISpec
import uk.gov.hmrc.uknwauthcheckerapi.generators.{InvalidEorisAuthorisationRequest, TestRegexes, UtcDateTime, ValidAuthorisationRequest}
import uk.gov.hmrc.uknwauthcheckerapi.models.eis.EisAuthorisationsResponse
import uk.gov.hmrc.uknwauthcheckerapi.models.{AuthorisationRequest, CustomHeaderNames}
import uk.gov.hmrc.uknwauthcheckerapi.utils.{EisAuthTypes, HmrcContentTypes}

class AuthorisationControllerISpec extends BaseISpec {

  private val callAmountWith5xxRetries = 4
  private val defaultCallAmount        = 1

  trait Setup {
    def reset(): Unit = {
      resetWireMock()
      stubAuthorised()
    }

    def verifyIntegrationFrameworkCall(calls: Int = defaultCallAmount): Unit =
      verify(
        calls,
        postRequestedFor(urlEqualTo(eisAuthorisationsEndpointPath))
          .withHeader(HeaderNames.AUTHORIZATION, equalTo(bearerToken))
          .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.JSON))
          .withHeader(HeaderNames.CONTENT_TYPE, equalToIgnoreCase(HmrcContentTypes.json))
          .withHeader(CustomHeaderNames.xCorrelationId, matching(TestRegexes.uuidPattern))
          .withHeader(HeaderNames.DATE, matching(TestRegexes.rfc7231DateTimePattern))
      )
  }

  "POST /authorisations" should {
    "return OK (200) with authorised eoris when request has valid eoris" in new Setup {
      forAll { (validRequest: ValidAuthorisationRequest, utcDateTime: UtcDateTime) =>
        reset()

        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        val expectedResponse = Json.toJson(
          EisAuthorisationsResponse(utcDateTime.formatted, EisAuthTypes.nopWaiver, Seq.empty)
        )

        stubPost(eisAuthorisationsEndpointPath, OK, expectedResponse.toString())

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe OK

        verifyIntegrationFrameworkCall()
      }
    }

    "return BAD_REQUEST when request validation is invalid" in new Setup {
      forAll { (invalidRequest: InvalidEorisAuthorisationRequest) =>
        reset()

        val authorisationRequestJson = Json.toJson(invalidRequest.request)

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe BAD_REQUEST
      }
    }

    "return BAD_REQUEST when integration framework returns BAD_REQUEST" in new Setup {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        val response = Json.toJson(badRequestEisAuthorisationResponseError)

        stubPost(eisAuthorisationsEndpointPath, BAD_REQUEST, response.toString())

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe BAD_REQUEST

        verifyIntegrationFrameworkCall()
      }
    }

    "return FORBIDDEN when integration framework returns FORBIDDEN" in new Setup {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        val response = Json.toJson(forbiddenEisAuthorisationResponseError)

        stubPost(eisAuthorisationsEndpointPath, FORBIDDEN, response.toString())

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe FORBIDDEN

        verifyIntegrationFrameworkCall()
      }
    }

    "return INTERNAL_SERVER_ERROR when integration framework returns INTERNAL_SERVER_ERROR" in new Setup {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        val response = Json.toJson(internalServerErrorEisAuthorisationResponseError)

        stubPost(eisAuthorisationsEndpointPath, INTERNAL_SERVER_ERROR, response.toString())

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe INTERNAL_SERVER_ERROR

        verifyIntegrationFrameworkCall(callAmountWith5xxRetries)
      }
    }

    "return INTERNAL_SERVER_ERROR when integration framework returns unhandled status" in new Setup {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        val response = Json.toJson(imATeapotEisAuthorisationResponseError)

        stubPost(eisAuthorisationsEndpointPath, IM_A_TEAPOT, response.toString())

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe INTERNAL_SERVER_ERROR

        verifyIntegrationFrameworkCall()
      }
    }

    "return METHOD_NOT_ALLOWED when integration framework returns METHOD_NOT_ALLOWED" in new Setup {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        val response = Json.toJson(methodNotAllowedEisAuthorisationResponseError)

        stubPost(eisAuthorisationsEndpointPath, METHOD_NOT_ALLOWED, response.toString())

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe METHOD_NOT_ALLOWED

        verifyIntegrationFrameworkCall()
      }
    }

    "return METHOD_NOT_ALLOWED (405) when request is DELETE" in new Setup {
      reset()

      val result: WSResponse = deleteRequest(authorisationsUrl)
      result.status mustBe METHOD_NOT_ALLOWED
    }

    "return METHOD_NOT_ALLOWED (405) when request is GET" in new Setup {
      reset()

      val result: WSResponse = getRequest(authorisationsUrl)
      result.status mustBe METHOD_NOT_ALLOWED
    }

    "return METHOD_NOT_ALLOWED (405) when request is HEAD" in new Setup {
      reset()

      val result: WSResponse = headRequest(authorisationsUrl)
      result.status mustBe METHOD_NOT_ALLOWED
    }

    "return METHOD_NOT_ALLOWED (405) when request is OPTIONS" in new Setup {
      reset()

      val result: WSResponse = optionsRequest(authorisationsUrl)
      result.status mustBe METHOD_NOT_ALLOWED
    }

    "return METHOD_NOT_ALLOWED (405) when request is PUT" in new Setup {
      forAll { (authorisationRequest: AuthorisationRequest) =>
        reset()

        val authorisationRequestJson = Json.toJson(authorisationRequest)
        val result                   = putRequest(authorisationsUrl, authorisationRequestJson)
        result.status mustBe METHOD_NOT_ALLOWED
      }
    }

    "return METHOD_NOT_ALLOWED (405) when request is PATCH" in new Setup {
      forAll { (authorisationRequest: AuthorisationRequest) =>
        reset()

        val authorisationRequestJson = Json.toJson(authorisationRequest)
        val result                   = patchRequest(authorisationsUrl, authorisationRequestJson)
        result.status mustBe METHOD_NOT_ALLOWED
      }
    }

    "return SERVICE_UNAVAILABLE when integration framework returns BAD_GATEWAY" in new Setup {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        stubPost(eisAuthorisationsEndpointPath, BAD_GATEWAY)

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe SERVICE_UNAVAILABLE
      }
    }

    "return UNAUTHORIZED when bearer token is missing" in new Setup {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        val headers = defaultHeaders.filterNot(header => header == authorizationHeader)

        val result = postRequest(authorisationsUrl, authorisationRequestJson, headers)

        result.status mustBe UNAUTHORIZED
      }
    }
  }
}
