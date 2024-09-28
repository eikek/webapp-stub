package webappstub.server.routes.contacts

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.Backend
import webappstub.common.model.*
import webappstub.server.context.*
import webappstub.server.routes.Layout
import webappstub.server.routes.contacts.Model.*
import webappstub.server.routes.contacts.Model.ContactEditForm

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.headers.HxTrigger
import org.http4s.*
import org.http4s.headers.Location
import org.http4s.implicits.*
import org.http4s.scalatags.*

final class ContactRoutes[F[_]: Async](api: RoutesApi[F]) extends Htmx4sDsl[F]:
  def routes =
    AuthedRoutes.of[Context.Account, F] {
      case ContextRequest(ctx, req @ GET -> Root :? Params.Query(q) +& Params.Page(p)) =>
        val settings = Settings.fromRequest(req)
        val views = Views(settings.theme)
        for {
          result <- api.search(ctx.id, q, p)
          resp <- req.headers
            .get[HxTrigger]
            .whenIn(views.searchControls)(
              Ok(views.contactTable(result, p.getOrElse(1))),
              Ok(
                views.contactListPage(
                  ctx,
                  ContactListPage(result, q.filter(_.nonEmpty), p.getOrElse(1))
                )
              )
            )
        } yield resp

      case ContextRequest(ctx, req @ GET -> Root / "count") =>
        val settings = Settings.fromRequest(req)
        val views = Views(settings.theme)
        api.countAll(ctx.id).flatMap(n => Ok(views.countSnippet(n)))

      case ContextRequest(ctx, req @ GET -> Root / "new") =>
        val settings = Settings.fromRequest(req)
        val views = Views(settings.theme)
        Ok(views.editContactPage(ctx, ContactEditPage.empty))

      case ContextRequest(ctx, req @ POST -> Root / "new") =>
        val settings = Settings.fromRequest(req)
        val views = Views(settings.theme)
        val notFoundPage = Layout.notFoundPage(settings.theme)

        for {
          formInput <- req.as[ContactEditForm]
          result <- api.upsert(formInput, ctx.id, None)
          resp <- result.fold(Ok(notFoundPage))(
            _.fold(
              errs =>
                Ok(
                  views.editContactPage(ctx, ContactEditPage(None, formInput, errs.some))
                ),
              _ => SeeOther(Location(uri"/app/contacts"))
            )
          )
        } yield resp

      case ContextRequest(ctx, req @ GET -> Root / Params.ContactId(id)) =>
        val settings = Settings.fromRequest(req)
        val views = Views(settings.theme)
        val notFoundPage = Layout.notFoundPage(settings.theme)
        for {
          c <- api.findById(id, ctx.id)
          view = c
            .map(ContactShowPage.apply)
            .fold(notFoundPage)(views.showContactPage(ctx, _))
          resp <- c.fold(NotFound(view))(_ => Ok(view))
        } yield resp

      case ContextRequest(ctx, req @ GET -> Root / Params.ContactId(id) / "edit") =>
        val settings = Settings.fromRequest(req)
        val views = Views(settings.theme)
        val notFoundPage = Layout.notFoundPage(settings.theme)
        for {
          contact <- api.findById(id, ctx.id)
          form = contact.map(ContactEditForm.from)
          view = form.fold(notFoundPage)(c =>
            views.editContactPage(ctx, ContactEditPage(id.some, c, None))
          )
          resp <- contact.fold(NotFound(view))(_ => Ok(view))
        } yield resp

      case ContextRequest(ctx, req @ POST -> Root / Params.ContactId(id) / "edit") =>
        val settings = Settings.fromRequest(req)
        val views = Views(settings.theme)
        val notFoundPage = Layout.notFoundPage(settings.theme)
        for {
          formInput <- req.as[ContactEditForm]
          result <- api.upsert(formInput, ctx.id, id.some)
          resp <- result.fold(Ok(notFoundPage))(
            _.fold(
              errs =>
                Ok(
                  views
                    .editContactPage(ctx, ContactEditPage(id.some, formInput, errs.some))
                ),
              _ => SeeOther(Location(uri"/app/contacts"))
            )
          )
        } yield resp

      case ContextRequest(ctx, req @ DELETE -> Root) =>
        val settings = Settings.fromRequest(req)
        val views = Views(settings.theme)
        for {
          ids <- req.as[SelectedIds]
          _ <- ids.selectedId.traverse(api.delete)
          all <- api.search(ctx.id, None, None)
          resp <- Ok(views.contactListPage(ctx, ContactListPage(all, None, 1)))
        } yield resp

      case ContextRequest(ctx, req @ DELETE -> Root / Params.ContactId(id)) =>
        val settings = Settings.fromRequest(req)
        val notFoundPage = Layout.notFoundPage(settings.theme)
        for {
          found <- api.delete(id)
          resp <-
            if (found) SeeOther(Location(uri"/app/contacts"))
            else NotFound(notFoundPage)
        } yield resp

      case ContextRequest(
            ctx,
            req @ GET -> Root / "email-check" :? Params.Email(emailStr) +& Params.IdOpt(
              id
            )
          ) =>
        val settings = Settings.fromRequest(req)
        val views = Views(settings.theme)
        for {
          result <- api.checkMail(id, ctx.id, emailStr)
          resp <- result.fold(
            errs => Ok(views.errorList(errs.some, ContactError.Key.Email)),
            _ => Ok(views.errorList(Nil))
          )
        } yield resp
    }
object ContactRoutes:
  def create[F[_]: Async](backend: Backend[F]): ContactRoutes[F] =
    ContactRoutes(RoutesApi(backend.contacts))
