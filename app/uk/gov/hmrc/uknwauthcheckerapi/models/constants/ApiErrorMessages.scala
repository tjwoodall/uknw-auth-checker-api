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

package uk.gov.hmrc.uknwauthcheckerapi.models.constants

object ApiErrorMessages {
  val badRequest:               String           = "Bad request"
  val forbidden:                String           = "You are not allowed to access this resource"
  val internalServerError:      String           = "Unexpected internal server error"
  val invalidEori:              String => String = eori => s"$eori is not a supported EORI number"
  val invalidEoriCount:         Int => String    = eoriMax => s"The request payload must contain between 1 and $eoriMax EORI entries"
  val invalidRequest:           String           = "Invalid request"
  val matchingResourceNotFound: String           = "Matching resource not found"
  val notAcceptable:            String           = "Cannot produce an acceptable response. The Accept or Content-Type header is missing or invalid"
  val requestEntityTooLarge:    String           = "Request Entity Too Large"
  val serviceUnavailable:       String           = "Service unavailable"

  val unauthorized: String = "The bearer token is invalid, missing, or expired"
}
