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

import play.api.libs.json._
import play.api.mvc.Request
import uk.gov.hmrc.uknwauthcheckerapi.errors.DataRetrievalError
import uk.gov.hmrc.uknwauthcheckerapi.errors.DataRetrievalError.ValidationDataRetrievalError
import uk.gov.hmrc.uknwauthcheckerapi.models.AuthorisationRequest
import uk.gov.hmrc.uknwauthcheckerapi.utils.CustomRegexes

import java.time.LocalDate
import java.time.format.DateTimeParseException
import scala.collection.Seq

class ValidationService {
  def validateRequest(request: Request[JsValue]): Either[DataRetrievalError, AuthorisationRequest] =
    request.body.validate[AuthorisationRequest] match {
      case JsSuccess(authorisationRequest: AuthorisationRequest, _) =>
        validateAuthorisationRequest(authorisationRequest) match {
          case Left(errors) => Left(ValidationDataRetrievalError(errors))
          case Right(r)     => Right(r)
        }
      case errors: JsError => Left(ValidationDataRetrievalError(errors))
    }

  private def validateAuthorisationRequest(request: AuthorisationRequest): Either[JsError, AuthorisationRequest] = {
    val date  = request.date
    val eoris = request.eoris

    val eoriErrors: Seq[JsonValidationError] = eoris
      .filterNot(e => e matches CustomRegexes.eoriPattern)
      .map(e => JsonValidationError(s"$e is not a supported EORI number"))

    val dateError = Seq(JsonValidationError(s"$date is not a valid date in the format YYYY-MM-DD"))

    (eoriErrors.nonEmpty, !date.isValidLocalDate) match {
      case (false, false) => Right(request)
      case (true, true) =>
        Left(
          JsError(
            Seq("eoris").map { field =>
              (JsPath \ field, eoriErrors)
            } ++
              Seq("date").map { field =>
                (JsPath \ field, dateError)
              }
          )
        )
      case (false, true) =>
        Left(
          JsError(
            Seq("date").map { field =>
              (JsPath \ field, dateError)
            }
          )
        )
      case (true, false) =>
        Left(
          JsError(
            Seq("eoris").map { field =>
              (JsPath \ field, eoriErrors)
            }
          )
        )
    }
  }

  private implicit class StringExtensions(text: String) {
    def isValidLocalDate: Boolean =
      try {
        LocalDate.parse(text)
        true
      } catch {
        case _: DateTimeParseException => false
      }
  }
}
