package io.github.sceneview.sample.arcursorplacement

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.webkit.WebViewAssetLoader
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.gson.Gson
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.node.CursorNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.material.setBaseColor
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.math.toFloat3
import io.github.sceneview.model.GLBLoader
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.utils.Color
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.io.File


lateinit var newVector: Vector3
lateinit var lastVector: Vector3
var placeFlag = false
var hideAddButton = false
lateinit var currentItem: Model

data class Model(
    val fileLocation: String,
    val displayName: String, // Add this line
    val modelImage: Bitmap?,
    val scaleUnits: Float? = null,
    val placementMode: PlacementMode = PlacementMode.BEST_AVAILABLE,
    val applyPoseRotation: Boolean = true

)

data class PlacedModel(
    val anchor: Anchor?,
    val position: FloatArray?,
    val rotation: Float3?,
    val model: Model,  // assuming Model is your 3D model class
    val scale: Scale? = null
)

data class AnchorData(val x: Float, val y: Float, val z: Float)
data class ModelData(
    val x: Float,
    val y: Float,
    val z: Float,
    val rotation: Float?,
    val scale: Scale?,
    val fileLocation: String
)


class MainFragment : Fragment(R.layout.fragment_main), OnModelClickListener {


    var anchors: MutableList<Anchor> = mutableListOf()
    var modelPlaceList = mutableListOf<PlacedModel>()
    var recordCount: Int = 0
    var currentScale: Float = 1.0f
    var lastAnchor: Anchor? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var webAppInterface: WebAppInterface
    private lateinit var assetLoader: WebViewAssetLoader
    lateinit var sceneView: ArSceneView
    lateinit var loadingView: View
    lateinit var modelsView: RecyclerView
    lateinit var webView: WebView
    lateinit var anchorButton: ExtendedFloatingActionButton
    lateinit var placeBtn: ExtendedFloatingActionButton
    lateinit var doneBtn: ExtendedFloatingActionButton
    lateinit var addNodeBtn: ExtendedFloatingActionButton
    var sphereModelInstance: ModelInstance? = null
    lateinit var cursorNode: CursorNode
    var modelNode: ArModelNode? = null
    var lineColor = Color(1.0f, 1.0f, 1.0f, 1.0f)
    var placedLines = mutableListOf<ArNode>()
    var modelInstance: ModelInstance? = null
    var lineScale = 0.005f
    lateinit var cubeModelInstance: ModelInstance

    val gson = Gson()
    val client = OkHttpClient()

    var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
        }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())


        loadingView = view.findViewById(R.id.loadingView)
        modelsView = view.findViewById(R.id.modelsRV)
        webView = view.findViewById(R.id.webView)
        setupWebView()

        webAppInterface = WebAppInterface(this, webView)
        webView.addJavascriptInterface(webAppInterface, "AndroidInterface")
//        setupWebView()

        webView.loadUrl("https://appassets.androidplatform.net/assets/3.html")
