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

sealed trait ApiErrorResponse {
  def statusCode: Int
  def code:       String
  def message:    String

  private def convertErrorsToReadableFormat: JsValue =
    this match {
      case badRequestError: BadRequestApiError     => Json.toJson(badRequestError)(ApiErrorResponse.badRequestApiErrorWrites)
      case validationError: JsonValidationApiError => Json.toJson(validationError)(ApiErrorResponse.jsonValidationApiErrorWrites)
      case _ => Json.toJson(this)
    }

  def toResult: Result = Status(statusCode)(Json.toJson(convertErrorsToReadableFormat))
}

object ApiErrorResponse {
  implicit val jsonValidationApiErrorWrites: Writes[JsonValidationApiError] = Writes { model =>
    Json.obj(
      "code"    -> model.code,
      "message" -> model.message,
      "errors"  -> model.getErrors
    )
  }

  implicit val badRequestApiErrorWrites: Writes[BadRequestApiError] = Writes { model =>
    Json.obj(
      "code"    -> model.code,
      "message" -> model.message,
      "errors"  -> model.getErrors
    )
  }

  implicit val writes: Writes[ApiErrorResponse] = (o: ApiErrorResponse) => JsObject(Seq("code" -> JsString(o.code), "message" -> JsString(o.message)))
}

case object ForbiddenApiError extends ApiErrorResponse {
  val statusCode: Int    = FORBIDDEN
  val code:       String = "FORBIDDEN"
  val message:    String = "You are not allowed to access this resource"
}

case object InternalServerApiError extends ApiErrorResponse {
  val statusCode: Int    = INTERNAL_SERVER_ERROR
  val code:       String = "INTERNAL_SERVER_ERROR"
  val message:    String = "Unexpected internal server error"
}

case object NotFoundApiError extends ApiErrorResponse {
  val statusCode: Int    = NOT_FOUND
  val code:       String = "MATCHING_RESOURCE_NOT_FOUND"
  val message:    String = "Matching resource not found"
}

case object MethodNotAllowedApiError extends ApiErrorResponse {
  val statusCode: Int    = METHOD_NOT_ALLOWED
  val code:       String = "METHOD_NOT_ALLOWED"
  val message:    String = "This method is not supported"
}

case object NotAcceptableApiError extends ApiErrorResponse {
  val statusCode: Int    = NOT_ACCEPTABLE
  val code:       String = "NOT_ACCEPTABLE"
  val message:    String = "Cannot produce an acceptable response. The Accept or Content-Type header is missing or invalid"
}

case object ServiceUnavailableApiError extends ApiErrorResponse {
  val statusCode: Int    = SERVICE_UNAVAILABLE
  val code:       String = "SERVICE_UNAVAILABLE"
  val message:    String = "Server is currently unable to handle the incoming requests"
}

case object UnauthorisedApiError extends ApiErrorResponse {
  val statusCode: Int    = UNAUTHORIZED
  val code:       String = "MISSING_CREDENTIALS"
  val message:    String = "Authentication information is not provided"
}

final case class BadRequestApiError(errorMessages: String) extends ApiErrorResponse with BadRequestErrorTransformer {
  val statusCode: Int    = BAD_REQUEST
  val code:       String = "BAD_REQUEST"
  val message:    String = "Invalid request"

  val getErrors: JsValue = transformBadRequest(errorMessages)
}

final case class JsonValidationApiError(jsErrors: JsError) extends ApiErrorResponse with JsErrorTransformer {
  val statusCode: Int    = BAD_REQUEST
  val code:       String = "BAD_REQUEST"
  val message:    String = "Bad request"

  val getErrors: JsValue = transformJsErrors(jsErrors)
}
