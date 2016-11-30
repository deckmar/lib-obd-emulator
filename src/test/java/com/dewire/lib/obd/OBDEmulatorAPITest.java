package com.dewire.lib.obd;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ThreadLocalRandom;

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
        car.setMILDistanceTraveledKm(0);
        car.setMILTimeTraveledMin(0);

        for(int speed = 0; speed <= 140; speed += 30) {
            LOG.debug("~~~~~~");
            car.setSpeed(speed);
            car.setEngineRPM(2400 + speed * 3);
            car.setFuelTankPercentage(70 - speed/10);
            car.sleep(1500);
        }

        LOG.debug("Breaking from 160 km/h to 90 km/h");

        car.setSpeed(90);
        car.setEngineRPM(2400);

        car.sleep(5000);

        car.setSpeed(0);
        car.setEngineRPM(0);

        car.setEngineStarted(false);

        car.close();
    }

    @Test
    public void test_fuel_values() throws Exception {
        car.connect(COM_PORT_NAME_OBDII_EMULATOR);

        car.setEngineFuelRateLitersPerHour(5);  // Liters per hour -- 5 is a good consumption, 10 is quite high
        car.setFuelTankPercentage(70);

        LOG.debug("Read fuel rate: {} L/h", car.getEngineFuelRate());
        LOG.debug("Read fuel tank: {} %", car.getFuelTankPercentage());
    }

    @Test
    public void test_make_two_trips_with_short_break_between() throws Exception {
        test_make_trip();
        car.sleep(20000);
        test_make_trip();
    }

    @Test
    public void test_make_two_trips_with_long_break_between() throws Exception {
        test_make_trip();
        car.sleep(130000);
        test_make_trip();
    }

    @Test
    public void test_long_trip() throws Exception {
        car.connect(COM_PORT_NAME_OBDII_EMULATOR);
        car.setVINReportingEnabled(false);
        car.setEngineStarted(true);
        car.setEngineFuelRateLitersPerHour(5);
        car.setFuelTankPercentage(50);

        //45 meters per second = 162 km per hour
        for (int tripLength = 0; tripLength <= 80000; tripLength += 45){
            car.setEngineRPM(ThreadLocalRandom.current().nextInt(1500,2500));
            car.setSpeed(ThreadLocalRandom.current().nextInt(160,170)); //Produce wildly fluctuating speed
            car.sleep(1000);
        }

        car.setSpeed(0);
        car.setEngineRPM(0);
        car.setEngineStarted(false);
    }

    @Test
    public void test_time_based_trip() throws Exception {
        car.connect(COM_PORT_NAME_OBDII_EMULATOR);
        car.setOBDProtocol("CAN_11B_500K");
        car.setVINReportingEnabled(true);
        //car.setVINReportingNr("");
        car.setEngineStarted(true);
        car.setMILPID(true);
        car.setMILDistanceTraveledKm(25);
        car.setEngineFuelRateLitersPerHour(5);
        car.setFuelTankPercentage(50);
        car.setDTC03Value("P0105,P0200,P0300,P0500,P0600,C0077");

        long t = System.currentTimeMillis();
        long end = t+60000; //How long trip should last (milliseconds)
        while(System.currentTimeMillis() < end) {
            car.setEngineRPM(ThreadLocalRandom.current().nextInt(2500,2700));
            car.setSpeed(72); //72km/h = 20 meters per second, easy to calculate an expected distance
        }

        car.setEngineRPM(0);
        car.setSpeed(0);
        car.setEngineStarted(false);
    }
}
