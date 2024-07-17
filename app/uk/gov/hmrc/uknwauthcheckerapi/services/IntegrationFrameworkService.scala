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

import cats.data.EitherT
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.uknwauthcheckerapi.config.AppConfig
import uk.gov.hmrc.uknwauthcheckerapi.connectors.IntegrationFrameworkConnector
import uk.gov.hmrc.uknwauthcheckerapi.errors.DataRetrievalError
import uk.gov.hmrc.uknwauthcheckerapi.errors.DataRetrievalError._
import uk.gov.hmrc.uknwauthcheckerapi.models.eis.{EisAuthorisationRequest, EisAuthorisationResponseError}
import uk.gov.hmrc.uknwauthcheckerapi.models.{AuthorisationRequest, AuthorisationResponse, AuthorisationsResponse}
import uk.gov.hmrc.uknwauthcheckerapi.utils.CustomRegexes._

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class IntegrationFrameworkService @Inject() (appConfig: AppConfig, integrationFrameworkConnector: IntegrationFrameworkConnector)(implicit
  ec: ExecutionContext
) {

  def getAuthorisations(
    authorisationRequest: AuthorisationRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, DataRetrievalError, AuthorisationsResponse] = {
    val eisAuthorisationRequest = EisAuthorisationRequest(
      Some(LocalDate.parse(authorisationRequest.date)),
      appConfig.authType,
      authorisationRequest.eoris
    )

    EitherT {
      integrationFrameworkConnector
        .getEisAuthorisationsResponse(eisAuthorisationRequest)
        .map { authorisationsResponse =>
          Right(
            AuthorisationsResponse(
              authorisationsResponse.processingDate,
              authorisationsResponse.results
                .map(authorisationResponse =>
                  AuthorisationResponse(
                    authorisationResponse.eori,
                    authorisationResponse.valid
                  )
                )
            )
          )
        }
        .recover {
          case _: BadGatewayException => Left(BadGatewayDataRetrievalError())
          case _ @UpstreamErrorResponse(_, BAD_GATEWAY, _, _) => Left(BadGatewayDataRetrievalError())
          case _ @UpstreamErrorResponse(_, FORBIDDEN, _, _)   => Left(ForbiddenDataRetrievalError())
          case _ @UpstreamErrorResponse(body, _, _, _)        => handleUpstreamErrorResponse(body)
          case NonFatal(thr)                                  => Left(InternalUnexpectedDataRetrievalError(thr.getMessage, thr))
        }
    }
  }

  private def handleUpstreamErrorResponse(body: String): Either[DataRetrievalError, AuthorisationsResponse] =
    Json
      .parse(body)
      .validate[EisAuthorisationResponseError] match {
      case JsSuccess(error, _) =>
        val errorCode    = error.errorDetail.errorCode
        val errorMessage = error.errorDetail.errorMessage

        Left(
          errorCode match {
            case BAD_REQUEST           => handleBadRequest(errorMessage)
            case INTERNAL_SERVER_ERROR => InternalServerDataRetrievalError(errorMessage)
            case METHOD_NOT_ALLOWED    => MethodNotAllowedDataRetrievalError(errorMessage)
            case _                     => InternalServerDataRetrievalError(errorMessage)
          }
        )
      case jsError: JsError => Left(UnableToDeserialiseDataRetrievalError(jsError))
    }

  private def handleBadRequest(errorMessage: String): DataRetrievalError =
    errorMessage match {
      case invalidAuthTypePatternRegex(_) => InternalServerDataRetrievalError(s"Invalid auth type ${appConfig.authType}")
      case message                        => BadRequestDataRetrievalError(message)
    }
}
