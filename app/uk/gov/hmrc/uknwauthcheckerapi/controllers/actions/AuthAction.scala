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

package uk.gov.hmrc.uknwauthcheckerapi.controllers.actions

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.Logging
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.authProviderId
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.uknwauthcheckerapi.errors.ApiErrorResponses.ServiceUnavailableApiError
import uk.gov.hmrc.uknwauthcheckerapi.errors.UnauthorizedApiError
import uk.gov.hmrc.uknwauthcheckerapi.models.constants.ApiErrorMessages
import uk.gov.hmrc.uknwauthcheckerapi.services.ZonedDateTimeService

@Singleton
class AuthAction @Inject() (ac: AuthConnector)(using ec: ExecutionContext, zs: ZonedDateTimeService) extends ActionFilter[Request], Logging {
  private val auth = new AuthorisedFunctions {
    def authConnector: AuthConnector = ac
  }

  override def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    auth
      .authorised()
      .retrieve(authProviderId) { authProviderId =>
        logger.warn(s"[AuthAction][filter] Auth Provider ID: ${authProviderId.toString}")
        Future.successful(None)
      }
      .recover {
        case exception: InternalError =>
          logger.error(s"[AuthAction][filter] Authorization failed with Internal Error", exception)
          Some(ServiceUnavailableApiError.toResult)
        case exception: AuthorisationException =>
          logger.warn(s"[AuthAction][filter] Authorization failed.", exception)
          Some(UnauthorizedApiError(ApiErrorMessages.unauthorized).toResult)
        case exception =>
          logger.warn(s"[AuthAction][filter] Authorization request failed with unexpected exception: $exception")
          Some(ServiceUnavailableApiError.toResult)
      }
  }

  override protected def executionContext: ExecutionContext = ec
}
