package webappstub.store

import webappstub.common.model.AccountId

final case class ContactQuery(owner: AccountId, query: String, limit: Long, offset: Long)
