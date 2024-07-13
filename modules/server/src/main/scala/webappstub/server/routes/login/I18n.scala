package webappstub.server.routes.login

import webappstub.server.data.UiLanguage

final case class I18n(
    username: String,
    password: String,
    loginPlaceholder: String,
    passwordPlaceholder: String,
    loginButton: String
)

object I18n:
  def apply(lang: UiLanguage): I18n =
    lang match
      case UiLanguage.English => en
      case UiLanguage.German  => de
      case UiLanguage.French  => fr

  val en = I18n(
    username = "Username",
    password = "Password",
    loginPlaceholder = "Login",
    passwordPlaceholder = "Password",
    loginButton = "Login"
  )

  val de = I18n(
    username = "Benutzer",
    password = "Passwort",
    loginPlaceholder = "Benutzer",
    passwordPlaceholder = "Passwort",
    loginButton = "Anmelden"
  )

  val fr = I18n(
    username = "Identifiant",
    password = "Mot de passe",
    loginPlaceholder = "Utilisateur",
    passwordPlaceholder = "Mot de passe",
    loginButton = "Connexion"
  )
