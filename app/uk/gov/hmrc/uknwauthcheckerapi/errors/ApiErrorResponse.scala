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
import play.api.libs.json.{JsError, JsObject, JsString, JsValue, Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results.Status

sealed trait ApiErrorResponse {
  def statusCode: Int
  def code:       String
  def message:    String

  private def convertJsErrorsToReadableFormat: JsValue =
    this match {
      case validationError: JsonValidationApiError => Json.toJson(validationError)(ApiErrorResponse.validationWrites)
      case _ => Json.toJson(this)
    }

  def toResult: Result = Status(statusCode)(Json.toJson(convertJsErrorsToReadableFormat))
}

object ApiErrorResponse {
  implicit val validationWrites: Writes[JsonValidationApiError] = Writes { model =>
    Json.obj(
      "code"    -> model.code,
      "message" -> model.message,
      "errors"  -> model.getErrors
    )
  }

  implicit val writes: Writes[ApiErrorResponse] = (o: ApiErrorResponse) => JsObject(Seq("code" -> JsString(o.code), "message" -> JsString(o.message)))
}

object InternalServerApiError extends ApiErrorResponse {
  val statusCode: Int    = INTERNAL_SERVER_ERROR
  val code:       String = "INTERNAL_SERVER_ERROR"
  val message:    String = "Unexpected internal server error"
}

object NotFoundApiError extends ApiErrorResponse {
  val statusCode: Int    = NOT_FOUND
  val code:       String = "MATCHING_RESOURCE_NOT_FOUND"
  val message:    String = "Matching resource not found"
}

object BadRequestApiError extends ApiErrorResponse {
  val statusCode: Int    = BAD_REQUEST
  val code:       String = "BAD_REQUEST"
  val message:    String = "Invalid request"
}

object UnauthorisedApiError extends ApiErrorResponse {
  val statusCode: Int    = UNAUTHORIZED
  val code:       String = "MISSING_CREDENTIALS"
  val message:    String = "Authentication information is not provided"
}

object ForbiddenApiError extends ApiErrorResponse {
  val statusCode: Int    = FORBIDDEN
  val code:       String = "FORBIDDEN"
  val message:    String = "You are not allowed to access this resource"
}

object NotAcceptableApiError extends ApiErrorResponse {
  val statusCode: Int    = NOT_ACCEPTABLE
  val code:       String = "NOT_ACCEPTABLE"
  val message:    String = "Cannot produce an acceptable response"
}

object MethodNotAllowedApiError extends ApiErrorResponse {
  val statusCode: Int    = METHOD_NOT_ALLOWED
  val code:       String = "METHOD_NOT_ALLOWED"
  val message:    String = "This method is not supported"
}

final case class JsonValidationApiError(jsErrors: JsError) extends ApiErrorResponse {
  val statusCode: Int    = BAD_REQUEST
  val code:       String = "JSON_ERROR"
  val message:    String = "The provided JSON was invalid."

  val getErrors: JsValue = Json.toJson(jsErrors.errors.flatMap { case (path, pathErrors) =>
    val dropObjDot = 4
    pathErrors.map(validationError =>
      Json.obj(
        "code"    -> "INVALID_FORMAT",
        "message" -> validationError.message,
        "path"    -> path.toJsonString.drop(dropObjDot)
      )
    )
  })
}
