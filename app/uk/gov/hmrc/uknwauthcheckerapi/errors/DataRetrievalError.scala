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

import play.api.libs.json.JsError

sealed trait DataRetrievalError

object DataRetrievalError {
  final case class BadGatewayDataRetrievalError() extends DataRetrievalError
  final case class BadRequestDataRetrievalError(message: String) extends DataRetrievalError
  final case class ForbiddenDataRetrievalError() extends DataRetrievalError
  final case class MethodNotAllowedDataRetrievalError(message: String) extends DataRetrievalError
  final case class InternalServerDataRetrievalError(message: String) extends DataRetrievalError
  final case class InternalUnexpectedDataRetrievalError(message: String, cause: Throwable) extends DataRetrievalError
  final case class UnableToDeserialiseDataRetrievalError(jsError: JsError) extends DataRetrievalError
  final case class ValidationDataRetrievalError(jsError: JsError) extends DataRetrievalError
}
