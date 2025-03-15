package webappstub.store.postgres

import cats.effect.*

import webappstub.common.model.*
import webappstub.store.PostgresTest

import munit.CatsEffectSuite

class AccountRepoTest extends CatsEffectSuite with PostgresTest:

  test("check auto user"):
    accountRepoResource.use { repo =>
      for
        acc <- repo.findByLogin(LoginName.autoUser, Some(AccountState.Active))
        _ = assert(acc.isDefined)
      yield ()
    }
