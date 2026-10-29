package it.unibo.cus.serial;

import com.fazecast.jSerialComm.SerialPort;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Provides a graphical dialog to select the WCS serial port.
 *
 * The user can choose from the available serial ports.
 * Arduino devices are preferred when available.
 */
public final class SerialPortSelector {

    private SerialPortSelector() {
    }

    /**
     * Shows the available serial ports and waits for the user's selection.
     * Arduino devices are preferred as the initial selection.
     *
     * @return the name of the selected serial port
     * @throws IllegalStateException if no graphical environment or serial ports
     *         are available, or if the user cancels the selection
     * @throws InterruptedException if the selection is interrupted
     * @throws InvocationTargetException if the dialog cannot be displayed
     */
    public static String selectPort()
        throws InterruptedException, InvocationTargetException {

        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException(
                "No graphical environment available; set cus.serial.port in cus.properties"
            );
        }

        final SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            throw new IllegalStateException("No serial ports detected");
        }

        final PortOption[] options = new PortOption[ports.length];
        PortOption preferredOption = null;

        for (int i = 0; i < ports.length; i++) {
            options[i] = new PortOption(ports[i]);

            if (preferredOption == null && isArduinoPort(ports[i])) {
                preferredOption = options[i];
            }
        }

        if (preferredOption == null) {
            preferredOption = options[0];
        }

        final PortOption initialOption = preferredOption;
        final PortOption[] selectedOption = new PortOption[1];

        SwingUtilities.invokeAndWait(() -> {
            selectedOption[0] = (PortOption) JOptionPane.showInputDialog(
                null,
                "Select the serial port connected to Arduino:",
                "WCS serial port selection",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                initialOption
            );
        });

        if (selectedOption[0] == null) {
            throw new IllegalStateException("Serial port selection cancelled");
        }

        return selectedOption[0].port().getSystemPortName();
    }

    private static boolean isArduinoPort(final SerialPort port) {
        final String portInformation =
            safe(port.getDescriptivePortName()) + " " + safe(port.getPortDescription());

        return portInformation.toLowerCase(Locale.ROOT).contains("arduino");
    }

    private static String safe(final String value) {
        return value == null ? "" : value.trim();
    }

    private record PortOption(SerialPort port) {
        @Override
        public String toString() {
            final String portName = port.getSystemPortName();
            final String description = safe(port.getDescriptivePortName());

            if (description.isEmpty()) {
                return portName;
            }

            return portName + " - " + description;
        }
    }
}
