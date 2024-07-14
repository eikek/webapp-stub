package webappstub.server.routes.contacts

import cats.syntax.all.*

import webappstub.common.model.*
import webappstub.server.data.UiTheme
import webappstub.server.routes.Layout
import webappstub.server.routes.Styles
import webappstub.server.routes.contacts.Model.*

import htmx4s.scalatags.Bundle.*
import scalatags.Text.TypedTag
import scalatags.Text.all.doctype

final class Views(theme: UiTheme):
  lazy val searchControls: Set[String] = Set(Id.searchBtn, Id.searchInput)
  private object Id:
    val searchBtn = "search-btn"
    val searchInput = "search-input"

  def showContact(c: Contact) =
    div(
      h2(cls := "text-xl font-bold underline", c.fullName),
      div(
        div("Phone:", c.phone.map(_.value).getOrElse("-")),
        div("Email:", c.email.map(_.value).getOrElse("-"))
      ),
      div(
        cls := "flex flex-row items-center space-x-2 mt-4",
        attr.hxBoost := true,
        a(cls := Styles.btn, attr.href := s"/app/contacts/${c.id}/edit", "Edit"),
        a(cls := Styles.btn, attr.href := "/app/contacts", "Back")
      )
    )

  def countSnippet(n: Long): TypedTag[String] =
    span(cls := "ml-2 text-sm", s"$n total contacts")

  def showContactPage(m: ContactShowPage) =
    Layout(m.contact.fullName, theme, Layout.topNavBar.some)(
      div(
        cls := "container mx-auto mt-2",
        showContact(m.contact)
      )
    )

  def errorList(errors: List[String]): TypedTag[String] =
    val hidden = if (errors.isEmpty) "hidden" else ""
    ul(attr.`class` := s"text-red-500 $hidden", errors.map(m => li(m)))

  def errorList(
      errors: Option[Errors],
      key: ContactError.Key
  ): TypedTag[String] =
    errorList(errors.flatMap(_.find(key)).map(_.toList).getOrElse(Nil))

  def editContact(
      c: ContactEditForm,
      id: Option[ContactId],
      formErrors: Option[Errors]
  ) =
    div(
      form(
        attr.action := id.fold("/app/contacts/new")(n => s"/app/contacts/$n/edit"),
        attr.method := "POST",
        fieldset(
          cls := "flex flex-col border border-gray-100 dark:border-slate-700 px-4 py-2",
          legend(cls := "font-semibold px-2", "Contact Values"),
          div(
            cls := "flex flex-col md:w-1/2",
            label(attr.`for` := "email", cls := "font-semibold text-md", "Email"),
            id.map(n =>
              input(
                cls := "hidden",
                attr.`type` := "hidden",
                attr.name := "id",
                attr.value := n.value
              )
            ),
            input(
              cls := Styles.textInput,
              attr.name := "email",
              attr.id := "email",
              attr.`type` := "email",
              attr.placeholder := "Email",
              attr.value := c.email.orEmpty,
              attr.hxGet := "/app/contacts/email-check",
              attr.hxInclude := "[name='id']",
              attr.hxTarget := "next .error-list",
              attr.hxSwap := "outerHTML",
              attr.hxTrigger := "change, keyup delay:200ms changed"
            ),
            errorList(formErrors, ContactError.Key.Email)
          ),
          div(
            cls := "flex flex-col md:w-1/2",
            label(
              attr.`for` := "name.first",
              cls := "font-semibold text-md",
              "First Name"
            ),
            input(
              cls := Styles.textInput,
              attr.name := "firstName",
              attr.id := "name.first",
              attr.`type` := "text",
              attr.placeholder := "First Name",
              attr.value := c.firstName
            ),
            errorList(formErrors, ContactError.Key.FirstName)
          ),
          div(
            cls := "flex flex-col md:w-1/2",
            label(attr.`for` := "name.last", cls := "font-semibold text-md", "Last Name"),
            input(
              cls := Styles.textInput,
              attr.name := "lastName",
              attr.id := "name.last",
              attr.`type` := "text",
              attr.placeholder := "Last Name",
              attr.value := c.lastName
            ),
            errorList(formErrors, ContactError.Key.LastName)
          ),
          div(
            cls := "flex flex-col md:w-1/2",
            label(attr.`for` := "phone", cls := "font-semibold text-md", "Phone"),
            input(
              cls := Styles.textInput,
              attr.name := "phone",
              attr.id := "phone",
              attr.`type` := "phone",
              attr.placeholder := "Phone",
              attr.value := c.phone.orEmpty
            ),
            errorList(formErrors, ContactError.Key.Phone)
          ),
          div(
            errorList(formErrors, ContactError.Key.Default)
          ),
          div(
            cls := "mt-4 flex flex-row items-center space-x-2",
            button(cls := Styles.btn, "Save"),
            id.map { n =>
              button(
                cls := Styles.btn + " mx-3",
                attr.hxDelete := s"/app/contacts/$n",
                attr.hxTarget := "body",
                attr.hxPushUrl := true,
                attr.hxConfirm := "Are you sure you want to delete this contact?",
                "Delete Contact"
              )
            },
            a(
              cls := Styles.btn,
              attr.hxBoost := true,
              attr.href := "/app/contacts",
              "Back"
            )
          )
        )
      )
    )

  def editContactPage(m: ContactEditPage) =
    val title = m.fullName.map(n => s"Edit $n").getOrElse("Create New")
    Layout(title, theme, Layout.topNavBar.some)(
      div(
        cls := "container mx-auto mt-2",
        h2(cls := "text-xl font-bold mb-2", title),
        editContact(m.form, m.id, m.validationErrors),
        m.validationErrors.map(_ => p("There are form errors"))
      )
    )

  def contactListPage(m: ContactListPage): doctype =
    Layout("Contact Search", theme, Layout.topNavBar.some)(
      div(
        cls := "flex flex-col container mx-auto",
        div(
          cls := "h-8 flex flex-row items-center my-1",
          div(
            input(
              cls := Styles.searchInput + " h-full mr-1",
              attr.id := Id.searchInput,
              attr.`type` := "search",
              attr.name := "q",
              attr.value := m.query.getOrElse(""),
              attr.hxGet := "/app/contacts",
              attr.hxTarget := "table",
              attr.hxTrigger := "search, keyup delay:200ms changed",
              attr.hxPushUrl := true,
              attr.hxIndicator := "#spinner"
            ),
            a(
              cls := s"${Styles.btn} text-xs rounded-lg",
              attr.hxGet := "/app/contacts",
              attr.hxInclude := "#search",
              attr.hxTarget := "table",
              attr.id := Id.searchBtn,
              attr.hxPushUrl := true,
              attr.hxIndicator := "#spinner",
              i(cls := "fa fa-search")
            )
          ),
          div(
            attr.id := "spinner",
            cls := "h-8 w-8 px-1 py-1 animate-spin htmx-indicator",
            "●"
          ),
          div(
            cls := "flex-grow flex flex-row items-center justify-end",
            attr.hxBoost := true,
            a(
              cls := Styles.link + " inline-block",
              attr.href := "/app/contacts/new",
              "Add Contact"
            ),
            span(
              attr.hxGet := "/app/contacts/count",
              attr.hxTrigger := "load"
            )
          )
        ),
        form(
          cls := "flex flex-col",
          contactTable(m.contacts, m.page),
          div(
            cls := "mt-3",
            a(
              cls := Styles.btn,
              attr.hxDelete := "/app/contacts",
              attr.hxConfirm := "Are you sure?",
              attr.hxTarget := "body",
              "Delete selected contacts"
            )
          )
        )
      )
    )

  def contactTable(contacts: List[Contact], page: Int) =
    table(
      cls := "w-full table-auto border-collapse",
      thead(
        tr(
          cls := "py-4 bg-grey-100 dark:bg-slate-700 border-b border-grey-100 dark:border-slate-700",
          th(),
          th(cls := "text-center", "Id"),
          th(cls := "text-left", "Name"),
          th(cls := "text-left", "E-Mail"),
          th(cls := "text-left", "Phone"),
          th("")
        )
      ),
      tbody(
        contacts.map(c =>
          tr(
            cls := "py-2 border-b border-grey-100 dark:border-slate-700 hover:bg-grey-50 dark:hover:bg-slate-700 hover:bg-opacity-25",
            hxOn"mouseenter" := "C.moveClass(event, '.actions', 'invisible', 'visible')",
            hxOn"mouseleave" := "C.moveClass(event, '.actions', 'visible', 'invisible')",
            td(
              cls := "",
              input(
                attr.`type` := "checkbox",
                attr.name := "selectedId[]",
                attr.value := c.id.value
              )
            ),
            td(cls := "text-center px-2", c.id.value),
            td(cls := "text-left px-2", c.fullName),
            td(cls := "text-left px-2", c.email.map(_.value).getOrElse("-")),
            td(cls := "text-left px-2", c.phone.map(_.value).getOrElse("-")),
            td(
              cls := "actions flex flex-row items-center justify-end space-x-2 text-sm invisible",
              attr.hxBoost := true,
              a(cls := Styles.btn, attr.href := s"/app/contacts/${c.id}/edit", "Edit"),
              a(cls := Styles.btn, attr.href := s"/app/contacts/${c.id}", "View")
            )
          )
        ),
        Option(contacts.size).filter(_ >= 10).map { _ =>
          tr(
            cls := "my-1 px-2",
            td(
              attr.colspan := 5,
              button(
                cls := Styles.btn,
                attr.hxTarget := "closest tr",
                attr.hxSwap := "outerHTML",
                attr.hxSelect := "tbody > tr",
                attr.hxGet := s"/app/contacts?page=${page + 1}",
                attr.hxIndicator := "#spinner",
                "Load More"
              )
            )
          )
        }
      )
    )
