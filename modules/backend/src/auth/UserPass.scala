package webappstub.backend.auth

import webappstub.common.model.*

final case class UserPass(login: LoginName, password: Password, rememberMe: Boolean)
