package com.mackson.delivery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import okhttp3.OkHttpClient

class MacksonApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        if (BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS) {
            connectToLocalEmulators()
        } else {
            // Play Integrity attestation needs a real signed build talking to a real Firebase
            // project, so it's skipped for local emulator runs (see connectToLocalEmulators).
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }

    /**
     * Points every Firebase SDK at `firebase emulators:start` running on the host machine.
     * `10.0.2.2` is the Android emulator's alias for the host's `localhost` — see README
     * "Run everything locally". Toggle via `USE_FIREBASE_EMULATORS` in app/build.gradle.kts.
     */
    private fun connectToLocalEmulators() {
        val host = "10.0.2.2"
        FirebaseAuth.getInstance().useEmulator(host, 9099)
        FirebaseFirestore.getInstance().useEmulator(host, 8080)
        FirebaseDatabase.getInstance().useEmulator(host, 9000)
        FirebaseFunctions.getInstance().useEmulator(host, 5001)
        FirebaseStorage.getInstance().useEmulator(host, 9199)
    }

    /**
     * Product photos (functions/seed/catalogue.ts) are hotlinked from Wikimedia Commons.
     * Verified directly against the live URLs: Wikimedia's edge returns 403 Forbidden for any
     * request whose User-Agent identifies as the `okhttp` library specifically — including a
     * custom-but-still-client-looking header — while a normal browser-style User-Agent is
     * accepted (200). Coil's OkHttp client sends "okhttp/<version>" by default, which is why
     * every product photo silently fell back to the grocery-bag icon
     * (ui/common/ProductThumbnail.kt) before this fix.
     */
    override fun newImageLoader(): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    )
                    .build()
                chain.proceed(request)
            }
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .build()
    }
}
