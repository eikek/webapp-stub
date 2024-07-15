package webappstub.backend.signup

import webappstub.common.model.Account

enum SignupResult:
  case Success(account: Account)
  case LoginExists
  case SignupClosed
  case InvalidKey
