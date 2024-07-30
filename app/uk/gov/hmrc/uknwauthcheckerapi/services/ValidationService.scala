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

import scala.collection.Seq

import play.api.libs.json._
import play.api.mvc.Request
import uk.gov.hmrc.uknwauthcheckerapi.errors.DataRetrievalError
import uk.gov.hmrc.uknwauthcheckerapi.errors.DataRetrievalError.ValidationDataRetrievalError
import uk.gov.hmrc.uknwauthcheckerapi.models.AuthorisationRequest
import uk.gov.hmrc.uknwauthcheckerapi.utils.{CustomRegexes, ErrorMessages}

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
    val eoris = request.eoris

    val eoriErrors: Seq[JsonValidationError] = eoris
      .filterNot(e => e matches CustomRegexes.eoriPattern)
      .map(e => JsonValidationError(s"$e is not a supported EORI number"))

    (eoriErrors.nonEmpty, isEoriSizeInvalid(eoris.size)) match {
      case (false, false) => Right(request)
      case (_, true)      => Left(JsError(JsPath \ "eoris", ErrorMessages.invalidEoriCount))
      case (true, _) =>
        Left(
          JsError(
            Seq("eoris").map { field =>
              (JsPath \ field, eoriErrors)
            }
          )
        )
    }
  }

  private def isEoriSizeInvalid(eorisSize: Int): Boolean =
    if (eorisSize > 3000 || eorisSize < 1) true else false

}
