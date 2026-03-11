package com.wang.twkanviewer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wang.twkanviewer.models.Chapter
import com.wang.twkanviewer.models.ChapterLocale
import com.wang.twkanviewer.models.Story
import com.wang.twkanviewer.models.StoryLocale

@Database(entities = [
    Story::class,
    StoryLocale::class,
    Chapter::class,
    ChapterLocale::class
], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun storyDao(): StoryDao
    abstract fun chapterDao(): ChapterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Recreate 'chapters' to fix nullability of 'content'
                db.execSQL("CREATE TABLE IF NOT EXISTS `chapters_new` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `order` INTEGER NOT NULL, `url` TEXT NOT NULL, `uploaded_at` INTEGER, `content` TEXT NOT NULL, `story_id` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`story_id`) REFERENCES `stories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO `chapters_new` (`id`, `title`, `order`, `url`, `uploaded_at`, `content`, `story_id`) SELECT `id`, `title`, `order`, `url`, `uploaded_at`, IFNULL(`content`, ''), `story_id` FROM `chapters`")
                db.execSQL("DROP TABLE `chapters`")
                db.execSQL("ALTER TABLE `chapters_new` RENAME TO `chapters`")

                // 2. Recreate 'chapter_locales' to fix nullability and add unique index
                db.execSQL("CREATE TABLE IF NOT EXISTS `chapter_locales_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `chapter_id` INTEGER NOT NULL, `language` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, FOREIGN KEY(`chapter_id`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO `chapter_locales_new` (`id`, `chapter_id`, `language`, `title`, `content`) SELECT `id`, `chapter_id`, `language`, `title`, IFNULL(`content`, '') FROM `chapter_locales`")
                db.execSQL("DROP TABLE `chapter_locales`")
                db.execSQL("ALTER TABLE `chapter_locales_new` RENAME TO `chapter_locales`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_chapter_locales_chapter_id_language` ON `chapter_locales` (`chapter_id`, `language`)")

                // 3. Recreate 'story_locales' to fix nullability and add unique index
                db.execSQL("CREATE TABLE IF NOT EXISTS `story_locales_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `story_id` INTEGER NOT NULL, `language` TEXT NOT NULL, `title` TEXT NOT NULL, `genre` TEXT NOT NULL, `description` TEXT NOT NULL, `tags` TEXT NOT NULL, FOREIGN KEY(`story_id`) REFERENCES `stories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO `story_locales_new` (`id`, `story_id`, `language`, `title`, `genre`, `description`, `tags`) SELECT `id`, `story_id`, `language`, `title`, `genre`, `description`, `tags` FROM `story_locales`")
                db.execSQL("DROP TABLE `story_locales`")
                db.execSQL("ALTER TABLE `story_locales_new` RENAME TO `story_locales`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_story_locales_story_id_language` ON `story_locales` (`story_id`, `language`)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `stories` ADD COLUMN `bookmarked_chapter_id` INTEGER")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
