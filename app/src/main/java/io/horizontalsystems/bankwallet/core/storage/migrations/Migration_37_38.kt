package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_37_38 : Migration(37, 38) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `WalletConnectV2Session` (`accountId` TEXT NOT NULL, `topic` TEXT NOT NULL, PRIMARY KEY(`accountId`, `topic`))")
    }
}
