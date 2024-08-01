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

import scala.concurrent.Future

import org.scalatest.Assertion

import play.api.http.Status._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.http.{HttpVerbs, NotFoundException}
import uk.gov.hmrc.uknwauthcheckerapi.controllers.BaseSpec
import uk.gov.hmrc.uknwauthcheckerapi.generators.TestConstants

class ApiErrorHandlerSpec extends BaseSpec {

  private lazy val apiErrorHandler = injected[ApiErrorHandler]

  trait TestContext {
    def doTestClient(statusCode: Int, response: ApiErrorResponse, request: FakeRequest[AnyContentAsEmpty.type] = fakePostRequest): Assertion = {
      val result = apiErrorHandler.onClientError(request, statusCode, TestConstants.emptyString)

      status(result) shouldEqual statusCode
      contentAsJson(result) shouldEqual contentAsJson(Future.successful(response.toResult))
    }

    def doTestServer(statusCode: Int, throwable: Throwable, response: ApiErrorResponse): Assertion = {
      val result = apiErrorHandler.onServerError(fakePostRequest, throwable)

      status(result) shouldEqual statusCode
      contentAsJson(result) shouldEqual contentAsJson(Future.successful(response.toResult))
    }
  }

  "onClientError" should {
    "convert a FORBIDDEN to Forbidden (403) response" in new TestContext {
      doTestClient(
        statusCode = FORBIDDEN,
        response = ForbiddenApiError
      )
    }

    "convert a INTERNAL_SERVER_ERROR to Internal Server Error (500) response" in new TestContext {
      doTestClient(
        statusCode = INTERNAL_SERVER_ERROR,
        response = InternalServerApiError
      )
    }

    "convert a METHOD_NOT_ALLOWED to Method Not Allowed (405) response" in new TestContext {
      doTestClient(
        statusCode = METHOD_NOT_ALLOWED,
        response = MethodNotAllowedApiError
      )
    }

    "convert a NOT_FOUND to Not Found (404) response" in new TestContext {
      doTestClient(
        statusCode = NOT_FOUND,
        response = NotFoundApiError
      )
    }

    "convert a NOT_FOUND with /authorisations url to to Method Not Allowed (405) response" in new TestContext {
      doTestClient(
        statusCode = METHOD_NOT_ALLOWED,
        response = MethodNotAllowedApiError,
        request = FakeRequest(HttpVerbs.POST, "/authorisations")
      )
    }

    "convert a SERVICE_UNAVAILABLE to Service Unavailable (503) response" in new TestContext {
      doTestClient(
        statusCode = SERVICE_UNAVAILABLE,
        response = ServiceUnavailableApiError
      )
    }
  }

  "onServerError" should {
    "convert a NotFoundException to Not Found response" in new TestContext {
      val notfoundException = new NotFoundException(TestConstants.emptyString)

      doTestServer(
        statusCode = NOT_FOUND,
        throwable = notfoundException,
        response = NotFoundApiError
      )
    }

    "convert a RuntimeException to Internal Server Error response" in new TestContext {
      val runtimeException = new RuntimeException(TestConstants.emptyString)

      doTestServer(
        statusCode = INTERNAL_SERVER_ERROR,
        throwable = runtimeException,
        response = InternalServerApiError
      )
    }
  }
}
