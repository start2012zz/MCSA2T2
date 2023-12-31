package io.github.sceneview

import android.opengl.EGLContext
import android.util.Log
import com.google.android.filament.Engine
import com.google.android.filament.Entity
import com.google.android.filament.EntityManager
import com.google.android.filament.RenderableManager
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.Utils
import io.github.sceneview.environment.IBLPrefilter
import io.github.sceneview.utils.OpenGL

// TODO : Add the lifecycle aware management when filament dependents are all kotlined
object Filament {

    init {
        Gltfio.init()
        com.google.android.filament.Filament.init()
        Utils.init()
    }

    private var eglContext: EGLContext? = null

    private var _engine: Engine? = null

    @JvmStatic
    val engine: Engine
        get() = _engine!!

    @JvmStatic
    val entityManager
        get() = EntityManager.get()

    @JvmStatic
    val transformManager
        get() = _engine!!.transformManager

    @JvmStatic
    val renderableManager
        get() = _engine!!.renderableManager

    @JvmStatic
    val lightManager
        get() = _engine!!.lightManager

    private var _resourceLoader: ResourceLoader? = null

    @JvmStatic
    val resourceLoader: ResourceLoader
        get() = _resourceLoader ?: ResourceLoader(_engine!!).also { _resourceLoader = it }

    private var _materialProvider: UbershaderProvider? = null

    @JvmStatic
    val materialProvider
        get() = _materialProvider ?: UbershaderProvider(_engine!!).also { _materialProvider = it }

    private var _assetLoader: AssetLoader? = null

    @JvmStatic
    val assetLoader: AssetLoader?
        get() = _assetLoader ?: _engine?.let {
            AssetLoader(
                it,
                materialProvider,
                entityManager
            ).also { _assetLoader = it }
        }

    private var _iblPrefilter: IBLPrefilter? = null

    @JvmStatic
    val iblPrefilter: IBLPrefilter
        get() = _iblPrefilter ?: IBLPrefilter(_engine!!).also { _iblPrefilter = it }

    var retainers = 0

    fun retain(): Engine {
        if (_engine == null) {
            _engine = Engine.create(eglContext ?: OpenGL.createEglContext().also {
                eglContext = it
            })
        }
        retainers++
        return _engine!!
    }

    fun release() {
        retainers--
        Log.d("Sceneview", "Filament Retainers: $retainers")
        if (retainers == 0) {
            destroy()
        }
    }

    fun destroy() {
        // TODO: We still got some errors on this destroy due to this nightmare Renderable
        //  Should be solved with RIP Renderable
        runCatching { _assetLoader?.destroy() }
        _assetLoader = null

        _resourceLoader?.apply {
            runCatching { asyncCancelLoad() }
            runCatching { evictResourceData() }
            runCatching { destroy() }
        }
        _resourceLoader = null

        // TODO: Materials should be destroyed by their own
        // TODO: Hot fix because of not destroyed instances
        runCatching { _materialProvider?.destroyMaterials() }
        runCatching { _materialProvider?.destroy() }
        _materialProvider = null

        runCatching { _iblPrefilter?.destroy() }
        _iblPrefilter = null

        runCatching { _engine?.flushAndWait() }
        runCatching { _engine?.destroy() }
        _engine = null

        eglContext?.let {
            runCatching { OpenGL.destroyEglContext(it) }
        }
        eglContext = null

        Log.e("Sceneview", "Filament Engine destroyed")
    }
}

fun Engine.createCamera() = createCamera(entityManager.create())

fun RenderableManager.Builder.build(engine: Engine, @Entity entity: Int) = build(engine, entity)