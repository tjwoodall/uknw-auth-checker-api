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

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.uknwauthcheckerapi.BaseISpec
import uk.gov.hmrc.uknwauthcheckerapi.generators.ValidAuthorisationRequest
import uk.gov.hmrc.uknwauthcheckerapi.models.AuthorisationRequest
import uk.gov.hmrc.uknwauthcheckerapi.models.eis.EisAuthorisationsResponse
import uk.gov.hmrc.uknwauthcheckerapi.utils.EisAuthTypes

import java.time.LocalDate

class AuthorisationControllerISpec extends BaseISpec {

  "POST /authorisations" should {
    "return OK (200) with authorised eoris when request has valid date and eoris" in {
      forAll { (validRequest: ValidAuthorisationRequest, date: LocalDate) =>
        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        val expectedResponse = Json.toJson(
          EisAuthorisationsResponse(date, EisAuthTypes.NopWaiver, Seq.empty)
        )

        stubPost(eisAuthorisationsEndpointPath, OK, expectedResponse.toString())

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe OK
      }
    }

    "return BAD_REQUEST when request validation is invalid" in {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        val request = validRequest.request.copy(
          date = "ABCD"
        )

        val authorisationRequestJson = Json.toJson(request)

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe BAD_REQUEST
      }
    }

    "return BAD_REQUEST when integration framework returns BAD_REQUEST" in {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        val response = Json.toJson(badRequestEisAuthorisationResponseError)

        stubPost(eisAuthorisationsEndpointPath, BAD_REQUEST, response.toString())

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe BAD_REQUEST
      }
    }

    "return FORBIDDEN when integration framework returns FORBIDDEN" in {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        val response = Json.toJson(forbiddenEisAuthorisationResponseError)

        stubPost(eisAuthorisationsEndpointPath, FORBIDDEN, response.toString())

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe FORBIDDEN
      }
    }

    "return INTERNAL_SERVER_ERROR when integration framework returns INTERNAL_SERVER_ERROR" in {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        val response = Json.toJson(internalServerErrorEisAuthorisationResponseError)

        stubPost(eisAuthorisationsEndpointPath, INTERNAL_SERVER_ERROR, response.toString())

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return INTERNAL_SERVER_ERROR when integration framework returns unhandled status" in {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        val response = Json.toJson(imATeapotEisAuthorisationResponseError)

        stubPost(eisAuthorisationsEndpointPath, IM_A_TEAPOT, response.toString())

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return METHOD_NOT_ALLOWED when integration framework returns METHOD_NOT_ALLOWED" in {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        val response = Json.toJson(methodNotAllowedEisAuthorisationResponseError)

        stubPost(eisAuthorisationsEndpointPath, METHOD_NOT_ALLOWED, response.toString())

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe METHOD_NOT_ALLOWED
      }
    }

    "return METHOD_NOT_ALLOWED (405) when request is DELETE" in {
      val result = deleteRequest(authorisationsUrl)
      result.status mustBe METHOD_NOT_ALLOWED
    }

    "return METHOD_NOT_ALLOWED (405) when request is GET" in {
      val result = getRequest(authorisationsUrl)
      result.status mustBe METHOD_NOT_ALLOWED
    }

    "return METHOD_NOT_ALLOWED (405) when request is HEAD" in {
      val result = headRequest(authorisationsUrl)
      result.status mustBe METHOD_NOT_ALLOWED
    }

    "return METHOD_NOT_ALLOWED (405) when request is OPTIONS" in {
      val result = optionsRequest(authorisationsUrl)
      result.status mustBe METHOD_NOT_ALLOWED
    }

    "return METHOD_NOT_ALLOWED (405) when request is PUT" in {
      forAll { (authorisationRequest: AuthorisationRequest) =>
        val authorisationRequestJson = Json.toJson(authorisationRequest)
        val result = putRequest(authorisationsUrl, authorisationRequestJson)
        result.status mustBe METHOD_NOT_ALLOWED
      }
    }

    "return METHOD_NOT_ALLOWED (405) when request is PATCH" in {
      forAll { (authorisationRequest: AuthorisationRequest) =>
        val authorisationRequestJson = Json.toJson(authorisationRequest)
        val result = patchRequest(authorisationsUrl, authorisationRequestJson)
        result.status mustBe METHOD_NOT_ALLOWED
      }
    }

    "return SERVICE_UNAVAILABLE when integration framework returns BAD_GATEWAY" in {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        val request = validRequest.request

        val authorisationRequestJson = Json.toJson(request)

        stubPost(eisAuthorisationsEndpointPath, BAD_GATEWAY)

        val result = postRequest(authorisationsUrl, authorisationRequestJson)

        result.status mustBe SERVICE_UNAVAILABLE
      }
    }
  }
}
