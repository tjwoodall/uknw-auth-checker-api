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

object ApiErrorCodes {
  val badRequest:               String = "BAD_REQUEST"
  val forbidden:                String = "FORBIDDEN"
  val internalServerError:      String = "INTERNAL_SERVER_ERROR"
  val invalidFormat:            String = "INVALID_FORMAT"
  val matchingResourceNotFound: String = "MATCHING_RESOURCE_NOT_FOUND"
  val methodNotAllowed:         String = "METHOD_NOT_ALLOWED"
  val notAcceptable:            String = "NOT_ACCEPTABLE"
  val serviceUnavailable:       String = "SERVICE_UNAVAILABLE"
  val unauthorized:             String = "UNAUTHORIZED"
}
