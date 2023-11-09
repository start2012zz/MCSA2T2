package io.github.sceneview.sample.arcursorplacement

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view to the layout file that contains your FragmentContainerView
        setContentView(R.layout.viewactivity)

        // Check if this is the first creation of the activity
        if (savedInstanceState == null) {
            // Get the ID passed to this activity
            val receivedId = intent.getStringExtra(EXTRA_ID) ?: return
            // Create a new instance of ViewFragment with the received ID
            val fragment = ViewFragment.newInstance(receivedId)

            // Begin a transaction to add the fragment to the container
            supportFragmentManager.beginTransaction()
                // Make sure the ID matches the ID of the FragmentContainerView in your XML
                .add(R.id.modelViewContainerFragment, fragment)
                .commit()
        }
    }

    companion object {
        const val EXTRA_ID = "EXTRA_ID"
    }
}
