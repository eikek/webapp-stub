package webappstub.common.model

enum AccountKey:
  case Internal(id: AccountId)
  case External(id: ExternalAccountId)

  def fold[A](f: AccountId => A, g: ExternalAccountId => A): A =
    this match
      case Internal(id) => f(id)
      case External(id) => g(id)
