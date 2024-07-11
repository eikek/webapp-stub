package webappstub.store.postgres

import cats.effect.*
import munit.CatsEffectSuite
import webappstub.common.model.*
import webappstub.store.PostgresTest

class ContactRepoTest extends CatsEffectSuite with PostgresTest:

  test("insert new contact"):
    contactRepoResource.use { repo =>
      val c = NewContact(Name.unsafe("John", "Doe"), Some(Email.unsafe("jdoe@me.com")), None)
      for
        id <- repo.insert(c)
        cc <- repo.findById(id)
        _ = assertEquals(cc, Some(c.withId(id)))
      yield ()
    }
