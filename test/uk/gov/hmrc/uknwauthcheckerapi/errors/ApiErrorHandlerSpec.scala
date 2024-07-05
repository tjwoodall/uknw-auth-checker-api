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

package uk.gov.hmrc.uknwauthcheckerapi.errors

import play.api.http.Status._
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.http.HttpVerbs.POST
import uk.gov.hmrc.http.{JsValidationException, NotFoundException}
import uk.gov.hmrc.uknwauthcheckerapi.controllers.BaseSpec

import scala.concurrent.Future

class ApiErrorHandlerSpec extends BaseSpec {

  private val apiErrorHandler = new ApiErrorHandler()
  private val errorMessage    = "ErrorMessage"

  "onClientError" should {
    "convert a BAD_REQUEST to Bad Request (400) response" in {
      val result = apiErrorHandler.onClientError(fakePostRequest, BAD_REQUEST, errorMessage)

      status(result) shouldEqual BAD_REQUEST
      contentAsJson(result) shouldEqual contentAsJson(Future.successful(BadRequestApiError.toResult))
    }

    "convert a FORBIDDEN to Forbidden (403) response" in {
      val result = apiErrorHandler.onClientError(fakePostRequest, FORBIDDEN, errorMessage)

      status(result) shouldEqual FORBIDDEN
      contentAsJson(result) shouldEqual contentAsJson(Future.successful(ForbiddenApiError.toResult))
    }

    "convert a INTERNAL_SERVER_ERROR to Internal Server Error (500) response" in {
      val result = apiErrorHandler.onClientError(fakePostRequest, INTERNAL_SERVER_ERROR, errorMessage)

      status(result) shouldEqual INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldEqual contentAsJson(Future.successful(InternalServerApiError.toResult))
    }

    "convert a METHOD_NOT_ALLOWED to Method Not Allowed (405) response" in {
      val result = apiErrorHandler.onClientError(fakePostRequest, METHOD_NOT_ALLOWED, errorMessage)

      status(result) shouldEqual METHOD_NOT_ALLOWED
      contentAsJson(result) shouldEqual contentAsJson(Future.successful(MethodNotAllowedApiError.toResult))
    }

    "convert a NOT_FOUND to Not Found (404) response" in {
      val result = apiErrorHandler.onClientError(fakePostRequest, NOT_FOUND, errorMessage)

      status(result) shouldEqual NOT_FOUND
      contentAsJson(result) shouldEqual contentAsJson(Future.successful(NotFoundApiError.toResult))
    }

    "convert a NOT_FOUND with /authorisations url to to Method Not Allowed (405) response" in {
      val result = apiErrorHandler.onClientError(FakeRequest(POST, "/authorisations"), NOT_FOUND, errorMessage)

      status(result) shouldEqual METHOD_NOT_ALLOWED
      contentAsJson(result) shouldEqual contentAsJson(Future.successful(MethodNotAllowedApiError.toResult))
    }

    "convert a UNAUTHORIZED to Unauthorized (401) response" in {
      val result = apiErrorHandler.onClientError(fakePostRequest, UNAUTHORIZED, errorMessage)

      status(result) shouldEqual UNAUTHORIZED
      contentAsJson(result) shouldEqual contentAsJson(Future.successful(UnauthorisedApiError.toResult))
    }
  }

  "onServerError" should {
    case class TestAuthorisationException(msg: String = errorMessage) extends AuthorisationException(msg)

    "convert a AuthorisationException to Unauthorized response" in {
      val authorisationException = new TestAuthorisationException

      val result = apiErrorHandler.onServerError(fakePostRequest, authorisationException)

      status(result) shouldEqual UNAUTHORIZED
      contentAsJson(result) shouldEqual contentAsJson(Future.successful(UnauthorisedApiError.toResult))
    }

    "convert a JsonValidationException to a Bad Request" in {
      val jsValidationException = new JsValidationException("method", "url", classOf[String], "errors")

      val result = apiErrorHandler.onServerError(fakePostRequest, jsValidationException)

      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldEqual contentAsJson(Future.successful(BadRequestApiError.toResult))
    }

    "convert a NotFoundException to Not Found response" in {
      val notfoundException = new NotFoundException(errorMessage)

      val result = apiErrorHandler.onServerError(fakePostRequest, notfoundException)

      status(result) shouldEqual NOT_FOUND
      contentAsJson(result) shouldEqual contentAsJson(Future.successful(NotFoundApiError.toResult))
    }

    "convert a RuntimeException to Internal Server Error response" in {
      val runtimeException = new RuntimeException(errorMessage)

      val result = apiErrorHandler.onServerError(fakePostRequest, runtimeException)

      status(result) shouldEqual INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldEqual contentAsJson(Future.successful(InternalServerApiError.toResult))
    }
  }
}
