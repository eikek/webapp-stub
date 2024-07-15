package webappstub.server.routes.signup

import webappstub.server.data.UiLanguage

final case class I18n(
    username: String,
    password: String,
    passwordConfirm: String,
    loginPlaceholder: String,
    passwordPlaceholder: String,
    passwordConfirmPlaceholder: String,
    submitButton: String,
    inviteKey: String,
    inviteKeyPlaceholder: String
)

object I18n:
  def apply(lang: UiLanguage): I18n =
    lang match
      case UiLanguage.English => en
      case UiLanguage.German  => de

  val en = I18n(
    username = "Username",
    password = "Password",
    passwordConfirm = "Password (confirm)",
    loginPlaceholder = "Login",
    passwordPlaceholder = "Password",
    passwordConfirmPlaceholder = "Password (confirm)",
    inviteKey = "Invite Key",
    inviteKeyPlaceholder = "Invite…",
    submitButton = "Submit"
  )

  val de = I18n(
    username = "Benutzer",
    password = "Passwort",
    passwordConfirm = "Passwort bestätigen",
    loginPlaceholder = "Benutzer",
    passwordPlaceholder = "Passwort",
    passwordConfirmPlaceholder = "Passwort bestätigen",
    inviteKey = "Einladungs-Schlüssel",
    inviteKeyPlaceholder = "Einladung…",
    submitButton = "Absenden"
  )
