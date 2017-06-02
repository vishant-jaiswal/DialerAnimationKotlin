package com.wemen.samvedna.kotlin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_dialer.*

class ActivityDialer : AppCompatActivity() {
   // private var iv_ring: ImageView? = null
    private var dialerHeight: Int = 0
    private var dialerWidth: Int = 0
    private var detector: GestureDetector? = null
    private var quadrantTouched: BooleanArray? = null
    private var allowRotating: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialer)

        detector = GestureDetector(this, MyGestureDetector())
        allowRotating = true
        quadrantTouched = booleanArrayOf(false, false, false, false, false)

        if (imageOriginal == null) {
            imageOriginal = BitmapFactory.decodeResource(resources, R.drawable.dialer_ring)
        }
        // initialize the matrix only once
        if (matrix == null) {
            matrix = Matrix()
        } else {
            // not needed, you can also post the matrix immediately to restore the old state
            matrix!!.reset()
        }


        //iv_ring = findViewById(R.id.iv_ring) as ImageView
        iv_ring!!.setOnTouchListener(MyOnTouchListener())

    }

    override fun onResume() {
        super.onResume()
        iv_ring!!.viewTreeObserver.addOnGlobalLayoutListener {
            // method called more than once, but the values only need to be initialized one time
            if (dialerHeight == 0 || dialerWidth == 0) {
                dialerHeight = iv_ring!!.height
                dialerWidth = iv_ring!!.width
                // resize
                val resize = Matrix()
                resize.postScale(Math.min(dialerWidth, dialerHeight).toFloat() / imageOriginal!!.width.toFloat(), Math.min(dialerWidth, dialerHeight).toFloat() / imageOriginal!!.height.toFloat())
                println("image original width:" + imageOriginal!!.width)
                println("image original height:" + imageOriginal!!.height)
                imageScaled = Bitmap.createBitmap(imageOriginal, 0, 0, imageOriginal!!.width, imageOriginal!!.height, resize, false)

                // translate to the image view's center
                val translateX = (dialerWidth / 2 - imageScaled!!.width / 2).toFloat()
                val translateY = (dialerHeight / 2 - imageScaled!!.height / 2).toFloat()
                matrix!!.postTranslate(translateX, translateY)
                iv_ring!!.setImageBitmap(imageScaled)
                iv_ring!!.imageMatrix = matrix
            }
        }

    }

    /**
     * @return The angle of the unit circle with the image view's center
     */
    private fun getAngle(xTouch: Double, yTouch: Double): Double {
        val x = xTouch - dialerWidth / 2.0
        val y = dialerHeight.toDouble() - yTouch - dialerHeight / 2.0

        when (getQuadrant(x, y)) {
            1 -> return Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI
            2 -> return 180 - Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI
            3 -> return 180 + -1.0 * Math.asin(y / Math.hypot(x, y)) * 180.0 / Math.PI
            4 -> return 360 + Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI
            else -> return 0.0
        }
    }

    /**
     * Rotate the dialer.

     * @param degrees The degrees, the dialer should get rotated.
     */
    private fun rotateDialer(degrees: Float) {
        matrix!!.postRotate(degrees, (dialerWidth / 2).toFloat(), (dialerHeight / 2).toFloat())
        iv_ring!!.imageMatrix = matrix
    }

    /**
     * Simple implementation of an [OnTouchListener] for registering the dialer's touch events.
     */


    private inner class MyOnTouchListener : View.OnTouchListener {
        private var startAngle: Double = 0.toDouble()

        override fun onTouch(v: View, event: MotionEvent): Boolean {

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    // reset the touched quadrants
                    allowRotating = false
                    for (i in quadrantTouched!!.indices) {
                        quadrantTouched!![i] = false
                    }
                    startAngle = getAngle(event.x.toDouble(), event.y.toDouble())
                }

                MotionEvent.ACTION_MOVE -> {
                    val currentAngle = getAngle(event.x.toDouble(), event.y.toDouble())
                    rotateDialer((startAngle - currentAngle).toFloat())
                    startAngle = currentAngle
                }

                MotionEvent.ACTION_UP -> allowRotating = true
            }
            // set the touched quadrant to true
            quadrantTouched!![getQuadrant((event.x - dialerWidth / 2).toDouble(), (dialerHeight.toFloat() - event.y - (dialerHeight / 2).toFloat()).toDouble())] = true

            detector!!.onTouchEvent(event)

            return true
        }
    }

    /**
     *      * Simple implementation of a [SimpleOnGestureListener] for detecting a fling event.
     *      
     */
    private inner class MyGestureDetector : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            // get the quadrant of the start and the end of the fling
            val q1 = getQuadrant((e1.x - dialerWidth / 2).toDouble(), (dialerHeight.toFloat() - e1.y - (dialerHeight / 2).toFloat()).toDouble())
            val q2 = getQuadrant((e2.x - dialerWidth / 2).toDouble(), (dialerHeight.toFloat() - e2.y - (dialerHeight / 2).toFloat()).toDouble())

            // the inversed rotations
            if (q1 == 2 && q2 == 2 && Math.abs(velocityX) < Math.abs(velocityY)
                    || q1 == 3 && q2 == 3
                    || q1 == 1 && q2 == 3
                    || q1 == 4 && q2 == 4 && Math.abs(velocityX) > Math.abs(velocityY)
                    || q1 == 2 && q2 == 3 || q1 == 3 && q2 == 2
                    || q1 == 3 && q2 == 4 || q1 == 4 && q2 == 3
                    || q1 == 2 && q2 == 4 && quadrantTouched!![3]
                    || q1 == 4 && q2 == 2 && quadrantTouched!![3]) {

                iv_ring!!.post(FlingRunnable(-1 * (velocityX + velocityY)))
            } else {
                // the normal rotation
                iv_ring!!.post(FlingRunnable(velocityX + velocityY))
            }

            return true
        }
    }

    private inner class FlingRunnable(private var velocity: Float) : Runnable {

        override fun run() {
            if (Math.abs(velocity) > 5 && allowRotating) {
                rotateDialer(velocity / 75)
                velocity /= 1.0666f
                // post this instance again
                iv_ring!!.post(this)
            }
        }
    }

    companion object {

        private var imageOriginal: Bitmap? = null
        private var imageScaled: Bitmap? = null
        private var matrix: Matrix? = null

        /**
         * @return The selected quadrant.
         */
        private fun getQuadrant(x: Double, y: Double): Int {
            if (x >= 0) {
                return if (y >= 0) 1 else 4
            } else {
                return if (y >= 0) 2 else 3
            }
        }
    }
}
