package Tiberiu.smartGrid.hybrid;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Loads and indexes household solar generation (GG) and general consumption (GC) time series
 * from the Ausgrid Solar Home Electricity dataset (48 half-hour intervals per day).
 */
public class AusgridDatasetLoader {

    /** Maps customerId -> net energy array (GG - GC in kWh) per tick. */
    private final Map<Integer, double[]> netEnergyProfiles = new HashMap<>();

    /**
     * Parses an official Ausgrid CSV file for a specific calendar date.
     * Expected columns: Customer, Generator Capacity, Postcode, Consumption Category, Date, 0:30..0:00
     *
     * @param csvPath
     *            path to the Ausgrid CSV file.
     * @param targetDate
     *            date string to filter (e.g., "1/07/2012"), or {@code null} for the first matching records.
     * @throws IOException
     *             if the CSV file cannot be read.
     */
    public void loadFromCsv(String csvPath, String targetDate) throws IOException {
        Map<Integer, double[]> generationMap = new HashMap<>();
        Map<Integer, double[]> consumptionMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String header = br.readLine();
            if (header != null && !header.startsWith("Customer")) {
                header = br.readLine();
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");
                if (cols.length < 53)
                    continue;

                int customerId = Integer.parseInt(cols[0].trim());
                String category = cols[3].trim();
                String date = cols[4].trim();

                if (targetDate != null && !targetDate.equals(date))
                    continue;

                double[] values = new double[48];
                for (int t = 0; t < 48; t++) {
                    values[t] = Double.parseDouble(cols[5 + t].trim());
                }

                if ("GG".equalsIgnoreCase(category)) {
                    generationMap.put(customerId, values);
                } else if ("GC".equalsIgnoreCase(category)) {
                    consumptionMap.put(customerId, values);
                }
            }
        }

        netEnergyProfiles.clear();
        for (Integer customerId : consumptionMap.keySet()) {
            double[] gc = consumptionMap.get(customerId);
            double[] gg = generationMap.getOrDefault(customerId, new double[48]);
            double[] net = new double[48];
            for (int t = 0; t < 48; t++) {
                net[t] = gg[t] - gc[t];
            }
            netEnergyProfiles.put(customerId, net);
        }
    }

    /**
     * Generates deterministic synthetic profiles following realistic solar/household load curves
     * for standalone testing when the external CSV file is not present.
     */
    public void generateSyntheticAusgridProfiles(int numCustomers, int numTicks, long seed) {
        netEnergyProfiles.clear();
        Random rng = new Random(seed);
        for (int c = 1; c <= numCustomers; c++) {
            double[] net = new double[numTicks];
            double pvScale = 1.5 + rng.nextDouble() * 3.5;
            double baseLoad = 0.2 + rng.nextDouble() * 0.4;

            for (int t = 0; t < numTicks; t++) {
                int slotOfDay = t % 48;
                double hour = slotOfDay * 0.5;

                // Midday photovoltaic generation bell curve
                double solar = (hour >= 6.5 && hour <= 18.5)
                        ? pvScale * 0.5 * Math.exp(-Math.pow(hour - 13.0, 2) / 8.0) * (0.85 + 0.3 * rng.nextDouble())
                        : 0.0;

                // Morning and evening household consumption peaks
                double morningPeak = 0.6 * Math.exp(-Math.pow(hour - 8.0, 2) / 2.0);
                double eveningPeak = 1.4 * Math.exp(-Math.pow(hour - 19.5, 2) / 3.5);
                double consumption = (baseLoad + morningPeak + eveningPeak) * (0.85 + 0.3 * rng.nextDouble());

                net[t] = solar - consumption;
            }
            netEnergyProfiles.put(c, net);
        }
    }

    /**
     * Returns the net energy surplus (> 0) or deficit (< 0) in kWh for the given customer and tick.
     */
    public double getNetEnergy(int customerId, long tick) {
        double[] profile = netEnergyProfiles.get(customerId);
        if (profile == null || profile.length == 0)
            return 0.0;
        return profile[(int) (tick % profile.length)];
    }
}