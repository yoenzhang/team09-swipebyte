package com.example.swipebyte.model

import com.example.swipebyte.ui.data.models.SwipeQueryable
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class SwipeQueryableTest {
    // Mock Firestore and its references
    private val mockFirestore = mockk<FirebaseFirestore>()
    private val mockCollectionRef = mockk<CollectionReference>()
    private val mockDocumentRef = mockk<DocumentReference>()
    private val mockTask = mockk<com.google.android.gms.tasks.Task<Void>>()

    @Before
    fun setup() {
        // Mock the SwipeQueryable object
        mockkObject(SwipeQueryable)

        // Mock static Firebase methods
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns mockFirestore

        // Mock collection reference
        every { mockFirestore.collection(any()) } returns mockCollectionRef
        every { mockCollectionRef.document(any()) } returns mockDocumentRef

        // Mock the set operation and task
        every { mockDocumentRef.set(any()) } returns mockTask
        every { mockTask.addOnSuccessListener(any()) } returns mockTask
        every { mockTask.addOnFailureListener(any()) } returns mockTask
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun recordSwipe_callsFirebaseCorrectly() = runBlocking {
        // Given
        val restaurantId = "123"
        val restaurantName = "Test Restaurant"
        val isLiked = true

        // When
        SwipeQueryable.recordSwipe(restaurantId, restaurantName, isLiked)

        // Then
        // Verify that Firestore was accessed
        verify { FirebaseFirestore.getInstance() }

        // Verify a document was accessed/created in a collection
        verify { mockFirestore.collection(any()) }
        verify { mockCollectionRef.document(any()) }

        // Verify data was set on the document
        verify { mockDocumentRef.set(any()) }
    }
}