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

package uk.gov.hmrc.uknwauthcheckerapi.controllers.responses

import scala.concurrent.{ExecutionContext, Future}

import cats.data.EitherT

import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.JsError.toJson
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Request, Result, Results}
import uk.gov.hmrc.uknwauthcheckerapi.errors.ApiErrorResponses._
import uk.gov.hmrc.uknwauthcheckerapi.errors.DataRetrievalError._
import uk.gov.hmrc.uknwauthcheckerapi.errors._
import uk.gov.hmrc.uknwauthcheckerapi.models.AuthorisationsResponse
import uk.gov.hmrc.uknwauthcheckerapi.models.constants.CustomHeaderNames
import uk.gov.hmrc.uknwauthcheckerapi.services.ZonedDateTimeService

trait AuthorisationResponseHandler extends Logging {

  extension (response: EitherT[Future, DataRetrievalError, AuthorisationsResponse]) {

    def toResult(using ec: ExecutionContext, request: Request[JsValue], zs: ZonedDateTimeService): Future[Result] =
      response.fold(
        {
          case BadGatewayDataRetrievalError() =>
            logger.error(toLogMessage(BAD_GATEWAY))
            ServiceUnavailableApiError.toResult

          case BadRequestDataRetrievalError(errorMessages) =>
            logger.warn(toLogMessage(BAD_REQUEST, Some(errorMessages)))
            BadRequestApiError(errorMessages).toResult

          case ForbiddenDataRetrievalError() =>
            logger.error(toLogMessage(FORBIDDEN))
            ForbiddenApiError.toResult

          case ValidationDataRetrievalError(errors) =>
            logger.warn(toLogMessage(BAD_REQUEST, Some(Json.stringify(toJson(errors)))))
            JsonValidationApiError(errors).toResult

          case InternalServerDataRetrievalError(errorMessage) =>
            logger.error(toLogMessage(INTERNAL_SERVER_ERROR, Some(errorMessage)))
            InternalServerApiError.toResult

          case ServiceUnavailableDataRetrievalError() =>
            logger.error(toLogMessage(SERVICE_UNAVAILABLE))
            ServiceUnavailableApiError.toResult

          case _ =>
            logger.error(toLogMessage(INTERNAL_SERVER_ERROR))
            InternalServerApiError.toResult
        },
        authorisationsResponse =>
          Results
            .Status(OK)(Json.toJson(authorisationsResponse))
            .withHeaders((CustomHeaderNames.xTimestamp, zs.nowAsIsoUtc8601String()))
      )
  }

  private def toLogMessage(statusCode: Int, errorMessage: Option[String] = None)(using request: Request[JsValue]): String = {
    val logMessage = s"[AuthorisationResponseHandler][toResult] error for (${request.method}) [${request.uri}] with status: $statusCode"
    errorMessage match {
      case Some(message) => s"$logMessage and message $message"
      case None          => logMessage
    }
  }
}
