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

import java.time.ZonedDateTime
import scala.concurrent.Future

import com.google.inject.AbstractModule
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

import play.api.http.Status._
import play.api.libs.json.{JsError, JsPath, Json, JsonValidationError}
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{BadGatewayException, ServiceUnavailableException, UpstreamErrorResponse}
import uk.gov.hmrc.uknwauthcheckerapi.connectors.IntegrationFrameworkConnector
import uk.gov.hmrc.uknwauthcheckerapi.controllers.BaseSpec
import uk.gov.hmrc.uknwauthcheckerapi.errors.DataRetrievalError
import uk.gov.hmrc.uknwauthcheckerapi.errors.DataRetrievalError._
import uk.gov.hmrc.uknwauthcheckerapi.generators._
import uk.gov.hmrc.uknwauthcheckerapi.models._
import uk.gov.hmrc.uknwauthcheckerapi.models.constants.{JsonErrorMessages, JsonPaths}
import uk.gov.hmrc.uknwauthcheckerapi.models.eis._

class IntegrationFrameworkServiceSpec extends BaseSpec {

  private lazy val service: IntegrationFrameworkService = injected[IntegrationFrameworkService]

  override def moduleOverrides: AbstractModule = new AbstractModule {
    override def configure(): Unit =
      bind(classOf[IntegrationFrameworkConnector]).toInstance(mockIntegrationFrameworkConnector)
  }

  trait TestContext {
    def doTest(
      request:     AuthorisationRequest,
      eisResponse: Future[EisAuthorisationsResponse],
      response:    Either[DataRetrievalError, AuthorisationsResponse]
    ): Assertion = {

      when(mockIntegrationFrameworkConnector.getEisAuthorisationsResponse(any())(any()))
        .thenReturn(eisResponse)

      val result = await(service.getAuthorisations(request).value)

      result shouldBe response
    }
  }

  "getEisAuthorisations" should {
    "return EisAuthorisationsResponse when call to the integration framework succeeds" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest, dateTime: ZonedDateTime) =>
        val request = validRequest.request

        val expectedEisResponse = EisAuthorisationsResponse(
          dateTime,
          appConfig.authType,
          request.eoris.map(r => EisAuthorisationResponse(r, valid = true, 0))
        )

        val expectedResponse = AuthorisationsResponse(
          dateTime,
          request.eoris.map(r => AuthorisationResponse(r, authorised = true))
        )

