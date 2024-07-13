package webappstub.store.postgres

import cats.effect.*

import webappstub.common.model.*
import webappstub.store.PostgresTest

import munit.CatsEffectSuite

class ContactRepoTest extends CatsEffectSuite with PostgresTest:

  test("insert new contact"):
    contactRepoResource.use { repo =>
      val c =
        NewContact(Name.unsafe("John", "Doe"), Some(Email.unsafe("jdoe@me.com")), None)
      for
        id <- repo.insert(c)
        cc <- repo.findById(id)
        _ = assertEquals(cc, Some(c.withId(id)))
      yield ()
    }
