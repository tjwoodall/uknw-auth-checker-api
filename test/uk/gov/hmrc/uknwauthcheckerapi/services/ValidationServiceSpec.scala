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
import uk.gov.hmrc.uknwauthcheckerapi.generators.{NoEorisAuthorisationRequest, TooManyEorisAuthorisationRequest}
import uk.gov.hmrc.uknwauthcheckerapi.models.AuthorisationRequest
import uk.gov.hmrc.uknwauthcheckerapi.models.constants.{ApiErrorMessages, JsonErrorMessages, JsonPaths}

class ValidationServiceSpec extends BaseSpec {

  private lazy val service = new ValidationService(minMaxValues)

  "validateRequest without overwritten config" should {

    "return AuthorisationRequest when AuthorisationRequest is valid" in {
      forAll { (authorisationRequest: AuthorisationRequest) =>
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
              Seq((JsPath, Seq(JsonValidationError(JsonErrorMessages.expectedJsObject))))
            )
          )

        val request = fakeRequestWithJsonBody(json)

        val response = service.validateRequest(request)

        response shouldBe Left(expectedResponse)
      }
    }

    "return JsError when AuthorisationRequest eoris are invalid and no duplicates" in {
      forAll { (authorisationRequest: AuthorisationRequest) =>
        val json = Json.toJson(authorisationRequest.copy(eoris = Seq("ABCD", "EFGH")))

        val expectedResponse =
          ValidationDataRetrievalError(
            JsError(
              Seq(JsonPaths.eoris).map { field =>
                (
                  JsPath \ field,
                  Seq(
                    JsonValidationError(ApiErrorMessages.invalidEori("ABCD")),
                    JsonValidationError(ApiErrorMessages.invalidEori("EFGH"))
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

    "return JsError when AuthorisationRequest eoris are invalid and duplicates must be flattened" in {
      forAll { (authorisationRequest: AuthorisationRequest) =>
        val json = Json.toJson(authorisationRequest.copy(eoris = Seq("ABCD", "ABCD")))

        val expectedResponse =
          ValidationDataRetrievalError(
            JsError(
              Seq(JsonPaths.eoris).map { field =>
                (
                  JsPath \ field,
                  Seq(
                    JsonValidationError(ApiErrorMessages.invalidEori("ABCD"))
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

    "must return JSError with usual case content when maxEori is 3000" when {
      "AuthorisationRequest has too many Eoris" in {
        forAll { (tooManyEorisRequest: TooManyEorisAuthorisationRequest) =>
          val json = Json.toJson(tooManyEorisRequest.request)

          val expectedResponse =
            ValidationDataRetrievalError(
              JsError(
                Seq((JsPath \ JsonPaths.eoris, Seq(JsonValidationError(ApiErrorMessages.invalidEoriCount(appConfig.eoriMax)))))
              )
            )

          val request = fakeRequestWithJsonBody(json)

          val response = service.validateRequest(request)

          response shouldBe Left(expectedResponse)
        }
      }

      "when AuthorisationRequest has no Eoris" in {
        forAll { (noEorisRequest: NoEorisAuthorisationRequest) =>
          val json = Json.toJson(noEorisRequest.request)

          val expectedResponse =
            ValidationDataRetrievalError(
              JsError(
                Seq((JsPath \ JsonPaths.eoris, Seq(JsonValidationError(ApiErrorMessages.invalidEoriCount(appConfig.eoriMax)))))
              )
            )

          val request = fakeRequestWithJsonBody(json)

          val response = service.validateRequest(request)

          response shouldBe Left(expectedResponse)
        }
      }
    }

  }
}

class ValidationServiceOverwrittenMaxEoriSpec extends BaseSpec {

  private lazy val maxEori = 10

  override def configOverrides: Map[String, Any] = Map(
    "microservice.services.self.eoriMax" -> maxEori
  )

  private val service = new ValidationService(minMaxValues)

  "validationRequest with overwritten config" should {

    "must return JSError with different content depending on maxEori" when {

      "AuthorisationRequest has too many Eoris" in {
        forAll { (tooManyEorisRequest: TooManyEorisAuthorisationRequest) =>
          val json = Json.toJson(tooManyEorisRequest.request)

          val expectedResponse =
            ValidationDataRetrievalError(
              JsError(
                Seq(
                  (
                    JsPath \ JsonPaths.eoris,
                    Seq(JsonValidationError(ApiErrorMessages.invalidEoriCount(maxEori)))
                  )
                )
              )
            )

          val request = fakeRequestWithJsonBody(json)

          val response = service.validateRequest(request)

          response shouldBe Left(expectedResponse)
        }
      }

      "when AuthorisationRequest has no Eoris" in {
        forAll { (noEorisRequest: NoEorisAuthorisationRequest) =>
          val json = Json.toJson(noEorisRequest.request)

          val expectedResponse =
            ValidationDataRetrievalError(
              JsError(
                Seq(
                  (
                    JsPath \ JsonPaths.eoris,
                    Seq(JsonValidationError(ApiErrorMessages.invalidEoriCount(maxEori)))
                  )
                )
              )
            )

          val request = fakeRequestWithJsonBody(json)

          val response = service.validateRequest(request)

          response shouldBe Left(expectedResponse)
        }
      }
    }
  }
}
