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

package uk.gov.hmrc.uknwauthcheckerapi.controllers

import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.uknwauthcheckerapi.errors.JsonValidationApiError
import uk.gov.hmrc.uknwauthcheckerapi.models.{AuthorisationRequest, AuthorisationResponse, AuthorisationsResponse}
import uk.gov.hmrc.uknwauthcheckerapi.utils.JsonResponses

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton()
class AuthorisationsController @Inject() (cc: ControllerComponents) extends BackendController(cc) with HeaderValidator with JsonResponses {

  def authorisations: Action[JsValue] = validateHeaders(cc).async(parse.json) { implicit request =>
    Future.successful(
      request.body.validate[AuthorisationRequest] match {
        case JsSuccess(authorisationRequest: AuthorisationRequest, _) =>
          Ok(AuthorisationsResponse(authorisationRequest.date, authorisationRequest.eoris.map(r => AuthorisationResponse(r, authorised = true))))
        case errors: JsError =>
          JsonValidationApiError(errors).toResult
      }
    )
  }
}
