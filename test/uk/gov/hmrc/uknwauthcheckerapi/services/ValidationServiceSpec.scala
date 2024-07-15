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

package uk.gov.hmrc.uknwauthcheckerapi.services

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsError, JsPath, Json, JsonValidationError}
import uk.gov.hmrc.uknwauthcheckerapi.controllers.BaseSpec
import uk.gov.hmrc.uknwauthcheckerapi.errors.DataRetrievalError.ValidationDataRetrievalError
import uk.gov.hmrc.uknwauthcheckerapi.models.AuthorisationRequest
import uk.gov.hmrc.uknwauthcheckerapi.utils.JsonErrors

class ValidationServiceSpec extends BaseSpec {

  val service = new ValidationService()

  "validateRequest" should {
    "return AuthorisationRequest when AuthorisationRequest is valid" in {
      forAll { authorisationRequest: AuthorisationRequest =>
        val json = Json.toJson(authorisationRequest)

        val request = fakeRequestWithJsonBody(json)

        val response = service.validateRequest(request)

        response shouldBe Right(authorisationRequest)
      }
    }

    "return JsError when AuthorisationRequest json object is invalid" in {
      forAll { (invalidJson: String) =>
        val json = Json.toJson(invalidJson)

        val expectedResponse =
          ValidationDataRetrievalError(
            JsError(
              Seq((JsPath, Seq(JsonValidationError(JsonErrors.expectedJsObject))))
            )
          )

        val request = fakeRequestWithJsonBody(json)

        val response = service.validateRequest(request)

        response shouldBe Left(expectedResponse)
      }
    }

    "return JsError when AuthorisationRequest date is invalid" in {
      forAll { (authorisationRequest: AuthorisationRequest, invalidDate: String) =>
        val json = Json.toJson(authorisationRequest.copy(date = invalidDate))

        val expectedResponse =
          ValidationDataRetrievalError(
            JsError(
              Seq("date").map { field =>
                (JsPath \ field, Seq(JsonValidationError(s"$invalidDate is not a valid date in the format YYYY-MM-DD")))
              }
            )
          )

        val request = fakeRequestWithJsonBody(json)

        val response = service.validateRequest(request)

        response shouldBe Left(expectedResponse)
      }
    }

    "return JsError when AuthorisationRequest eoris are invalid" in {
      forAll { authorisationRequest: AuthorisationRequest =>
        val json = Json.toJson(authorisationRequest.copy(eoris = Seq("ABCD", "EFGH")))

        val expectedResponse =
          ValidationDataRetrievalError(
            JsError(
              Seq("eoris").map { field =>
                (
                  JsPath \ field,
                  Seq(
                    JsonValidationError(s"ABCD is not a supported EORI number"),
                    JsonValidationError(s"EFGH is not a supported EORI number")
                  )
                )
              }
            )
          )

        val request = fakeRequestWithJsonBody(json)

        val response = service.validateRequest(request)

        response shouldBe Left(expectedResponse)
      }
    }

    "return JsError when AuthorisationRequest eoris and date are invalid" in {
      forAll { (invalidEori: String, invalidDate: String) =>
        val json = Json.toJson(
          AuthorisationRequest(invalidDate, Seq(invalidEori))
        )

        val expectedResponse =
          ValidationDataRetrievalError(
            JsError(
              Seq("eoris").map { field =>
                (JsPath \ field, Seq(JsonValidationError(s"$invalidEori is not a supported EORI number")))
              } ++
                Seq("date").map { field =>
                  (JsPath \ field, Seq(JsonValidationError(s"$invalidDate is not a valid date in the format YYYY-MM-DD")))
                }
            )
          )

        val request = fakeRequestWithJsonBody(json)

        val response = service.validateRequest(request)

        response shouldBe Left(expectedResponse)
      }
    }
  }

}
