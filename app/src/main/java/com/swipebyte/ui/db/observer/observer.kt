package com.swipebyte.ui.db.observer

import com.swipebyte.ui.data.models.Restaurant

// Define specific observer interfaces for different data types
interface RestaurantObserver {
    fun onRestaurantUpdate(data: List<Restaurant>)
}

interface PreferencesObserver {
    fun onPreferencesUpdate(data: UserPreferences)
}

// Generic subject interface
interface Subject<T, O> {
    fun registerObserver(observer: O)
    fun unregisterObserver(observer: O)
    fun notifyObservers()
}

// Restaurant-specific observer pattern implementation
class RestaurantObservable : Subject<List<Restaurant>, RestaurantObserver> {
    private val observers = mutableListOf<RestaurantObserver>()
    private var restaurants: List<Restaurant> = emptyList()

    override fun registerObserver(observer: RestaurantObserver) {
        if (!observers.contains(observer)) {
            observers.add(observer)
        }
    }

    override fun unregisterObserver(observer: RestaurantObserver) {
        observers.remove(observer)
    }

    override fun notifyObservers() {
        observers.forEach { it.onRestaurantUpdate(restaurants) }
    }

    fun updateRestaurants(newRestaurants: List<Restaurant>) {
        restaurants = newRestaurants
        notifyObservers()
    }

    fun getRestaurants(): List<Restaurant> {
        return restaurants
    }
}

// A singleton to provide global access to the restaurant observable
object RestaurantDataObserver {
    private val restaurantObservable = RestaurantObservable()

    fun getObservable(): RestaurantObservable {
        return restaurantObservable
    }
}