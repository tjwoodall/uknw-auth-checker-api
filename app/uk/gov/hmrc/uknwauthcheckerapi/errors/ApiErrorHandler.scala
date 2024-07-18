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

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

import play.api.Logging
import play.api.http.HttpErrorHandler
import play.api.http.Status._
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.http.NotFoundException

@Singleton
class ApiErrorHandler @Inject() extends HttpErrorHandler with Logging {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    logger.warn(
      s"[ApiErrorHandler][onClientError] error for (${request.method}) [${request.uri}] with status:" +
        s" $statusCode and message: $message"
    )

    Future.successful(
      statusCode match {
        case FORBIDDEN          => ForbiddenApiError.toResult
        case METHOD_NOT_ALLOWED => MethodNotAllowedApiError.toResult
        case NOT_FOUND =>
          request.path match {
            case "/authorisations" => MethodNotAllowedApiError.toResult
            case _                 => NotFoundApiError.toResult
          }
        case SERVICE_UNAVAILABLE => ServiceUnavailableApiError.toResult
        case _ =>
          logger.warn(s"[ApiErrorHandler][onClientError] Unexpected client error type")
          InternalServerApiError.toResult
      }
    )
  }

  override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] = {
    logger.warn(
      s"[ApiErrorHandler][onServerError] Internal server error for (${request.method}) [${request.uri}] -> ",
      ex
    )

    ex match {
      case _: NotFoundException => Future.successful(NotFoundApiError.toResult)
      case ex =>
        logger.error(s"[ApiErrorHandler][onServerError] Server error due to unexpected exception", ex)
        Future.successful(InternalServerApiError.toResult)
    }
  }
}
