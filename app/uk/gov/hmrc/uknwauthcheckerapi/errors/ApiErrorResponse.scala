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
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.uknwauthcheckerapi.errors.transformers.{BadRequestErrorTransformer, JsErrorTransformer}
import uk.gov.hmrc.uknwauthcheckerapi.models.constants.{ApiErrorCodes, ApiErrorMessages, CustomHeaderNames, JsonErrorMessages, JsonPaths}
import uk.gov.hmrc.uknwauthcheckerapi.services.ZonedDateTimeService

sealed trait ApiErrorResponse {
  def statusCode: Int
  def code:       String
  def message:    String

  private def convertErrorsToReadableFormat: JsValue =
    this match {
      case badRequestError: BadRequestApiError     => Json.toJson(badRequestError)(using ApiErrorResponse.badRequestApiErrorWrites)
      case validationError: JsonValidationApiError => Json.toJson(validationError)(using ApiErrorResponse.jsonValidationApiErrorWrites)
      case _ => Json.toJson(this)
    }

  def toResult(using zs: ZonedDateTimeService): Result = Status(statusCode)(Json.toJson(convertErrorsToReadableFormat))
    .withHeaders((CustomHeaderNames.xTimestamp, zs.nowAsIsoUtc8601String()))
}

object ApiErrorResponse {
  implicit val jsonValidationApiErrorWrites: Writes[JsonValidationApiError] = Writes { model =>
    Json.obj(
      JsonPaths.code    -> model.code,
      JsonPaths.message -> model.message,
      JsonPaths.errors  -> model.getErrors
    )
  }

  implicit val badRequestApiErrorWrites: Writes[BadRequestApiError] = Writes { model =>
    Json.obj(
      JsonPaths.code    -> model.code,
      JsonPaths.message -> model.message,
      JsonPaths.errors  -> model.getErrors
    )
  }

  implicit val writes: Writes[ApiErrorResponse] = (o: ApiErrorResponse) =>
    JsObject(Seq(JsonPaths.code -> JsString(o.code), JsonPaths.message -> JsString(o.message)))
}

enum ApiErrorResponses(val statusCode: Int, val code: String, val message: String) extends ApiErrorResponse {
  case ForbiddenApiError extends ApiErrorResponses(FORBIDDEN, ApiErrorCodes.forbidden, ApiErrorMessages.forbidden)
  case InternalServerApiError
      extends ApiErrorResponses(INTERNAL_SERVER_ERROR, ApiErrorCodes.internalServerError, ApiErrorMessages.internalServerError)
  case NotAcceptableApiError extends ApiErrorResponses(NOT_ACCEPTABLE, ApiErrorCodes.notAcceptable, ApiErrorMessages.notAcceptable)
  case NotFoundApiError extends ApiErrorResponses(NOT_FOUND, ApiErrorCodes.matchingResourceNotFound, ApiErrorMessages.matchingResourceNotFound)
  case RequestEntityTooLargeApiError
      extends ApiErrorResponses(REQUEST_ENTITY_TOO_LARGE, ApiErrorCodes.requestEntityTooLarge, ApiErrorMessages.requestEntityTooLarge)
  case ServiceUnavailableApiError
      extends ApiErrorResponses(SERVICE_UNAVAILABLE, ApiErrorCodes.serviceUnavailable, ApiErrorMessages.serviceUnavailable)
}

final case class UnauthorizedApiError(reason: String) extends ApiErrorResponse {
  val statusCode: Int    = UNAUTHORIZED
  val code:       String = ApiErrorCodes.unauthorized
  val message:    String = ApiErrorMessages.unauthorized
}

final case class BadRequestApiError(errorMessages: String) extends ApiErrorResponse, BadRequestErrorTransformer {
  val statusCode: Int    = BAD_REQUEST
  val code:       String = ApiErrorCodes.badRequest
  val message:    String = ApiErrorMessages.invalidRequest

  val getErrors: JsValue = transformBadRequest(errorMessages)
}

final case class JsonValidationApiError(jsErrors: JsError) extends ApiErrorResponse, JsErrorTransformer {
  val statusCode: Int    = BAD_REQUEST
  val code:       String = ApiErrorCodes.badRequest
  val message:    String = ApiErrorMessages.badRequest

  val getErrors: JsValue = transformJsErrors(jsErrors)
}

object JsonValidationApiError {
  def jsonStructureError: JsonValidationApiError = JsonValidationApiError(JsError(JsonErrorMessages.jsonStructureIncorrect))
}
