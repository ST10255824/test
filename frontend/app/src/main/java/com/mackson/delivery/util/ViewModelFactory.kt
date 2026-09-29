package com.mackson.delivery.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Generic Factory (Dependency Injection / Factory pattern, Part 1 section 9.3.12 item C):
 * lets a ViewModel declare its dependencies in its constructor instead of reaching for
 * FirestoreRepository/FirebaseAuthManager singletons directly, so a fake repository can be
 * substituted in tests without touching production code.
 *
 * Usage: `viewModel(factory = viewModelFactory { CartViewModel(FirestoreRepository) })`
 */
class GenericViewModelFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <U : ViewModel> create(modelClass: Class<U>): U = creator() as U
}

fun <T : ViewModel> viewModelFactory(creator: () -> T): GenericViewModelFactory<T> =
    GenericViewModelFactory(creator)
