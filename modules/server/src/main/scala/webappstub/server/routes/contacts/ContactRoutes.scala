package webappstub.server.routes.contacts

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.Backend
import webappstub.common.model.*
import webappstub.server.context.Context
import webappstub.server.routes.Layout
import webappstub.server.routes.contacts.Model.*
import webappstub.server.routes.contacts.Model.ContactEditForm

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.headers.HxTrigger
import org.http4s.HttpRoutes
import org.http4s.headers.Location
import org.http4s.implicits.*
import org.http4s.scalatags.*

final class ContactRoutes[F[_]: Async](api: RoutesApi[F]) extends Htmx4sDsl[F]:
  def routes(ctx: Context.Authenticated): HttpRoutes[F] =
    val views = Views(ctx.settings.theme)
    val notFoundPage = Layout.notFoundPage(ctx.settings.theme)
    HttpRoutes.of {
      case req @ GET -> Root :? Params.Query(q) +& Params.Page(p) =>
        for {
          result <- api.search(ctx.account, q, p)
          resp <- req.headers
            .get[HxTrigger]
            .whenIn(views.searchControls)(
              Ok(views.contactTable(result, p.getOrElse(1))),
              Ok(
                views.contactListPage(
                  ContactListPage(result, q.filter(_.nonEmpty), p.getOrElse(1))
                )
              )
            )
        } yield resp

      case GET -> Root / "count" =>
        api.countAll(ctx.account).flatMap(n => Ok(views.countSnippet(n)))

      case GET -> Root / "new" =>
        Ok(views.editContactPage(ContactEditPage.empty))

      case req @ POST -> Root / "new" =>
        for {
          formInput <- req.as[ContactEditForm]
          result <- api.upsert(formInput, ctx.account, None)
          resp <- result.fold(Ok(notFoundPage))(
            _.fold(
              errs =>
                Ok(views.editContactPage(ContactEditPage(None, formInput, errs.some))),
              _ => SeeOther(Location(uri"/app/contacts"))
            )
          )
        } yield resp

      case GET -> Root / Params.ContactId(id) =>
        for {
          c <- api.findById(id, ctx.account)
          view = c
            .map(ContactShowPage.apply)
            .fold(notFoundPage)(views.showContactPage)
          resp <- c.fold(NotFound(view))(_ => Ok(view))
        } yield resp

      case GET -> Root / Params.ContactId(id) / "edit" =>
        for {
          contact <- api.findById(id, ctx.account)
          form = contact.map(ContactEditForm.from)
          view = form.fold(notFoundPage)(c =>
            views.editContactPage(ContactEditPage(id.some, c, None))
          )
          resp <- contact.fold(NotFound(view))(_ => Ok(view))
        } yield resp

      case req @ POST -> Root / Params.ContactId(id) / "edit" =>
        for {
          formInput <- req.as[ContactEditForm]
          result <- api.upsert(formInput, ctx.account, id.some)
          resp <- result.fold(Ok(notFoundPage))(
            _.fold(
              errs =>
                Ok(views.editContactPage(ContactEditPage(id.some, formInput, errs.some))),
              _ => SeeOther(Location(uri"/app/contacts"))
            )
          )
        } yield resp

      case req @ DELETE -> Root =>
        for {
          ids <- req.as[SelectedIds]
          _ <- ids.selectedId.traverse(api.delete)
          all <- api.search(ctx.account, None, None)
          resp <- Ok(views.contactListPage(ContactListPage(all, None, 1)))
        } yield resp

      case DELETE -> Root / Params.ContactId(id) =>
        for {
          found <- api.delete(id)
          resp <-
            if (found) SeeOther(Location(uri"/app/contacts"))
            else NotFound(notFoundPage)
        } yield resp

      case GET -> Root / "email-check" :?
          Params.Email(emailStr) +& Params.IdOpt(id) =>
        for {
          result <- api.checkMail(id, ctx.account, emailStr)
          resp <- result.fold(
            errs => Ok(views.errorList(errs.some, ContactError.Key.Email)),
            _ => Ok(views.errorList(Nil))
          )
        } yield resp
    }
object ContactRoutes:
  def create[F[_]: Async](backend: Backend[F]): ContactRoutes[F] =
    ContactRoutes(RoutesApi(backend.contacts))
