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

import org.scalatest.prop.TableDrivenPropertyChecks.whenever
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.uknwauthcheckerapi.BaseISpec
import uk.gov.hmrc.uknwauthcheckerapi.models.AuthorisationRequest
import uk.gov.hmrc.uknwauthcheckerapi.models.eis.EisAuthorisationsResponse

import java.time.LocalDate

class AuthorisationControllerISpec extends BaseISpec {

  "POST /authorisations" should {
    "return OK (200) with authorised eoris when request has valid date and eoris" in {
      forAll { authorisationRequest: AuthorisationRequest =>
        whenever (authorisationRequest.date.isDefined && authorisationRequest.eoris.nonEmpty) {
          val authorisationRequestJson = Json.toJson(authorisationRequest)

          val expectedResponse = Json.toJson(
            EisAuthorisationsResponse(LocalDate.now(), "UKNW", Seq.empty)
          )

          stubPost("/cau/validatecustomsauth/v1", OK, expectedResponse.toString())

          val result = postRequest(authorisationsUrl, authorisationRequestJson)

          result.status mustBe OK
        }
      }
    }

    "return METHOD_NOT_ALLOWED (405) when request is not DELETE" in {
      val result = deleteRequest(authorisationsUrl)
      result.status mustBe METHOD_NOT_ALLOWED
    }

    "return METHOD_NOT_ALLOWED (405) when request is not GET" in {
      val result = deleteRequest(authorisationsUrl)
      result.status mustBe METHOD_NOT_ALLOWED
    }

    "return METHOD_NOT_ALLOWED (405) when request is not HEAD" in {
      val result = deleteRequest(authorisationsUrl)
      result.status mustBe METHOD_NOT_ALLOWED
    }

    "return METHOD_NOT_ALLOWED (405) when request is not OPTIONS" in {
      val result = deleteRequest(authorisationsUrl)
      result.status mustBe METHOD_NOT_ALLOWED
    }

    "return METHOD_NOT_ALLOWED (405) when request is not PUT" in {
      forAll { (authorisationRequest: AuthorisationRequest) =>
        val authorisationRequestJson = Json.toJson(authorisationRequest)
        val result = putRequest(authorisationsUrl, authorisationRequestJson)
        result.status mustBe METHOD_NOT_ALLOWED
      }
    }

    "return METHOD_NOT_ALLOWED (405) when request is not PATCH" in {
      forAll { (authorisationRequest: AuthorisationRequest) =>
        val authorisationRequestJson = Json.toJson(authorisationRequest)
        val result = patchRequest(authorisationsUrl, authorisationRequestJson)
        result.status mustBe METHOD_NOT_ALLOWED
      }
    }
  }
}
