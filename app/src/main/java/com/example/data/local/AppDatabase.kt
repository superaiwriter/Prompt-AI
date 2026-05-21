package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Prompt
import com.example.data.model.Review
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Prompt::class, Review::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun promptDao(): PromptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prompt_studio_db"
                )
                .addCallback(AppDatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    try {
                        populateDatabase(database.promptDao())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        private suspend fun populateDatabase(dao: PromptDao) {
            // 1. Image Generation Prompts
            val p1 = dao.insertPrompt(Prompt(
                title = "Chiaroscuro Cinematic Portrait",
                content = "A close-up dramatic portrait of an elder storyteller, weathered face, deep wrinkles showing a lifetime of wisdom, intense expressive eyes, dramatic chiaroscuro studio lighting with rich dark shadows and striking warm key light, shot on 85mm lens, f/1.4, photorealistic texture, shallow depth of field, cinematic grading, high dynamic range, intricate details --ar 16:9 --style raw",
                instruction = "Copy directly into Midjourney v6. Works best with '--style raw' parameters to yield maximum realism on facial features and rich lighting details.",
                targetPlatform = "Midjourney v6",
                categories = listOf("Image Generation", "Photorealistic Portrait"),
                isPreloaded = true
            ))

            val p2 = dao.insertPrompt(Prompt(
                title = "Aetherpunk Floating Citadel",
                content = "Stunning concept art of an emerald green floating citadel suspended high in cyan skies by massive glowing geometric crystals. Streams of waterfall cascade off the levitating island edges into the clouds. Sleek brass and white porcelain airships dock on delicate spindly spires. Soft dawn mist, golden ratio design, high-fantasy ethereal mood, unreal engine 5 render, digital illustration --ar 16:9",
                instruction = "Highly tested on Stable Diffusion XL (SDXL). Set steps to 35, guidance scale to 7.0 for optimal high-concept environmental clarity.",
                targetPlatform = "Stable Diffusion",
                categories = listOf("Image Generation", "Fantasy Concept Art"),
                isPreloaded = true
            ))

            val p3 = dao.insertPrompt(Prompt(
                title = "Isometric Sci-Fi Mech Bay",
                content = "A highly detailed cute 3D isometric stylized miniature of a futuristic sci-fi robot assembly garage. Small orange utility drones welding a metallic cyber-mech suspended in a carbon-fiber dock. Holographic diagnostic screens glowing neon sky-blue, complex cable wiring on polished concrete floor, volumetric ambient smoke, octane render style, soft clay shader, warm orange and cool blue rim lights --ar 1:1",
                instruction = "Perfect for DALL-E 3 on ChatGPT or Bing Image Creator. No extra parameters needed, DALL-E automatically renders isometric view beautifully.",
                targetPlatform = "DALL-E 3",
                categories = listOf("Image Generation", "3D Isometric Render"),
                isPreloaded = true
            ))

            // 2. Video Generation Prompts
            val p4 = dao.insertPrompt(Prompt(
                title = "Cyberpunk Tokyo Street Puddle",
                content = "Cinematic slow-motion shot tracking at low angle across a neon-drenched rainy Tokyo street. The camera glides over a deep puddle, capturing the crisp reflection of a passing holographic cyberpunk train above and shimmering neon kanji signs. The water surface ripples slowly as heavy raindrops disturb the glass-like reflection. Anamorphic lens flare, deep teal and amber color grade, incredibly realistic water physics and light refraction, 4k resolution, 60fps.",
                instruction = "Copy directly into OpenAI Sora or Runway Gen-3 Alpha. Ideal for cinematic text-to-video triggers.",
                targetPlatform = "OpenAI Sora",
                categories = listOf("Video Generation", "Sora Cinematic Hook", "Video - Sci-Fi"),
                isPreloaded = true
            ))

            val p5 = dao.insertPrompt(Prompt(
                title = "Misty Nordic Fjord Flyover",
                content = "Breathtaking FPV drone flight sweeping down from jagged, snow-capped Norwegian peaks, weaving smoothly through a narrow misty fjord at sunrise. The golden morning sun rays slice through the low-hanging fog bank, illuminating a tiny red wooden cabin nestled on the isolated pine forest shoreline below. The mountain lake surface is mirror-smooth, reflecting the dramatic golden clouds. Hyper-realistic cinematic drone cinematography, cinematic dynamic range.",
                instruction = "Built for Luma Dream Machine and Runway Gen-3. Yields perfect mountain scale and consistent light path tracking.",
                targetPlatform = "Runway Gen-3 / Luma",
                categories = listOf("Video Generation", "Cinematic Drone Shot"),
                isPreloaded = true
            ))

            // 3. Text Generation Prompts
            val p6 = dao.insertPrompt(Prompt(
                title = "PAS Copywriting Framework Optimizer",
                content = "Act as an elite conversion copywriter. I will give you a product and target audience. Write 3 distinct and compelling variations using the Problem-Agitate-Solve (PAS) framework. Variation 1 should be direct and high-urgency; Variation 2 story-driven and emotional; Variation 3 analytical and fact-focused. Ensure the tone is punchy, eliminates jargon, and includes a clear, high-click CTA for each.",
                instruction = "Paste into ChatGPT or Claude. You can follow up with: [Product: AI Coding Companion, Audience: Indie Hackers].",
                targetPlatform = "ChatGPT",
                categories = listOf("Text Generation", "Marketing Copywriting"),
                isPreloaded = true
            ))

            val p7 = dao.insertPrompt(Prompt(
                title = "Viral Hook & Thread Architect",
                content = "You are a master of audience engagement on platforms like X/Twitter and LinkedIn. Analyze the following article or topic. Create 5 distinct viral hook ideas (focusing on Curiosity Loops, Contradictory Statements, and Relatable Struggles). For the best hook, draft a cohesive 6-part thread that uses bucket brigades, open loops, and high density value, ending with a call to action to follow or subscribe.",
                instruction = "Works perfectly with Claude 3.5 Sonnet. Insert any blog post URL or transcript paste to draft.",
                targetPlatform = "Claude 3.5 Sonnet",
                categories = listOf("Text Generation", "Social Media Hook"),
                isPreloaded = true
            ))

            // 4. Creative Writing Prompts
            val p8 = dao.insertPrompt(Prompt(
                title = "Dystopian Cosmic Noir Narrative",
                content = "Generate a dense, atmospheric, world-building blueprint for a cosmic noir detective story set in a hollowed-out asteroid city. Define 3 key factions (with conflicting goals), a dark neon-drenched setting that serves as a functional protagonist, and a unique technological anomaly that becomes the core mystery piece. Include sensory descriptions, specific slangs used by citizens, and an outline of the first act ending in a major plot twist.",
                instruction = "Fabulous for deep adventure crafting. Recommended model: ChatGPT Plus (GPT-4o) or Gemini 1.5 Pro.",
                targetPlatform = "ChatGPT / Claude",
                categories = listOf("Creative Writing", "Sci-Fi Story Plot"),
                isPreloaded = true
            ))

            val p9 = dao.insertPrompt(Prompt(
                title = "Cybernetic Iambic Pentameter Bard",
                content = "Compose a beautiful, melancholic poem of 4 stanzas in traditional heroic couplets (iambic pentameter, AABB schema) on the theme of a lonely AI observatory deep in interstellar space waiting for a signal that never arrives. The poem must strictly maintain standard iambic rhythm and possess rich, evocative celestial metaphors.",
                instruction = "Paste into Gemini 1.5 Pro or ChatGPT. Restricts word counts and enforces metric rhyming.",
                targetPlatform = "Gemini",
                categories = listOf("Creative Writing", "Poetry Meter Generator"),
                isPreloaded = true
            ))

            // 5. Programming Prompts
            val p10 = dao.insertPrompt(Prompt(
                title = "High-Performance Clean Code Refactorer",
                content = "Act as an expert software architect and performance engineer. Review the following code snippet carefully. Provide a refactored version that is optimized for time and memory complexity, adheres strictly to modern clean coding principles, utilizes proper concurrency or idiomatic language patterns, and includes clear JSDoc/KDoc annotations. Explain the precise architectural improvements made, with a before/after performance comparison.",
                instruction = "Compatible with any LLM. Perfect for fixing legacy Java/Kotlin blocks or complex JavaScript/Python structures.",
                targetPlatform = "Claude / ChatGPT",
                categories = listOf("Programming", "Code Refactoring"),
                isPreloaded = true
            ))

            val p11 = dao.insertPrompt(Prompt(
                title = "Scalable PostgreSQL Schema Designer",
                content = "You are a high-level database administrator. Design a highly scalable PostgreSQL database schema for an online real-time chess platform with competitive matches, user tournament brackets, review stats, and historical rankings. Provide the full DDL scripts, configure appropriate indexing strategies, foreign key constraints, indexes to avoid race conditions, and write a custom trigger function to update user ELO ratings upon match completion.",
                instruction = "Use GPT-4o or Claude 3.5 Sonnet. Yields production-grade schema layouts and advanced indexing tips.",
                targetPlatform = "ChatGPT / Claude",
                categories = listOf("Programming", "Database Schema Designer"),
                isPreloaded = true
            ))


            // Preload seed Reviews & Ratings to give life to details screen
            dao.insertReview(Review(
                promptId = p1.toInt(),
                rating = 5,
                reviewerName = "Sarah Jenkins",
                reviewText = "Stunning outputs! Midjourney created the most beautiful, dark cinematic portraits I've ever generated. Highly recommended to use '--style raw' as instructed."
            ))
            dao.insertReview(Review(
                promptId = p1.toInt(),
                rating = 4,
                reviewerName = "Devon K.",
                reviewText = "Very intense light styling. The chiaroscuro contrast is exactly what I was looking for. Highly detailed."
            ))

            dao.insertReview(Review(
                promptId = p3.toInt(),
                rating = 5,
                reviewerName = "Liam Vance",
                reviewText = "The isometric detail is mindblowing! Rendered a mech bay mockup for my indie game in 30 seconds."
            ))

            dao.insertReview(Review(
                promptId = p4.toInt(),
                rating = 5,
                reviewerName = "Koji S.",
                reviewText = "Sora generated an incredibly realistic puddle reflection. The anamorphic lens flare was gorgeous. Best video prompt so far."
            ))

            dao.insertReview(Review(
                promptId = p6.toInt(),
                rating = 4,
                reviewerName = "Monica G.",
                reviewText = "Extremely useful utility for drafting landing page variants. Saved me hours of copywriting."
            ))

            dao.insertReview(Review(
                promptId = p10.toInt(),
                rating = 5,
                reviewerName = "Alex Rivera",
                reviewText = "Amazing refactoring rules. Claude optimized my nested loops perfectly and annotated all edge conditions seamlessly."
            ))
        }
    }
}
