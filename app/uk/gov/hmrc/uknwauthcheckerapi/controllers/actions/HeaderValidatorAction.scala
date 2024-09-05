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

package uk.gov.hmrc.uknwauthcheckerapi.controllers.actions

import scala.concurrent.{ExecutionContext, Future}

import play.api.http.{HeaderNames, MimeTypes}
import play.api.mvc._
import uk.gov.hmrc.uknwauthcheckerapi.errors.NotAcceptableApiError
import uk.gov.hmrc.uknwauthcheckerapi.models.constants.HmrcMimeTypes
import uk.gov.hmrc.uknwauthcheckerapi.services.ZonedDateTimeService
import uk.gov.hmrc.uknwauthcheckerapi.utils.RequestExtensions

trait HeaderValidatorAction(using zs: ZonedDateTimeService) extends Results with RequestExtensions {

  def validateHeaders(controllerComponents: ControllerComponents): ActionBuilder[Request, AnyContent] = new ActionBuilder[Request, AnyContent] {

    def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {

      val hasAcceptHeader:      Boolean = request.hasHeaderValue(HeaderNames.ACCEPT, HmrcMimeTypes.json)
      val hasContentTypeHeader: Boolean = request.hasHeaderValue(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)

      if (hasAcceptHeader && hasContentTypeHeader) {
        block(request)
      } else {
        Future.successful(NotAcceptableApiError.toResult)
      }
    }

    override def parser: BodyParser[AnyContent] = controllerComponents.parsers.defaultBodyParser

    override protected def executionContext: ExecutionContext = controllerComponents.executionContext
  }
}
