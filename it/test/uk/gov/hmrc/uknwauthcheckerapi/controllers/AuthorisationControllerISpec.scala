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

import java.time.ZonedDateTime

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

import play.api.http.MimeTypes
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.uknwauthcheckerapi.generators._
import uk.gov.hmrc.uknwauthcheckerapi.models._
import uk.gov.hmrc.uknwauthcheckerapi.models.constants.{CustomHeaderNames, HmrcContentTypes}
import uk.gov.hmrc.uknwauthcheckerapi.models.eis._
import uk.gov.hmrc.uknwauthcheckerapi.{BaseISpec, generators}

class AuthorisationControllerISpec extends BaseISpec {

  private val callAmountWith5xxRetries = 4
  private val defaultCallAmount        = 1

  trait TestContext {
    def reset(): Unit = {
      resetWireMock()
      stubAuthorised()
    }

    def verifyIntegrationFrameworkCall(calls: Int = defaultCallAmount): Unit =
      verify(
        calls,
        postRequestedFor(urlEqualTo(TestConstants.eisAuthorisationsEndpointPath))
          .withHeader(HeaderNames.AUTHORIZATION, equalTo(TestConstants.bearerToken))
          .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.JSON))
          .withHeader(HeaderNames.CONTENT_TYPE, equalToIgnoreCase(HmrcContentTypes.json))
          .withHeader(CustomHeaderNames.xCorrelationId, matching(TestRegexes.uuidPattern))
          .withHeader(HeaderNames.DATE, matching(TestRegexes.rfc7231DateTimePattern))
      )

    def verifyTimestampHeader(response: WSResponse): Unit =
      response.header(CustomHeaderNames.xTimestamp) match
        case Some(header) => header must fullyMatch regex TestRegexes.iso8601DateTimeFormatPattern
        case None         => fail("Timestamp header not present")
  }

  "POST /authorisations" should {
    "return OK (200) with authorised eoris when request has valid eoris" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest, dateTime: ZonedDateTime) =>
        reset()

        val request:                  AuthorisationRequest = validRequest.request
        val authorisationRequestJson: JsValue              = Json.toJson(request)
        val expectedResponse: JsValue = Json.toJson(
          EisAuthorisationsResponse(dateTime, appConfig.authType, Seq.empty)
        )
        stubPost(TestConstants.eisAuthorisationsEndpointPath, OK, expectedResponse.toString())

        val result: WSResponse = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe OK
        verifyTimestampHeader(result)
        verifyIntegrationFrameworkCall()
      }
    }

    "return BAD_REQUEST when request validation is invalid" in new TestContext {
      forAll { (invalidRequest: InvalidEorisAuthorisationRequest) =>
        reset()

        val authorisationRequestJson: JsValue = Json.toJson(invalidRequest.request)

        val result: WSResponse = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe BAD_REQUEST
        verifyTimestampHeader(result)
      }
    }

    "return BAD_REQUEST when request validation is invalid and duplicates passed" in new TestContext {
      forAll { (invalidRequest: DuplicateInvalidEorisAuthorisationRequest) =>
        reset()

        val authorisationRequestJson: JsValue = Json.toJson(invalidRequest.request)

        val result: WSResponse = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe BAD_REQUEST
        verifyTimestampHeader(result)
      }
    }

    "return REQUEST_ENTITY_TOO_LARGE when request validation is invalid" in new TestContext {
      forAll { (invalidRequest: TooLargeAuthorisationRequest) =>
        reset()

        val authorisationRequestJson: JsValue = Json.toJson(invalidRequest.request)

        val result: WSResponse = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe REQUEST_ENTITY_TOO_LARGE
        verifyTimestampHeader(result)
      }
    }

    "return BAD_REQUEST when integration framework returns BAD_REQUEST" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request:                  AuthorisationRequest = validRequest.request
        val authorisationRequestJson: JsValue              = Json.toJson(request)
        val response:                 JsValue              = Json.toJson(badRequestEisAuthorisationResponseError)
        stubPost(TestConstants.eisAuthorisationsEndpointPath, BAD_REQUEST, response.toString())

        val result: WSResponse = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe BAD_REQUEST
        verifyTimestampHeader(result)
        verifyIntegrationFrameworkCall()
      }
    }

    "return FORBIDDEN when integration framework returns FORBIDDEN" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request:                  AuthorisationRequest = validRequest.request
        val authorisationRequestJson: JsValue              = Json.toJson(request)
        val response:                 JsValue              = Json.toJson(forbiddenEisAuthorisationResponseError)
        stubPost(TestConstants.eisAuthorisationsEndpointPath, FORBIDDEN, response.toString())

        val result: WSResponse = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe FORBIDDEN
        verifyTimestampHeader(result)
        verifyIntegrationFrameworkCall()
      }
    }

    "return INTERNAL_SERVER_ERROR when integration framework returns INTERNAL_SERVER_ERROR" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request:                  AuthorisationRequest = validRequest.request
        val authorisationRequestJson: JsValue              = Json.toJson(request)
        val response:                 JsValue              = Json.toJson(internalServerErrorEisAuthorisationResponseError)
        stubPost(TestConstants.eisAuthorisationsEndpointPath, INTERNAL_SERVER_ERROR, response.toString())

        val result: WSResponse = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe INTERNAL_SERVER_ERROR
        verifyTimestampHeader(result)
        verifyIntegrationFrameworkCall(callAmountWith5xxRetries)
      }
    }

    "return INTERNAL_SERVER_ERROR when integration framework returns unhandled status" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request:                  AuthorisationRequest = validRequest.request
        val authorisationRequestJson: JsValue              = Json.toJson(request)
        val response:                 JsValue              = Json.toJson(imATeapotEisAuthorisationResponseError)
        stubPost(TestConstants.eisAuthorisationsEndpointPath, IM_A_TEAPOT, response.toString())

        val result: WSResponse = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe INTERNAL_SERVER_ERROR
        verifyTimestampHeader(result)
        verifyIntegrationFrameworkCall()
      }
    }

    "return INTERNAL_SERVER_ERROR when integration framework response is unable to be deserialised" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request:                  AuthorisationRequest = validRequest.request
        val authorisationRequestJson: JsValue              = Json.toJson(request)
        val response:                 String               = "{}"
        stubPost(TestConstants.eisAuthorisationsEndpointPath, INTERNAL_SERVER_ERROR, response)

        val result: WSResponse = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe INTERNAL_SERVER_ERROR
        verifyTimestampHeader(result)
        verifyIntegrationFrameworkCall(callAmountWith5xxRetries)
      }
    }

    "return SERVICE_UNAVAILABLE when integration framework returns BAD_GATEWAY" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request:                  AuthorisationRequest = validRequest.request
        val authorisationRequestJson: JsValue              = Json.toJson(request)
        stubPost(TestConstants.eisAuthorisationsEndpointPath, BAD_GATEWAY)

        val result: WSResponse = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe SERVICE_UNAVAILABLE
        verifyTimestampHeader(result)
      }
    }

    "return SERVICE_UNAVAILABLE when integration framework returns SERVICE_UNAVAILABLE" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request:                  AuthorisationRequest = validRequest.request
        val authorisationRequestJson: JsValue              = Json.toJson(request)
        stubPost(TestConstants.eisAuthorisationsEndpointPath, SERVICE_UNAVAILABLE)

        val result: WSResponse = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe SERVICE_UNAVAILABLE
        verifyTimestampHeader(result)
      }
    }

    "return UNAUTHORIZED when Authorization header is missing" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request:                  AuthorisationRequest  = validRequest.request
        val authorisationRequestJson: JsValue               = Json.toJson(request)
        val headers:                  Seq[(String, String)] = defaultHeaders.filterNot(header => header == authorizationHeader)

        val result: WSResponse = postRequest(authorisationsUrl, authorisationRequestJson, headers)

        result.status mustBe UNAUTHORIZED
        verifyTimestampHeader(result)
      }
    }

    "return NOT_ACCEPTABLE when Accept header is missing" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        reset()

        val request:                  AuthorisationRequest  = validRequest.request
        val authorisationRequestJson: JsValue               = Json.toJson(request)
        val headers:                  Seq[(String, String)] = defaultHeaders.filterNot(header => header == acceptHeader)

        val result: WSResponse = postRequest(authorisationsUrl, authorisationRequestJson, headers)

        result.status mustBe NOT_ACCEPTABLE
        verifyTimestampHeader(result)
      }
    }

    "return NOT_ACCEPTABLE when Content-Type header is missing" in new TestContext {
      reset()

      val headers: Seq[(String, String)] = defaultHeaders.filterNot(header => header == contentTypeHeader)

      val result: WSResponse = postEmptyRequest(authorisationsUrl, headers)

      result.status mustBe NOT_ACCEPTABLE
      verifyTimestampHeader(result)
    }
  }

}
