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

import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.Future

import cats.data.EitherT
import com.google.inject.AbstractModule
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.Assertion
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

import play.api.libs.json._
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.uknwauthcheckerapi.errors.DataRetrievalError._
import uk.gov.hmrc.uknwauthcheckerapi.errors._
import uk.gov.hmrc.uknwauthcheckerapi.generators._
import uk.gov.hmrc.uknwauthcheckerapi.models._
import uk.gov.hmrc.uknwauthcheckerapi.models.constants._
import uk.gov.hmrc.uknwauthcheckerapi.services._

class AuthorisationsControllerSpec extends BaseSpec {

  private lazy val controller = injected[AuthorisationsController]

  override def moduleOverrides: AbstractModule = new AbstractModule {
    override def configure(): Unit = {
      bind(classOf[AuthConnector]).toInstance(mockAuthConnector)
      bind(classOf[IntegrationFrameworkService]).toInstance(mockIntegrationFrameworkService)
      bind(classOf[ValidationService]).toInstance(mockValidationService)
      bind(classOf[LocalDateService]).toInstance(mockLocalDateService)
    }
  }

  override protected def beforeAll(): Unit = {
    stubAuthorization()
    super.beforeAll()
  }

  trait TestContext {
    def doTest(
      validateResponse:       Option[Either[DataRetrievalError, AuthorisationRequest]] = None,
      authorisationsResponse: Option[EitherT[Future, DataRetrievalError, AuthorisationsResponse]] = None,
      requestBody:            JsValue,
      statusCode:             Int,
      expectedResponse:       JsValue,
      headers:                Seq[(String, String)] = defaultHeaders
    ): Assertion = {

      reset(mockLocalDateService)
      reset(mockValidationService)
      reset(mockIntegrationFrameworkService)

      when(mockLocalDateService.now()).thenReturn(LocalDate.now)

      validateResponse match {
        case Some(response) =>
          when(
            mockValidationService
              .validateRequest(any())
          )
            .thenReturn(response)
        case None => ()
      }

      authorisationsResponse match {
        case Some(response) =>
          when(
            mockIntegrationFrameworkService
              .getAuthorisations(any())(any())
          )
            .thenReturn(response)
        case None => ()
      }

      val request = fakeRequestWithJsonBody(requestBody, headers = headers)

      val result = controller.authorisations()(request)

      status(result)        shouldBe statusCode
      contentAsJson(result) shouldBe Json.toJson(expectedResponse)
    }
  }

  "AuthorisationsController" should {
    "return OK (200) with authorised eoris when request has valid eoris" in new TestContext {
      forAll { (authorisationRequest: AuthorisationRequest, dateTime: ZonedDateTime) =>
        val expectedResponse = AuthorisationsResponse(
          dateTime,
          authorisationRequest.eoris.map(r => AuthorisationResponse(r, authorised = true))
        )

        doTest(
          validateResponse = Some(Right(authorisationRequest)),
          authorisationsResponse = Some(EitherT.rightT(expectedResponse)),
          requestBody = Json.toJson(authorisationRequest),
          statusCode = OK,
          expectedResponse = Json.toJson(expectedResponse)
        )
      }
    }

    "return BAD_REQUEST (400) error when request json field is missing" in new TestContext {
      val jsError: JsError = JsError(
        Seq("date", "eoris").map { field =>
          (JsPath \ field, Seq(JsonValidationError(JsonErrorMessages.pathMissing)))
        }
      )

      val expectedResponse: JsValue = Json.toJson(
        JsonValidationApiError(jsError)
      )(ApiErrorResponse.jsonValidationApiErrorWrites)

      doTest(
        validateResponse = Some(Left(ValidationDataRetrievalError(jsError))),
        requestBody = Json.toJson(emptyJson),
        statusCode = BAD_REQUEST,
        expectedResponse = Json.toJson(expectedResponse)
      )
    }

    "return BAD_REQUEST (400) error when request json is malformed" in new TestContext {
      val jsError: JsError = JsError(
        Seq((JsPath \ "", Seq(JsonValidationError(JsonErrorMessages.expectedJsObject))))
      )

      val expectedResponse: JsValue = Json.toJson(
        JsonValidationApiError(jsError)
      )(ApiErrorResponse.jsonValidationApiErrorWrites)

      doTest(
        validateResponse = Some(Left(ValidationDataRetrievalError(jsError))),
        requestBody = Json.toJson(emptyJson),
        statusCode = BAD_REQUEST,
        expectedResponse = Json.toJson(expectedResponse)
      )
    }

    "return BAD_REQUEST (400) error when request error is custom" in new TestContext {
      val jsError: JsError = JsError(
        Seq((JsPath \ "", Seq(JsonValidationError("test error"))))
      )

      val expectedResponse: JsValue = Json.toJson(
        JsonValidationApiError(jsError)
      )(ApiErrorResponse.jsonValidationApiErrorWrites)

      doTest(
        validateResponse = Some(Left(ValidationDataRetrievalError(jsError))),
        requestBody = Json.toJson(emptyJson),
        statusCode = BAD_REQUEST,
        expectedResponse = Json.toJson(expectedResponse)
      )
    }
  }

