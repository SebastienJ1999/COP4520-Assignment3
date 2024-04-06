import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Assignment32 {
    private static final List<TemperatureReading> temperatureReadings = Collections.synchronizedList(new ArrayList<>());
    private static final Lock lock = new ReentrantLock();

    static class TemperatureReading {
        long timestamp;
        int sensorId;
        double temperature;
    
        public TemperatureReading(long timestamp, int sensorId, double temperature) {
            this.timestamp = timestamp;
            this.sensorId = sensorId;
            this.temperature = temperature;
        }
    
        @Override
        public String toString() {
            return "Sensor ID: " + sensorId + ", Temperature: " + temperature;
        }
    }
    

    static class TemperatureSensor implements Runnable {
        private final int sensorId;
        private final Random random = new Random();

        public TemperatureSensor(int sensorId) {
            this.sensorId = sensorId;
        }

        @Override
        public void run() {
            while (true) {
                double temperature = -100 + (70 - (-100)) * random.nextDouble();
                addTemperatureReading(new TemperatureReading(System.currentTimeMillis(), sensorId, temperature));
                try {
                    Thread.sleep(60 * 1000); // Read temperature every minute
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static void addTemperatureReading(TemperatureReading reading) {
        lock.lock();
        try {
            temperatureReadings.add(reading);
        } finally {
            lock.unlock();
        }
    }

    static void generateReport() {
        while (true) {
            long currentTime = System.currentTimeMillis();
            List<TemperatureReading> pastHourReadings = new ArrayList<>();
            lock.lock();
            try {
                for (TemperatureReading reading : temperatureReadings) {
                    if (currentTime - reading.timestamp < 3600 * 1000) {
                        pastHourReadings.add(reading);
                    }
                }
            } finally {
                lock.unlock();
            }

            // Sort pastHourReadings by temperature
            pastHourReadings.sort((r1, r2) -> Double.compare(r1.temperature, r2.temperature));

            // Get top 5 highest and lowest temperatures
            List<TemperatureReading> highestTemps = pastHourReadings.subList(Math.max(0, pastHourReadings.size() - 5), pastHourReadings.size());
            List<TemperatureReading> lowestTemps = pastHourReadings.subList(0, Math.min(5, pastHourReadings.size()));

            // Find the largest temperature difference
            double largestDifference = 0;
            long startTimestamp = 0;
            long endTimestamp = 0;
            for (int i = 0; i < pastHourReadings.size() - 1; i++) {
                double diff = Math.abs(pastHourReadings.get(i).temperature - pastHourReadings.get(i + 1).temperature);
                if (diff > largestDifference) {
                    largestDifference = diff;
                    startTimestamp = pastHourReadings.get(i).timestamp;
                    endTimestamp = pastHourReadings.get(i + 1).timestamp;
                }
            }

            System.out.println("Report for the past hour:");
            System.out.println("Top 5 highest temperatures: " + highestTemps.stream().map(TemperatureReading::toString).collect(Collectors.toList()));
            System.out.println("Top 5 lowest temperatures: " + lowestTemps.stream().map(TemperatureReading::toString).collect(Collectors.toList()));
            System.out.println("10-minute interval with the largest temperature difference: " +
                    "Start Time: " + startTimestamp + ", End Time: " + endTimestamp);

            try {
                Thread.sleep(3600 * 1000); // Generate report every hour
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        // Create threads for temperature sensors
        Thread[] threads = new Thread[8];
        for (int i = 0; i < 8; i++) {
            threads[i] = new Thread(new TemperatureSensor(i));
            threads[i].start();
        }

        // Start thread for generating report
        new Thread(Assignment32::generateReport).start();

        // Wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
