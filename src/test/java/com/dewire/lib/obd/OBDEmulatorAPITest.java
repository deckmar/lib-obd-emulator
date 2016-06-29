package com.dewire.lib.obd;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johan Deckmar
 */
public class OBDEmulatorAPITest {

    private final static Logger LOG = LoggerFactory.getLogger(OBDEmulatorAPITest.class);

    private final String COM_PORT_NAME_OBDII_EMULATOR = "COM3";

    private OBDEmulatorAPI car;

    @Before
    public void setUp() throws Exception {
        car = new OBDEmulatorAPI();
    }

    @Test
    public void test_battery_voltage_low() throws Exception {

        car.connect(COM_PORT_NAME_OBDII_EMULATOR);

        for(int voltage = 12; voltage >= 0; voltage--) {
            car.setBatteryVoltage(voltage);
            int batteryVoltage = car.getBatteryVoltage();
            //LOG.debug("Read battery voltage: {} V", batteryVoltage);
            car.sleep(3000);
        }
        car.setBatteryVoltage(12);
    }

    @Test
    public void test_make_trip() throws Exception {

        car.connect(COM_PORT_NAME_OBDII_EMULATOR);

        car.setEngineStarted(false);
        car.setSpeed(0);
        car.setBatteryVoltage(12);
        car.sleep(1000);
        car.setEngineStarted(true);
        car.sleep(1000);

        car.setEngineRPM(2400);
        car.setEngineFuelRateLitersPerHour(5);  // Liters per hour -- 5 is a good consumption, 10 is quite high
        car.setFuelTankPercentage(70);

        for(int speed = 0; speed <= 140; speed += 140) {
            LOG.debug("~~~~~~");
            car.setSpeed(speed);
            car.setEngineRPM(2400 + speed * 3);
            car.setFuelTankPercentage(70 - speed/10);
            car.sleep(8000);
        }

        LOG.debug("Breaking from 160 km/h to 90 km/h");

        car.setSpeed(90);
        car.setEngineRPM(2400);

        car.sleep(5000);

        car.setSpeed(0);
        car.setEngineRPM(0);

        car.setEngineStarted(false);

    }

    @Test
    public void test_fuel_values() throws Exception {
        car.connect(COM_PORT_NAME_OBDII_EMULATOR);

        car.setEngineFuelRateLitersPerHour(5);  // Liters per hour -- 5 is a good consumption, 10 is quite high
        car.setFuelTankPercentage(70);

        LOG.debug("Read fuel rate: {} L/h", car.getEngineFuelRate());
        LOG.debug("Read fuel tank: {} %", car.getFuelTankPercentage());


    }

}