        doTest(
          request = request,
          eisResponse = Future.successful(expectedEisResponse),
          response = Right(expectedResponse)
        )
      }
    }

    "return BadGatewayRetrievalError error when call to the integration framework fails with BAD_GATEWAY via exception" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest, errorMessage: String) =>
        val request = validRequest.request

        val expectedEisResponse: BadGatewayException          = new BadGatewayException(errorMessage)
        val expectedResponse:    BadGatewayDataRetrievalError = BadGatewayDataRetrievalError()

        doTest(
          request = request,
          eisResponse = Future.failed(expectedEisResponse),
          response = Left(expectedResponse)
        )
      }
    }

    "return BadGatewayRetrievalError error when call to the integration framework fails with BAD_GATEWAY via UpstreamErrorResponse" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest, errorMessage: String) =>
        val request = validRequest.request

        val expectedEisResponse: UpstreamErrorResponse        = UpstreamErrorResponse(TestConstants.emptyJson, BAD_GATEWAY)
        val expectedResponse:    BadGatewayDataRetrievalError = BadGatewayDataRetrievalError()

        doTest(
          request = request,
          eisResponse = Future.failed(expectedEisResponse),
          response = Left(expectedResponse)
        )
      }
    }

    "return BadRequestDataRetrievalError error when call to the integration framework fails with a BAD_REQUEST" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest, eisErrorResponse: EisAuthorisationResponseError) =>
        val request = validRequest.request

        val eisError = eisErrorResponse.copy(errorDetail =
          eisErrorResponse.errorDetail
            .copy(
              errorCode = BAD_REQUEST,
              errorMessage = TestConstants.invalidEorisEisErrorMessage
            )
        )

        val expectedEisResponse: UpstreamErrorResponse        = UpstreamErrorResponse(Json.stringify(Json.toJson(eisError)), BAD_REQUEST)
        val expectedResponse:    BadRequestDataRetrievalError = BadRequestDataRetrievalError(TestConstants.invalidEorisEisErrorMessage)

        doTest(
          request = request,
          eisResponse = Future.failed(expectedEisResponse),
          response = Left(expectedResponse)
        )
      }
    }

    "return ForbiddenDataRetrievalError error when call to the integration framework fails with a FORBIDDEN" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        val request = validRequest.request

        val expectedEisResponse: UpstreamErrorResponse       = UpstreamErrorResponse(TestConstants.emptyJson, FORBIDDEN)
        val expectedResponse:    ForbiddenDataRetrievalError = ForbiddenDataRetrievalError()

        doTest(
          request = request,
          eisResponse = Future.failed(expectedEisResponse),
          response = Left(expectedResponse)
        )
      }
    }

    "return InternalUnexpectedDataRetrievalError error when call to the integration framework fails with a non fatal error" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest, errorMessage: String) =>
        val request = validRequest.request

        val expectedEisResponse: Exception = new Exception(errorMessage)
        val expectedResponse: InternalUnexpectedDataRetrievalError =
          InternalUnexpectedDataRetrievalError(expectedEisResponse.getMessage, expectedEisResponse)

        doTest(
          request = request,
          eisResponse = Future.failed(expectedEisResponse),
          response = Left(expectedResponse)
        )
      }
    }

    "return InternalServerDataRetrievalError error when call to the integration framework fails with a INTERNAL_SERVER_ERROR" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest, eisErrorResponse: EisAuthorisationResponseError) =>
        val request = validRequest.request

        val eisError = eisErrorResponse.copy(errorDetail =
          eisErrorResponse.errorDetail
            .copy(errorCode = INTERNAL_SERVER_ERROR)
        )

        val expectedEisResponse: UpstreamErrorResponse = UpstreamErrorResponse(Json.stringify(Json.toJson(eisError)), INTERNAL_SERVER_ERROR)
        val expectedResponse: InternalServerDataRetrievalError = InternalServerDataRetrievalError(eisError.errorDetail.errorMessage)

        doTest(
          request = request,
          eisResponse = Future.failed(expectedEisResponse),
          response = Left(expectedResponse)
        )
      }
    }

    "return InternalServerDataRetrievalError error when call to the integration framework fails with invalid auth type error" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest, eisErrorResponse: EisAuthorisationResponseError) =>
        val request = validRequest.request

        val eisError = eisErrorResponse.copy(errorDetail =
          eisErrorResponse.errorDetail
            .copy(
              errorCode = BAD_REQUEST,
              errorMessage = TestConstants.invalidAuthTypeEisErrorMessage
            )
        )

        val expectedEisResponse = UpstreamErrorResponse(Json.stringify(Json.toJson(eisError)), BAD_REQUEST)
        val expectedResponse    = InternalServerDataRetrievalError(TestConstants.invalidAuthTypeErrorMessage)

        doTest(
          request = request,
          eisResponse = Future.failed(expectedEisResponse),
          response = Left(expectedResponse)
        )
      }
    }

    "return InternalServerDataRetrievalError error when call to the integration framework fails with a unmanaged status code" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest, eisErrorResponse: EisAuthorisationResponseError) =>
        val request = validRequest.request

        val eisError = eisErrorResponse.copy(errorDetail =
          eisErrorResponse.errorDetail
            .copy(errorCode = IM_A_TEAPOT)
        )

        val expectedEisResponse: UpstreamErrorResponse            = UpstreamErrorResponse(Json.stringify(Json.toJson(eisError)), IM_A_TEAPOT)
        val expectedResponse:    InternalServerDataRetrievalError = InternalServerDataRetrievalError(eisError.errorDetail.errorMessage)

        doTest(
          request = request,
          eisResponse = Future.failed(expectedEisResponse),
          response = Left(expectedResponse)
        )
      }
    }

    "return ServiceUnavailableRetrievalError error when call to the integration framework fails with SERVICE_UNAVAILABLE via exception" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest, errorMessage: String) =>
        val request = validRequest.request

        val expectedEisResponse: ServiceUnavailableException          = new ServiceUnavailableException(errorMessage)
        val expectedResponse:    ServiceUnavailableDataRetrievalError = ServiceUnavailableDataRetrievalError()

        doTest(
          request = request,
          eisResponse = Future.failed(expectedEisResponse),
          response = Left(expectedResponse)
        )
      }
    }

    "return ServiceUnavailableRetrievalError error when call to the integration framework fails with SERVICE_UNAVAILABLE via UpstreamErrorResponse" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest, errorMessage: String) =>
        val request = validRequest.request

        val expectedEisResponse: UpstreamErrorResponse                = UpstreamErrorResponse(TestConstants.emptyJson, SERVICE_UNAVAILABLE)
        val expectedResponse:    ServiceUnavailableDataRetrievalError = ServiceUnavailableDataRetrievalError()

        doTest(
          request = request,
          eisResponse = Future.failed(expectedEisResponse),
          response = Left(expectedResponse)
        )
      }
    }

    "return UnableToDeserialiseDataRetrievalError error when call to the integration framework fails with unvalidated json" in new TestContext {
      forAll { (validRequest: ValidAuthorisationRequest) =>
        val request = validRequest.request

        val jsError = JsError(
          Seq((JsPath \ JsonPaths.errorDetail, Seq(JsonValidationError(JsonErrorMessages.pathMissing))))
        )

        val expectedEisResponse: UpstreamErrorResponse                 = UpstreamErrorResponse(TestConstants.emptyJson, BAD_REQUEST)
        val expectedResponse:    UnableToDeserialiseDataRetrievalError = UnableToDeserialiseDataRetrievalError(jsError)

        doTest(
          request = request,
          eisResponse = Future.failed(expectedEisResponse),
          response = Left(expectedResponse)
        )
      }
    }
  }
}
