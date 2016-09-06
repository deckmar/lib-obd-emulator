package com.dewire.lib.obd;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Johan Deckmar
 */
public class OBDEmulatorAPI {

    private final static Logger LOG = LoggerFactory.getLogger(OBDEmulatorAPI.class);

    protected String APPLICATION_NAME = "DEWIRE_CAR_CONTROLLER";

    public static final String OBD_PID_ENGINE_RPM = "010C";
    public static final String OBD_PID_VEHICLE_SPEED = "010D";
    public static final String OBD_PID_BATTERY_VOLTAGE = "0142";
    public static final String OBD_PID_FUEL_TANK_LEVEL = "012F";
    public static final String OBD_PID_ENGINE_FUEL_RATE= "015E";
    protected SerialPort port;


    public OBDEmulatorAPI() {

    }

    public SerialPort connect(String comPort) {
        LOG.debug("Listing all serial ports:");
        for (SerialPort serialPort : SerialPort.getCommPorts()) {
            LOG.debug(serialPort.getSystemPortName());
            if (comPort.equals("CHOOSE_FIRST_AVAILABLE")) comPort = serialPort.getSystemPortName();
        }

        LOG.debug("Trying to connect to: " + comPort);


        for (SerialPort serialPort : SerialPort.getCommPorts()) {
            if (serialPort.getSystemPortName().equals(comPort)) {
                this.port = serialPort;
                this.port.setBaudRate(38400);
                this.port.openPort();
                LOG.debug("Successfully connected");
                return serialPort;
            }
        }

        LOG.debug("Could not find COM port: " + comPort);

        return null;
    }

    public void setEngineStarted(boolean started) throws IOException {
        LOG.debug("[SET] Engine: " + (started ? "ON" : "OFF"));
        write(port.getOutputStream(), "ATACC" + (started ? "1" : "0"));
        readUntilEnd(port.getInputStream());
    }

    public int getEngineRPM() throws Exception {
        return scaleValueFromCar(hexToInteger(getPIDValue(OBD_PID_ENGINE_RPM)), hexToInteger("9C 3C"), 9999);
    }


    public void setEngineRPM(int value) throws IOException {
        LOG.debug("[SET] Engine: " + value + " RPM");
        write(port.getOutputStream(), String.format("ATSET %s=%d", OBD_PID_ENGINE_RPM, value));
        readUntilEnd(port.getInputStream());
    }

    public int getSpeed() throws Exception {
        return hexToInteger(getPIDValue(OBD_PID_VEHICLE_SPEED));
    }

    public void setSpeed(int value) throws IOException {
        LOG.debug("[SET] Speed: " + value + " km/h");
        write(port.getOutputStream(), String.format("ATSET %s=%d", OBD_PID_VEHICLE_SPEED, value));
        readUntilEnd(port.getInputStream());
    }

    public int getBatteryVoltage() throws Exception {
        return hexToInteger(getPIDValue(OBD_PID_BATTERY_VOLTAGE));
    }

    public void setBatteryVoltage(int value) throws IOException {
        LOG.debug("[SET] Battery voltage: " + value + " V");
        write(port.getOutputStream(), String.format("ATSET %s=%d", OBD_PID_BATTERY_VOLTAGE, value));
        readUntilEnd(port.getInputStream());
    }

    public int getFuelTankPercentage() throws Exception {
        return (int) ((hexToInteger(getPIDValue(OBD_PID_FUEL_TANK_LEVEL)) / 254.0) * 100);
    }

    public void setFuelTankPercentage(int value) throws IOException {
        LOG.debug("[SET] Fuel tank: " + value + " %");
        write(port.getOutputStream(), String.format("ATSET %s=%d", OBD_PID_FUEL_TANK_LEVEL, value));
        readUntilEnd(port.getInputStream());
    }

    public int getEngineFuelRate() throws Exception {
        return hexToInteger(getPIDValue(OBD_PID_ENGINE_FUEL_RATE)) / 20;
    }

    public void setEngineFuelRateLitersPerHour(int value) throws IOException {
        LOG.debug("[SET] Engine fuel consumtion rate: " + value + " L/h");
        write(port.getOutputStream(), String.format("ATSET %s=%d", OBD_PID_ENGINE_FUEL_RATE, value));
        readUntilEnd(port.getInputStream());
    }

    public void sleep(int t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
        }
    }

    public void close() {

    }

    protected String getPIDValue(String pid) throws Exception {
        // Read away any "garbage"
        readUntilEnd(port.getInputStream());

        write(port.getOutputStream(), "ATGET " + pid);

        String resp = readUntilEnd(port.getInputStream());
        if (resp == null || resp.length() == 0 || resp.indexOf("=") == 0) {
            LOG.debug("Error: Did not expect response ("+resp.length()+"): " + resp);
        }

        //LOG.debug(resp);
        return resp.split("=")[1].trim();
    }

    protected boolean setPIDValue(String pid, String value) throws Exception {
        write(port.getOutputStream(), "ATSET " + pid + " " + value);

        String resp = readUntilEnd(port.getInputStream());

        return resp.equals("OK");
    }


    protected void readUntilEndAndPrint(InputStream ins) throws Exception {
        String msg = readUntilEnd(ins);
        LOG.debug(msg);
    }


    protected String readUntilEnd(InputStream ins) throws IOException {
        int n;
        String msg = "";
        sleep(100);
        while ((n = ins.available()) > 0) {
            for (int i = 0; i < n; ++i) {
                int read = ins.read();
                if (read != 13) msg += (char) read;
            }
            sleep(100);
        }
        return msg;
    }

    protected void write(OutputStream os, String msg) throws IOException {
        os.write((msg + "\r").getBytes("US-ASCII"));
    }

    protected int hexToInteger(String hex) {
        hex = hex.toUpperCase().replaceAll("[^0-9A-F]", "");
        return (int) Long.parseLong(hex, 16);
    }

    protected String integerToHex(int num) {
        String hex = Integer.toHexString(num).toUpperCase();
        hex = hex.replaceAll("([0-9A-Z]{2})", "$1 ").trim();
        if (hex.length() == 2) hex = "00 " + hex;
        return hex.trim();
    }

    protected int scaleValueFromCar(int value, int maxValue, int maxScaledValue) {
        return (int) (((double) value / (double) maxValue) * (double) maxScaledValue);
    }

    protected String scaleValueToCar(int value, int maxValue, int maxScaledValue) {
        return integerToHex((int) (((double) value / (double) maxScaledValue) * (double) maxValue));
    }
}