  "return BAD_REQUEST (400) error when getAuthorisations call has eori errors" in new TestContext {
    forAll { (validRequest: ValidAuthorisationRequest) =>
      val authorisationRequest: AuthorisationRequest = validRequest.request

      val error = BadRequestDataRetrievalError(TestConstants.invalidEorisEisErrorMessage)

      val expectedResponse = Json.toJson(
        BadRequestApiError(TestConstants.invalidEorisEisErrorMessage)
      )(ApiErrorResponse.badRequestApiErrorWrites)

      doTest(
        validateResponse = Some(Right(authorisationRequest)),
        authorisationsResponse = Some(EitherT.leftT(error)),
        requestBody = Json.toJson(authorisationRequest),
        statusCode = BAD_REQUEST,
        expectedResponse = Json.toJson(expectedResponse)
      )
    }
  }

  "return SERVICE_UNAVAILABLE (503) when integration framework service returns BadGatewayDataRetrievalError" in new TestContext {
    forAll { (authorisationRequest: AuthorisationRequest) =>
      val expectedResponse = Json.toJson(
        ServiceUnavailableApiError
      )(ApiErrorResponse.writes.writes)

      doTest(
        validateResponse = Some(Right(authorisationRequest)),
        authorisationsResponse = Some(EitherT.leftT(BadGatewayDataRetrievalError())),
        requestBody = Json.toJson(authorisationRequest),
        statusCode = SERVICE_UNAVAILABLE,
        expectedResponse = Json.toJson(expectedResponse)
      )
    }
  }

  "return BAD_REQUEST (400) with authorised eoris when request has valid eoris but they exceed the maximum eoris" in new TestContext {

    forAll { authorisationRequest: TooManyEorisAuthorisationRequest =>
      val jsError = JsError(JsPath \ "eoris", JsonValidationError(ApiErrorMessages.invalidEoriCount))

      val expectedResponse = Json.toJson(
        JsonValidationApiError(jsError)
      )(ApiErrorResponse.jsonValidationApiErrorWrites)

      doTest(
        validateResponse = Some(Left(ValidationDataRetrievalError(jsError))),
        requestBody = Json.toJson(authorisationRequest.request),
        statusCode = BAD_REQUEST,
        expectedResponse = Json.toJson(expectedResponse)
      )
    }
  }

  "return BAD_REQUEST (400) when request has no eoris" in new TestContext {

    forAll { authorisationRequest: NoEorisAuthorisationRequest =>
      val jsError = JsError(JsPath \ "eoris", JsonValidationError(ApiErrorMessages.invalidEoriCount))

      val expectedResponse = Json.toJson(
        JsonValidationApiError(jsError)
      )(ApiErrorResponse.jsonValidationApiErrorWrites)

      doTest(
        validateResponse = Some(Left(ValidationDataRetrievalError(jsError))),
        requestBody = Json.toJson(authorisationRequest.request),
        statusCode = BAD_REQUEST,
        expectedResponse = Json.toJson(expectedResponse)
      )
    }
  }

