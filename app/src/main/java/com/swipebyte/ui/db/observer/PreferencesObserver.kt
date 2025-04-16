package com.swipebyte.ui.db.observer

// Data class to hold user preferences
data class UserPreferences(
    val cuisinePreferences: List<String> = emptyList(),
    val pricePreferences: List<String> = emptyList(),
    val locationRadius: Float = 5.0f
)

// Preferences-specific observer pattern implementation
class PreferencesObservable : Subject<UserPreferences, PreferencesObserver> {
    private val observers = mutableListOf<PreferencesObserver>()
    private var preferences: UserPreferences = UserPreferences()

    override fun registerObserver(observer: PreferencesObserver) {
        if (!observers.contains(observer)) {
            observers.add(observer)
        }
    }

    override fun unregisterObserver(observer: PreferencesObserver) {
        observers.remove(observer)
    }

    override fun notifyObservers() {
        observers.forEach { it.onPreferencesUpdate(preferences) }
    }

    fun updatePreferences(newPreferences: UserPreferences) {
        preferences = newPreferences
        notifyObservers()
    }

    fun getPreferences(): UserPreferences {
        return preferences
    }
}

// A singleton to provide global access to the preferences observable
object PreferencesDataObserver {
    private val preferencesObservable = PreferencesObservable()

    fun getObservable(): PreferencesObservable {
        return preferencesObservable
    }
}