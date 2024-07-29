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

import scala.concurrent.Future

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import play.api.test.Helpers.await
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{InternalError, MissingBearerToken}
import uk.gov.hmrc.uknwauthcheckerapi.controllers.actions.AuthAction
import uk.gov.hmrc.uknwauthcheckerapi.errors.{ServiceUnavailableApiError, UnauthorizedApiError}
import uk.gov.hmrc.uknwauthcheckerapi.utils.ErrorMessages

class AuthActionSpec extends BaseSpec {

  private lazy val authAction = injected[AuthAction]

  "AuthAction" should {
    "allow a request through if authorized" in {
      stubAuthorization()

      val result = await(authAction.filter(fakePostRequest))

      result shouldBe None
    }

    "return ServiceUnavailableApiError when authorised returns an InternalError" in {

      when(mockAuthConnector.authorise[Credentials](any(), any())(any(), any()))
        .thenReturn(Future.failed(InternalError()))

      val result = await(authAction.filter(fakePostRequest))

      result shouldBe Some(ServiceUnavailableApiError.toResult)
    }

    "return UnauthorizedApiError when authorised returns an AuthorisationException/MissingBearerToken" in {

      when(mockAuthConnector.authorise[Credentials](any(), any())(any(), any()))
        .thenReturn(Future.failed(MissingBearerToken()))

      val result = await(authAction.filter(fakePostRequest))

      result shouldBe Some(UnauthorizedApiError(ErrorMessages.unauthorized).toResult)
    }

    "return ServiceUnavailableApiError when authorised returns an unexpected exception" in {

      when(mockAuthConnector.authorise[Credentials](any(), any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException()))

      val result = await(authAction.filter(fakePostRequest))

      result shouldBe Some(ServiceUnavailableApiError.toResult)
    }
  }
}
