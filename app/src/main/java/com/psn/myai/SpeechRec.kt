package com.psn.myai

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
/**
 * Wrapper for easy offline speech recognition support.
 *
 *
 * When offline the speech recognizer will not more end with ERROR_NO_MATCH when text was spoken.
 * Instead it is reported like online recognized text with SpeechRecognizer.RESULTS_RECOGNITION.
 *
 *
 * The difference between online and offline is that you only get one match in the results.
 *
 *
 * See also http://stackoverflow.com/questions/30654191/speechrecognizer-offline-error-no-match
 */
class SpeechRec(private val wrappedRecognitionListener: RecognitionListener) :
    RecognitionListener {
    /**
     * Save the latest partial result for reporting when offline.
     */
    private var latestPartialResults: Bundle? = null

    /**
     * The onError method get called multiple times with the same errorCode when offline
     * but it is enough to report it only once.
     */
    private var isErrorAlreadyCalledAfterBeginOfSpeech = false
    override fun onReadyForSpeech(params: Bundle) {
        latestPartialResults = null
        isErrorAlreadyCalledAfterBeginOfSpeech = false
        wrappedRecognitionListener.onReadyForSpeech(params)
    }

    override fun onResults(results: Bundle) {
        wrappedRecognitionListener.onResults(results)
    }

    override fun onPartialResults(partialResults: Bundle) {
        latestPartialResults = partialResults
        wrappedRecognitionListener.onPartialResults(partialResults)
    }

    override fun onError(error: Int) {
        if (isErrorAlreadyCalledAfterBeginOfSpeech) {
            return
        }
        isErrorAlreadyCalledAfterBeginOfSpeech = true
        if (isOfflineSpeechRecognition(error, latestPartialResults)) {
            appendUnstableTextToResult(latestPartialResults)
            wrappedRecognitionListener.onResults(latestPartialResults)
        } else {
            wrappedRecognitionListener.onError(error)
        }
    }

    private fun isOfflineSpeechRecognition(error: Int, latestPartialResults: Bundle?): Boolean {
        return error == SpeechRecognizer.ERROR_NO_MATCH && latestPartialResults != null
    }

    private fun appendUnstableTextToResult(partialResults: Bundle?) {
        // Partial results are split up by previous spoken text and the current spoken text after
        // short speak break given as 'UNSTABLE_TEXT'. To provide same usage for online and offline
        // we report it combined. It gives only one result when doing speech recognition offline.
        val target = partialResults!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val source = partialResults.getStringArrayList("android.speech.extra.UNSTABLE_TEXT")
        target!![0] = (target[0] + " " + source!![0]).trim { it <= ' ' }
    }

    override fun onBeginningOfSpeech() {
        latestPartialResults = null
        wrappedRecognitionListener.onBeginningOfSpeech()
    }

    override fun onBufferReceived(buffer: ByteArray) {
        wrappedRecognitionListener.onBufferReceived(buffer)
    }

    override fun onEndOfSpeech() {
        wrappedRecognitionListener.onEndOfSpeech()
    }

    override fun onEvent(eventType: Int, params: Bundle) {
        wrappedRecognitionListener.onEvent(eventType, params)
    }

    override fun onRmsChanged(rmsdB: Float) {
        wrappedRecognitionListener.onRmsChanged(rmsdB)
    }
}