//        webView.loadUrl("https:www.google.com")
        webView.isVisible = false
        val modelsFolder = File(requireContext().filesDir, "models")
        Log.e("modelsDirectory", modelsFolder.toString())
        Log.e("modelsDirectory", modelsFolder.exists().toString())
        if (modelsFolder.exists() && modelsFolder.isDirectory) {
            // List all files in the directory
            val files = modelsFolder.listFiles()
            // Check if 'files' is not null and then iterate over the array
            files?.forEach { file ->
                // Print the name of each file
                Log.e("FileName", file.name)
            }
        } else {
            Log.e("modelsDirectory", "The directory does not exist or is not a directory")
        }
        assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(requireContext()))
            .addPathHandler(
                "/files/models/",
                WebViewAssetLoader.InternalStoragePathHandler(requireContext(), modelsFolder)
            )

            .build()


        addNodeBtn = view.findViewById<ExtendedFloatingActionButton>(R.id.addNodeButton).apply {
            setOnClickListener {
                // Record current cursor position
                cursorNode.createAnchor()?.let { newAnchor ->
                    // If lastAnchor is not null, calculate the distance
                    lastAnchor?.let { lastAnchor ->


                        val newPose = newAnchor.pose
                        val lastPose = lastAnchor.pose

                        val dx = newPose.tx() - lastPose.tx()
                        val dy = newPose.ty() - lastPose.ty()
                        val dz = newPose.tz() - lastPose.tz()

                        // Compute the Euclidean distance and convert it to centimeters
                        val distance = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble())
                        val distanceInCm = distance * 100

                        Toast.makeText(
                            context,
                            "Distance between anchors: ${String.format("%.2f", distanceInCm)} m",
                            Toast.LENGTH_SHORT
                        ).show()
                        newVector = Vector3(newPose.tx(), newPose.ty(), newPose.tz())
                        lastVector = Vector3(lastPose.tx(), lastPose.ty(), lastPose.tz())
                        placeFlag = true
                        updateDistance()

                    }

                    // Set newAnchor as lastAnchor for the next click
                    lastAnchor = newAnchor
                    // Increment the record count and add the new anchor to the list
                    recordCount++
                    anchors.add(newAnchor)
                    Log.d("DEBUG", "Anchor created successfully") // Log
                    try {
                        sceneView.addChild(
                            ArModelNode(
                                sceneView.engine,
                                "layoutModel/sphere.glb",
                                true,
                                0.05f
                            ).apply {
                                Log.d(
                                    "DEBUG",
                                    "Model instance set: $modelInstance"
                                ) // Log for debugging
                                anchor = newAnchor // Set the anchor
                                parent = sceneView // Set the parent of the node to the sceneView
                                Log.d("DEBUG", "Node added to scene") // Log for debugging
                            })
                    } catch (e: Exception) {
                        Log.e(
                            "ERROR",
                            "Error adding node to scene: ${e.message}"
                        ) // Log error message
                    }


                    val anchorPose = newAnchor.pose
                    val position = anchorPose.translation.let {
                        "x: ${it[0]}, y: ${it[1]}, z: ${it[2]}"
                    }
                    // Display Toast with record count and position
                    Toast.makeText(
                        context,
                        "Record count: $recordCount at position $position",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("pos:", position)
                    Log.e("anchors", anchors.toString())
                }
            }
        }
        addNodeBtn.setIconResource(R.drawable.ic_target)

        placeBtn = view.findViewById<ExtendedFloatingActionButton>(R.id.placeGLBBtn).apply {
            setOnClickListener {
                setOnClickListener { placeModelNode() }
            }
        }
        placeBtn.setIconResource(R.drawable.ic_target)
        doneBtn = view.findViewById<ExtendedFloatingActionButton>(R.id.doneBtn).apply {
            setOnClickListener {
                doneAndGenerate()
            }
        }

        sceneView = view.findViewById<ArSceneView?>(R.id.sceneView).apply {

            planeRenderer.isVisible = false
            depthEnabled = true
            instantPlacementEnabled = true
            // Handle a fallback in case of non AR usage. The exception contains the failure reason
            // e.g. SecurityException in case of camera permission denied
            onArSessionFailed = { e: Exception ->
                // If AR is not available or the camara permission has been denied, we add the model
                // directly to the scene for a fallback 3D only usage
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
            onTapAr = { hitResult, _ ->
                anchorOrMove(hitResult.createAnchor())
            }
        }

        cursorNode = CursorNode(sceneView.engine).apply {
            onHitResult = { node, _ ->
                if (!isLoading) {
//                    anchorButton.isGone = !node.isTracking
                }
            }
        }
        sceneView.addChild(cursorNode)
        //recycler view
        modelsView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val models = loadModelsFromFiles(requireContext())
        val modelsAdaptor = ModelsAdapter(models, this@MainFragment)
        modelsView.adapter = modelsAdaptor
        modelsView.isVisible = false
        placeBtn.isVisible = false
        doneBtn.isVisible = false

        lifecycleScope.launchWhenCreated {
            modelInstance = GLBLoader.loadModelInstance(
                context = requireContext(),
                glbFileLocation = "layoutModel/cube.glb"
            )

            cubeModelInstance = GLBLoader.loadModelInstance(
                context = requireContext(),
                glbFileLocation = "layoutModel/cube.glb" // Replace with the actual path to your cube model
            )!!

            modelNode?.modelInstance = cubeModelInstance
//            anchorButton.text = getString(R.string.move_object)
//            anchorButton.setIconResource(R.drawable.ic_target)
//            anchorButton.isVisible=false
            isLoading = false





            while (true) {
//                updateDistance()
                try {
                    addLineBetweenPoints(cursorNode.hitResult!!, sceneView, lastVector, newVector)
                    Log.e("lineAdd", "line added")
                } catch (e: Exception) {
                    Log.e("ERROR", "Error adding line to scene: ${e.message}")
                }
                nextTask()
                delay(1000)
            }


        }
    }

    fun anchorOrMove(anchor: Anchor) {
        if (modelNode == null) {
            modelNode = ArModelNode(
                sceneView.engine,
                followHitPosition = false
            ).apply {
                modelInstance = modelInstance
                parent = sceneView
            }
        }
        modelNode!!.anchor = anchor
    }

    fun placeModelNode() {
        //place model
        modelNode?.anchor()
//        placeBtn.isVisible = false
        sceneView.planeRenderer.isVisible = false
        // Get the position of the anchor
        val anchorPose = modelNode?.anchor?.pose
        val position = anchorPose?.translation?.let {
            "x: ${it[0]}, y: ${it[1]}, z: ${it[2]}"
        } ?: "Unknown position"

        //record model has been placed
        val placedModel = PlacedModel(
            modelNode?.anchor, anchorPose?.translation,
            modelNode?.modelRotation, currentItem,
            modelNode?.modelScale
        )
        Log.e("nextTask:,model", placedModel.anchor?.pose.toString())
        Log.e("doneAndGenerate--:,model", placedModel.rotation.toString())
        Log.e("doneAndGenerate--:", "place" + placedModel.toString())
        modelPlaceList.add(placedModel)
        doneBtn.isVisible = true
    }

    override fun onModelClick(model: Model) {

        doneBtn.isVisible = false
        isLoading = true
        modelNode?.takeIf { !it.isAnchored }?.let {
            sceneView.removeChild(it)
            it.destroy()
        }

        modelNode = ArModelNode(sceneView.engine, model.placementMode).apply {
            isSmoothPoseEnable = true
            applyPoseRotation = model.applyPoseRotation
            Log.e("onModelClick", model.fileLocation)
            loadModelGlbAsync(
                glbFileLocation = model.fileLocation,
                autoAnimate = true,
                scaleToUnits = model.scaleUnits,
                // Place the model origin at the bottom center
                centerOrigin = Position(y = -1.0f)
            ) {
                sceneView.planeRenderer.isVisible = true
                isLoading = false
            }
            Log.e("onModelClick", model.fileLocation)
            onAnchorChanged = { anchor ->
                placeBtn.isGone = anchor != null
            }
            onHitResult = { node, _ ->
                placeBtn.isGone = !node.isTracking

            }
        }
        sceneView.addChild(modelNode!!)
        // Select the model node by default (the model node is also selected on tap)
        sceneView.selectedNode = modelNode
        currentItem = model
    }

    // update the distance between lastAnchor and current position of the CursorNode in camera
    fun updateDistance() {
        lastAnchor?.let { lastAnchor ->
            val lastPose = lastAnchor.pose
            val lastPosition = lastPose.translation

            // Get current position of the CursorNode
            val cursorPosition = cursorNode.worldPosition

            // Calculate the distance
            val distance = Math.sqrt(
                Math.pow((cursorPosition.x - lastPosition[0]).toDouble(), 2.0) +
                        Math.pow((cursorPosition.y - lastPosition[1]).toDouble(), 2.0) +
                        Math.pow((cursorPosition.z - lastPosition[2]).toDouble(), 2.0)
            )


            val distanceInCm = distance * 100

            // Update the UI with the calculated distance
            Toast.makeText(
                context,
                "anchor and current position have : ${String.format("%.2f", distanceInCm)} cm",
                Toast.LENGTH_SHORT
            ).show()
        }


    }

    suspend fun addLineBetweenPoints(
        hitResult: HitResult,
        scene: SceneView,
        from: Vector3,
        to: Vector3
    ) {


        if (placeFlag == true) {
            Log.e("lineThings", hitResult.toString())
            Log.e("lineThings", "from:$from")
            Log.e("lineThings", "to:$to")

            // make an ARCore Anchor
            val anchor = hitResult.createAnchor()
            // Node that is automatically positioned in world space based on the ARCore Anchor.
            val anchorNode = ArNode(scene.engine)
            anchorNode.anchor = anchor
            anchorNode.parent = sceneView

            // Compute a line's length
            val lineLength = Vector3.subtract(from, to).length() * 1.5f
            val context = requireContext()
            val modelIns = GLBLoader.loadModelInstance(
                context = context,
                glbFileLocation = "layoutModel/cube.glb"
            ) as ModelInstance

            for (material in modelIns.materialInstances) {
                material.setBaseColor(lineColor)
            }

            // 3. make node
            val node = ArNode(scene.engine)
            node.parent = anchorNode
            node.modelInstance = modelIns

            // 4. set rotation
            val difference = Vector3.subtract(to, from)
            val directionFromTopToBottom = difference.normalized()
            val rotationFromAToB =
                Quaternion.lookRotation(
                    directionFromTopToBottom,
                    Vector3.up()
                )
            val quaternionRot = Quaternion.multiply(
                rotationFromAToB,
                Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 90f)
            )
            node.worldPosition = Vector3.add(from, to).scaled(.5f).toFloat3()
            node.worldPosition.y = from.y
            node.worldQuaternion = dev.romainguy.kotlin.math.Quaternion(
                quaternionRot.x,
                quaternionRot.y,
                quaternionRot.z,
                quaternionRot.w
            )
            node.worldScale = Scale(lineScale, lineLength, lineScale)
            node.isPositionEditable = false
            node.isRotationEditable = false
            node.isScaleEditable = false
            placedLines.add(anchorNode)
            placeFlag = false
        } else {
            Log.e("flag", "false")
        }
    }

    fun disCursorToFirstAnchor(): Double {
        if (anchors.isNotEmpty() && anchors.size > 3) {

            val cursorPos = cursorNode.worldPosition
            val firstPos = anchors[0].pose

            val dx = cursorPos.x - firstPos.tx()
            val dy = cursorPos.y - firstPos.ty()
            val dz = cursorPos.z - firstPos.tz()

            return Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble())


        } else {
            return Double.MAX_VALUE
        }
    }

    suspend fun nextTask() {

        if (disCursorToFirstAnchor() < 0.1) {
            modelsView.isVisible = true
            placeBtn.isVisible = true
            hideAddButton = true
            val firstPos = anchors[0].pose
            val firstPosVector = Vector3(firstPos.tx(), firstPos.ty(), firstPos.tz())
            try {
                Log.e("lineAdd", "$lastVector,  $firstPosVector")
                lastVector = newVector
                placeFlag = true
                addLineBetweenPoints(cursorNode.hitResult!!, sceneView, lastVector, firstPosVector)
                Log.e("lineAdd", "next line added")
                placeFlag = false
            } catch (e: Exception) {
                Log.e("ERROR", "Error adding line to scene: ${e.message}")
            }
        } else {
            hideAddButton = false
            return
        }

        for (item in anchors) {
            Log.e("nextTask", item.pose.toString())
        }

//         addNodeBtn.isGone = hideAddButton == true
//         anchorButton.isVisible= hideAddButton==true
        updateButtonVisibility(hideAddButton)
    }

    fun updateButtonVisibility(hideAddButton: Boolean) {
        activity?.runOnUiThread {
            addNodeBtn.isGone = hideAddButton
        }
    }


    fun doneAndGenerate() {
        Log.e("doneAndGenerate", "executed")
        webView.isVisible = false
        placeBtn.isVisible=false

        getLocation(object : LocationCallback {
            override fun onLocationResult(latitude: Double, longitude: Double, altitude: Double) {
                val layoutPositions = anchors.map { anchor ->
                    val pose = anchor.pose
                    AnchorData(pose.tx(), pose.ty(), pose.tz())
                }
                val modelsPositions = modelPlaceList.map {
                    val pose = it.anchor?.pose
                    ModelData(
                        pose?.tx()!!,
                        pose.ty(),
                        pose.tz(),
                        it.rotation?.y,
                        it.scale,
                        it.model.fileLocation
                    )
                }
                val data = mapOf(
                    "layoutPositions" to layoutPositions,
                    "modelsPositions" to modelsPositions,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "altitude" to altitude
                )
                val dataJson = gson.toJson(data)
                webView.evaluateJavascript(
                    "javascript:window.AndroidInterfaceT.receiveData('$dataJson')",
                    null
                )
                Log.e("doneAndGenerate", "data send")
                isLoading = true

//                sleep(3000)
//                webAppInterface.closeWebViewT("654bdaf7f9e5b6180f7a6518")
                Log.e("doneAndGenerate", "webAppInterface.closeWebViewT")
            }

            override fun onPermissionDenied() {
                Log.e("doneAndGenerate", "Location permission denied")
            }

            override fun onLocationUnavailable() {
                Log.e("doneAndGenerate", "Location is unavailable")
            }
        })
    }

    fun loadModelsFromFiles(context: Context, scale: Float = 0.6f): List<Model> {
        val models = mutableListOf<Model>()
        try {
            val modelsFolder = File(context.filesDir, "models")
            val imagesFolder = File(context.filesDir, "images")

            if (modelsFolder.exists() && modelsFolder.isDirectory) {
                val modelFiles = modelsFolder.listFiles { _, name -> name.endsWith(".glb") }

                modelFiles?.forEach { modelFile ->
                    val displayName = modelFile.name.substringBeforeLast(".glb")
                    val imageFile = File(
                        imagesFolder,
                        "${displayName}.png"
                    ) // Assuming images are in PNG format
                    val modelImageBitmap: Bitmap? = if (imageFile.exists()) {
                        BitmapFactory.decodeFile(imageFile.absolutePath)
                    } else {
                        null
                    }
                    models.add(
                        Model(
                            Uri.fromFile(modelFile).toString(),
                            displayName,
                            modelImageBitmap,
                            scale
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return models
    }


    private fun getLocation(callback: LocationCallback) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            callback.onPermissionDenied()
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val altitude = location.altitude
                    callback.onLocationResult(latitude, longitude, altitude)
                } else {
                    callback.onLocationUnavailable()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {

                return request?.url?.let { assetLoader.shouldInterceptRequest(it) }
            }

            override fun shouldInterceptRequest(
                view: WebView,
                url: String
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(Uri.parse(url))
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }
    }


    class WebAppInterface(private val context: MainFragment, private val webView: WebView) {
        //        @JavascriptInterface
//        fun closeWebViewT(receivedId: String) {
//            Log.e("closeWebView", receivedId)
//            context.isLoading = false
//            context.requireActivity().runOnUiThread {
//                // Prepare the new fragment and pass the ID to it
//                val newFragment = ViewFragment.newInstance(receivedId)
//
//                // Replace the current fragment with the new one
//                context.parentFragmentManager.beginTransaction().apply {
//                    replace(R.id.containerFragment, newFragment)
//                    addToBackStack(null) // If you want to add the transaction to the back stack
//                    commit()
////                    commitAllowingStateLoss()
//                }
//
//            }
        @JavascriptInterface
        fun closeWebViewT(receivedId: String) {
            // Run on the UI thread because you're performing UI operations
            context.requireActivity().runOnUiThread {
                context.isLoading = false

                // Intent to start a new Activity
                val intent = Intent(context.requireContext(), ViewActivity::class.java).apply {
                    // Put any necessary extras
                    putExtra(ViewActivity.EXTRA_ID, receivedId)
                }

                // Start the new activity
                context.startActivity(intent)

                // Finish the current Activity only after the next Activity has been started
                // This ensures that the current Activity is still running until the next one is fully operational
                context.requireActivity().finish()
            }
        }


//        @JavascriptInterface
//        fun receiveData(dataJson: String) {
//            webView.post {
//                webView.evaluateJavascript("javascript:receiveData('$dataJson')", null)
//            }
//        }

        @JavascriptInterface
        fun receiveModelPlacementData(modelPlacementJson: String) {
            webView.post {
                webView.evaluateJavascript(
                    "javascript:receiveModelPlacementData('$modelPlacementJson')",
                    null
                )
            }
        }

        @JavascriptInterface
        fun receiveAnchorsData(anchorsJson: String) {
            webView.post {
                webView.evaluateJavascript(
                    "javascript:receiveAnchorsData('$anchorsJson')",
                    null
                )
            }
        }

        @JavascriptInterface
        fun sendDataToAndroid(dataJson: String) {
            // Process the data here
            Log.d("WebAppInterface", "Data received from web: $dataJson")
        }


    }
}
