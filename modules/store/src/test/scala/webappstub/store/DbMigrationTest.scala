package webappstub.store

import munit.CatsEffectSuite
import skunk.codec.all.int8
import skunk.implicits.sql

class DbMigrationTest extends CatsEffectSuite with PostgresTest:
  test("migration") {
    session.use { s =>
      assertIO(
        s.unique(sql"SELECT count(*) FROM contact".query(int8)),
        0L
      )
    }
  }
