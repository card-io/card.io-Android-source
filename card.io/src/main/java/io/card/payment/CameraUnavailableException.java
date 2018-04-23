package io.card.payment;

/* CameraUnavailableException.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

/**
 * This exception is thrown when the camera cannot be opened for an unexpected reason. It should NOT
 * be used if the hardware is known to be unacceptable.
 *
 * @version 1.0
 */
public class CameraUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

}
