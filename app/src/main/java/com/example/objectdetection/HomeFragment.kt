package com.example.objectdetection

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import com.example.objectdetection.databinding.FragmentHomeBinding
import com.example.objectdetection.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp


class HomeFragment : Fragment() {
    lateinit var labels: List<String>
    val paint = Paint()
    lateinit var bitmap: Bitmap
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    lateinit var cameraDevice: CameraDevice
    private lateinit var cameraManager: CameraManager
    lateinit var handler: Handler
    lateinit var model: SsdMobilenetV11Metadata1
    lateinit var imageProcessor: ImageProcessor
    var colors = listOf(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getPermission()
        labels = FileUtil.loadLabels(requireContext(), "labels.txt")
        imageProcessor =
            ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = SsdMobilenetV11Metadata1.newInstance(requireContext())

        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()

        handler = Handler(handlerThread.looper)
        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                // Handle surface texture size change
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                bitmap = binding.textView.bitmap!!

                // Creates inputs for reference.
                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)
                // Runs model inference and gets result.
                val outputs = model.process(image)
                val locations = outputs.locationsAsTensorBuffer.floatArray
                val classes = outputs.classesAsTensorBuffer.floatArray
                val scores = outputs.scoresAsTensorBuffer.floatArray
                val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

                val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutable)


                val h = mutable.height
                val w = mutable.width
                paint.textSize = h / 15f
                paint.strokeWidth = h / 85f
                var x = 0
                scores.forEachIndexed { index, fl ->
                    x = index
                    x *= 4
                    if (fl > 0.5) {
                        paint.color = colors[index]
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(
                            RectF(
                                locations[x + 1] * w,
                                locations[x] * h,
                                locations[x + 3] * w,
                                locations[x + 2] * h
                            ), paint
                        )
                        paint.style = Paint.Style.FILL
                        canvas.drawText(
                            labels[classes[index].toInt()] + " " + fl.toString(),
                            locations[x + 1] * w,
                            locations[x] * h,
                            paint
                        )
                    }
                }
                binding.imageView.setImageBitmap(mutable)

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        model.close()
    }


    @SuppressLint("MissingPermission")
    fun openCamera() {
        cameraManager.openCamera(
            cameraManager.cameraIdList[0], @SuppressLint("MissingPermission")
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera

                    val surfaceTexture = binding.textView.surfaceTexture
                    val surface = Surface(surfaceTexture)

                    val captureRequest =
                        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    captureRequest.addTarget(surface)

                    cameraDevice.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.setRepeatingRequest(captureRequest.build(), null, null)
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                TODO("Not yet implemented")
                            }
                        },
                        handler
                    )
                }

                override fun onDisconnected(camera: CameraDevice) {
                    TODO("Not yet implemented")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    TODO("Not yet implemented")
                }

            }, handler
        )
    }

    private fun getPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        )
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getPermission()
        }
    }
}