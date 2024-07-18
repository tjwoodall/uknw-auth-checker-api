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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import cats.data.EitherT

import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.uknwauthcheckerapi.controllers.actions.{AuthAction, HeaderValidatorAction}
import uk.gov.hmrc.uknwauthcheckerapi.controllers.responses.AuthorisationResponseHandler
import uk.gov.hmrc.uknwauthcheckerapi.services.{IntegrationFrameworkService, ValidationService}

@Singleton()
class AuthorisationsController @Inject() (
  authAction:                  AuthAction,
  cc:                          ControllerComponents,
  integrationFrameworkService: IntegrationFrameworkService,
  validationService:           ValidationService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with HeaderValidatorAction
    with AuthorisationResponseHandler {

  def authorisations: Action[JsValue] = Action
    .andThen(authAction)
    .andThen(validateHeaders(cc))
    .async(parse.json) { implicit request =>
      (for {
        authorisationsRequest <- EitherT.fromEither[Future](validationService.validateRequest(request))
        response              <- integrationFrameworkService.getAuthorisations(authorisationsRequest)
      } yield response).toResult
    }
}
