package webappstub.server.routes

import webappstub.server.data.UiTheme

import htmx4s.scalatags.Bundle.*
import scalatags.Text.TypedTag
import scalatags.Text.all.doctype

object Layout:
  def styleMain(withTopBar: Boolean) = {
    val base =
      "flex flex-col w-full bg-white dark:bg-stone-800 text-gray-800 dark:text-stone-300 antialiased"
    if (withTopBar) s"$base mt-12" else base
  }

  val styleContent = "flex flex-grow"

  private val sizes = attr("sizes")

  def apply(titleStr: String, theme: UiTheme, topBar: Option[TypedTag[String]] = None)(
      content: TypedTag[String]
  ) =
    doctype("html")(
      html(
        head(
          title(attr.name := s"Webappstub - $titleStr"),
          meta(attr.charset := "UTF-8"),
          meta(attr.name := "mobile-web-app-capable", attr.content := "yes"),
          meta(
            attr.name := "viewport",
            attr.content := "width=device-width, initial-scale=1, user-scalable=yes"
          ),
          script(attr.src := "/app/assets/htmx/htmx.min.js"),
          script(attr.src := "/app/assets/htmx/ext/response-targets.js"),
          link(attr.href := "/app/assets/fa/css/all.min.css", attr.rel := "stylesheet"),
          link(
            attr.href := "/app/assets/fi/css/flag-icons.min.css",
            attr.rel := "stylesheet"
          ),
          link(attr.href := "/app/assets/self/index.css", attr.rel := "stylesheet"),
          script(attr.src := "/app/assets/self/all.min.js"),
          // favicon
          link(
            attr.rel := "apple-touch-icon",
            sizes := "180x180",
            attr.href := "/app/assets/self/favicon/apple-touch-icon.png"
          ),
          link(
            attr.rel := "icon",
            attr.`type` := "image/png",
            sizes := "32x32",
            attr.href := "/app/assets/self/favicon/favicon-32x32.png"
          ),
          link(
            attr.rel := "icon",
            attr.`type` := "image/png",
            sizes := "16x16",
            attr.href := "/app/assets/self/favicon/favicon-16x16.png"
          ),
          link(
            attr.rel := "manifest",
            attr.href := "/app/assets/self/favicon/site.webmanifest.json"
          ),
          meta(attr.name := "theme-color", attr.content := "#ffffff")
        ),
        body(
          cls := theme.fold("bg-white", "bg-stone-800 dark"),
          topBar,
          div(
            cls := styleMain(topBar.isDefined),
            div(
              cls := styleContent,
              content
            )
          )
        )
      )
    )

  def topNavBar: TypedTag[String] =
    div(
      cls := "top-0 fixed z-50 w-full flex flex-row justify-start shadow-sm h-12 bg-indigo-100 dark:bg-stone-900 text-gray-800 dark:text-stone-200 antialiased",
      a(
        cls := "inline-flex font-bold hover:bg-indigo-200 dark:hover:bg-stone-800 items-center px-4",
        attr.href := "/app/contacts",
        img(
          cls := "w-9 h-9 mr-2 block",
          attr.src := "/app/assets/self/favicon/favicon-32x32.png"
        ),
        div(cls := "", "Webappstub")
      ),
      div(
        cls := "flex flex-grow justify-end"
      )
    )

  private def notFound =
    div(
      cls := "container mx-auto mt-2",
      h2(cls := "text-2xl font-semibold my-2", "Resource not found!"),
      p("Sorry, this doesn't exist."),
      p(
        a(cls := Styles.link, attr.href := "/app/contacts", "Home")
      )
    )

  def notFoundPage(theme: UiTheme) = Layout("Not Found", theme)(notFound)