  "return FORBIDDEN (403) when integration framework service returns ForbiddenDataRetrievalError" in new TestContext {
    forAll { (authorisationRequest: AuthorisationRequest) =>
      val expectedResponse = Json.toJson(
        ForbiddenApiError
      )(ApiErrorResponse.writes.writes)

      doTest(
        validateResponse = Some(Right(authorisationRequest)),
        authorisationsResponse = Some(EitherT.leftT(ForbiddenDataRetrievalError())),
        requestBody = Json.toJson(authorisationRequest),
        statusCode = FORBIDDEN,
        expectedResponse = Json.toJson(expectedResponse)
      )
    }
  }

  "return METHOD_NOT_ALLOWED (405) when integration framework service returns MethodNotAllowedDataRetrievalError" in new TestContext {
    forAll { (authorisationRequest: AuthorisationRequest, errorMessage: String) =>
      val expectedResponse = Json.toJson(
        MethodNotAllowedApiError
      )(ApiErrorResponse.writes.writes)

      doTest(
        validateResponse = Some(Right(authorisationRequest)),
        authorisationsResponse = Some(EitherT.leftT(MethodNotAllowedDataRetrievalError(errorMessage))),
        requestBody = Json.toJson(authorisationRequest),
        statusCode = METHOD_NOT_ALLOWED,
        expectedResponse = Json.toJson(expectedResponse)
      )
    }
  }

  "return INTERNAL_SERVER_ERROR (500) when integration framework service returns InternalServerDataRetrievalError" in new TestContext {
    forAll { (authorisationRequest: AuthorisationRequest, errorMessage: String) =>
      val expectedResponse = Json.toJson(
        InternalServerApiError
      )(ApiErrorResponse.writes.writes)

      doTest(
        validateResponse = Some(Right(authorisationRequest)),
        authorisationsResponse = Some(EitherT.leftT(InternalServerDataRetrievalError(errorMessage))),
        requestBody = Json.toJson(authorisationRequest),
        statusCode = INTERNAL_SERVER_ERROR,
        expectedResponse = Json.toJson(expectedResponse)
      )
    }
  }

  "return INTERNAL_SERVER_ERROR (500) when integration framework service returns InternalUnexpectedDataRetrievalError" in new TestContext {
    forAll { (authorisationRequest: AuthorisationRequest, errorMessage: String) =>
      val expectedResponse = Json.toJson(
        InternalServerApiError
      )(ApiErrorResponse.writes.writes)

      doTest(
        validateResponse = Some(Right(authorisationRequest)),
        authorisationsResponse = Some(EitherT.leftT(InternalUnexpectedDataRetrievalError(errorMessage, new Exception()))),
        requestBody = Json.toJson(authorisationRequest),
        statusCode = INTERNAL_SERVER_ERROR,
        expectedResponse = Json.toJson(expectedResponse)
      )
    }
  }

  "return NOT_ACCEPTABLE (406) error when accept header is not present" in new TestContext {
    forAll { authorisationRequest: AuthorisationRequest =>
      val expectedResponse = Json.toJson(
        NotAcceptableApiError
      )(ApiErrorResponse.writes.writes)

      val headers = defaultHeaders.filterNot(_._1.equals(acceptHeader._1))

      doTest(
        requestBody = Json.toJson(authorisationRequest),
        statusCode = NOT_ACCEPTABLE,
        expectedResponse = expectedResponse,
        headers = headers
      )
    }
  }

  "return NOT_ACCEPTABLE (406) error when content type header is not present" in new TestContext {
    forAll { authorisationRequest: AuthorisationRequest =>
      val expectedResponse = Json.toJson(
        NotAcceptableApiError
      )(ApiErrorResponse.writes.writes)

      val headers = defaultHeaders.filterNot(_._1.equals(contentTypeHeader._1))

      doTest(
        requestBody = Json.toJson(authorisationRequest),
        statusCode = NOT_ACCEPTABLE,
        expectedResponse = expectedResponse,
        headers = headers
      )
    }
  }
}
