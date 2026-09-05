package GameFlow.Vision

/**
 * Provides the default {@link TemplateMatcher}. A real OpenCV engine is used
 * only when it is compiled in AND explicitly opted in via the system property
 * {@code -Dgameflow.opencv=true}. Otherwise the zero-dependency pure-Kotlin
 * matcher is used so the app always starts with no native binaries.
 */
class TemplateMatchers private constructor() {

    companion object {

        private val OPENCV_CLASS = "GameFlow.Vision.opencv.OpencvTemplateMatcher"

        fun createDefault(opencvEnabled: Boolean): TemplateMatcher {
            val wantOpenCv = opencvEnabled || System.getProperty("gameflow.opencv", "false") == "true"
            if (wantOpenCv) {
                try {
                    val engine = Class.forName(OPENCV_CLASS).getDeclaredConstructor().newInstance()
                    return engine as TemplateMatcher
                } catch (t: Throwable) {
                    // OpenCV not on classpath or failed to init -> fall back.
                }
            }
            return PureJavaTemplateMatcher()
        }

        /** Convenience used by the app; adopts whatever the runtime default yields. */
        fun createDefault(): TemplateMatcher = createDefault(false)
    }
}