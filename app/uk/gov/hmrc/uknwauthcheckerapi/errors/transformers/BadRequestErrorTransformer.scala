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

package uk.gov.hmrc.uknwauthcheckerapi.errors.transformers

import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.uknwauthcheckerapi.utils.CustomRegexes.{invalidFormatOfDatePattern, invalidFormatOfEorisPattern}

trait BadRequestErrorTransformer {

  private val eoriSeparator              = ","
  private val errorMessagePrefix         = "Invalid"
  private val errorMessageValueSeparator = ":"

  def transformBadRequest(errorMessages: String): JsValue = {
    val dateErrors = transformDateError(errorMessages)
    val eoriErrors = transformEoriErrors(errorMessages)

    Json.toJson(
      (dateErrors ++ eoriErrors).flatten
    )
  }

  private def transformDateError(errorMessages: String): Option[Array[JsObject]] = errorMessages
    .split(errorMessagePrefix)
    .filter(_ matches invalidFormatOfDatePattern)
    .map(_.trim)
    .headOption
    .map { errorMessage =>
      val date = errorMessage.split(errorMessageValueSeparator).last.trim
      Array(
        Json.obj(
          "code"    -> "INVALID_FORMAT",
          "message" -> s"$date is not a valid date in the format YYYY-MM-DD",
          "path"    -> "date"
        )
      )
    }

  private def transformEoriErrors(errorMessages: String): Option[Array[JsObject]] = errorMessages
    .split(errorMessagePrefix)
    .filter(_ matches invalidFormatOfEorisPattern)
    .map(_.trim)
    .headOption
    .map { errorMessage =>
      errorMessage
        .split(errorMessageValueSeparator)
        .last
        .split(eoriSeparator)
        .map(_.trim) map { eori =>
        Json.obj(
          "code"    -> "INVALID_FORMAT",
          "message" -> s"$eori is not a supported EORI number",
          "path"    -> "eoris"
        )
      }
    }
}
