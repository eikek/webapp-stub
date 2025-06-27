package webappstub.backend.auth

import webappstub.common.model.*

import org.mindrot.jbcrypt.BCrypt

object PasswordCrypt:
  def crypt(pass: Password): Password =
    if (pass.isEmpty) pass
    else Password(BCrypt.hashpw(pass.value, BCrypt.gensalt()))

  def check(plain: Password, hashed: Password): Boolean =
    hashed.nonEmpty && plain.nonEmpty && BCrypt.checkpw(plain.value, hashed.value)
